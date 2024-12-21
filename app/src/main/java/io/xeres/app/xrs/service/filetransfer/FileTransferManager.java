/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
 *
 * This file is part of Xeres.
 *
 * Xeres is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xeres is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Xeres.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.xeres.app.xrs.service.filetransfer;

import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.SettingsService;
import io.xeres.app.service.file.FileService;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.rest.file.FileProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static io.xeres.app.service.file.FileService.DOWNLOAD_EXTENSION;
import static io.xeres.app.service.file.FileService.DOWNLOAD_PREFIX;
import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.CHUNK_SIZE;

/**
 * File transfer class.
 * <p>
 * <img src="doc-files/filetransfer.svg" alt="File transfer diagram">
 */
class FileTransferManager implements Runnable
{
	private static final Logger log = LoggerFactory.getLogger(FileTransferManager.class);

	private static final int DEFAULT_TICK = 250;

	private final FileTransferRsService fileTransferRsService;
	private final FileService fileService;
	private final SettingsService settingsService;
	private final LocationService locationService;
	private final DatabaseSessionManager databaseSessionManager;
	private final Location ownLocation;
	private final BlockingQueue<Action> queue;
	private final FileTransferStrategy fileTransferStrategy;

	private final Map<Sha1Sum, FileTransferAgent> leechers = new HashMap<>(); // files that we are downloading (client)
	private final Map<Sha1Sum, FileTransferAgent> seeders = new HashMap<>(); // files that we are uploading (serving)

	private final List<FileProgress> downloadsProgress = new ArrayList<>();
	private final List<FileProgress> uploadsProgress = new ArrayList<>();

	public FileTransferManager(FileTransferRsService fileTransferRsService, FileService fileService, SettingsService settingsService, LocationService locationService, DatabaseSessionManager databaseSessionManager, Location ownLocation, BlockingQueue<Action> queue, FileTransferStrategy fileTransferStrategy)
	{
		this.fileTransferRsService = fileTransferRsService;
		this.fileService = fileService;
		this.settingsService = settingsService;
		this.locationService = locationService;
		this.databaseSessionManager = databaseSessionManager;
		this.ownLocation = ownLocation;
		this.queue = queue;
		this.fileTransferStrategy = fileTransferStrategy;
	}

	@Override
	public void run()
	{
		var done = false;

		while (!done)
		{
			try
			{
				var action = (leechers.isEmpty() && seeders.isEmpty()) ? queue.take() : queue.poll(DEFAULT_TICK, TimeUnit.MILLISECONDS); // XXX: change the timeout value... or better... have a way to compute the next one.
				processAction(action);
				processLeechers();
				processSeeders();
			}
			catch (InterruptedException e)
			{
				log.debug("FileTransferManager thread interrupted");
				cleanup();
				done = true;
				Thread.currentThread().interrupt();
			}
		}
	}

	private void cleanup()
	{
		leechers.forEach((hash, fileTransferAgent) -> fileService.suspendDownload(hash, fileTransferAgent.getFileProvider().getChunkMap()));
	}

	public List<FileProgress> getDownloadsProgress()
	{
		synchronized (downloadsProgress)
		{
			//noinspection unchecked
			return (List<FileProgress>) ((ArrayList<FileProgress>) downloadsProgress).clone();
		}
	}

	public List<FileProgress> getUploadsProgress()
	{
		synchronized (uploadsProgress)
		{
			//noinspection unchecked
			return (List<FileProgress>) ((ArrayList<FileProgress>) uploadsProgress).clone();
		}
	}

	private void processLeechers()
	{
		leechers.forEach((hash, agent) -> agent.process());
	}

	private void processSeeders()
	{
		seeders.entrySet().removeIf(agent -> {
			if (agent.getValue().isIdle())
			{
				agent.getValue().stop();
				return true;
			}
			return false;
		});
		seeders.forEach((hash, agent) -> agent.process());
	}

	private void processAction(Action action)
	{
		switch (action)
		{
			case ActionAddPeer(Sha1Sum hash, Location location) -> actionAddPeer(hash, location);
			case ActionRemovePeer(Sha1Sum hash, Location location) -> actionRemovePeer(hash, location);

			case ActionReceiveDataRequest(Location location, Sha1Sum hash, long offset, int chunkSize) -> actionReceiveDataRequest(location, hash, offset, chunkSize);
			case ActionReceiveData(Location location, Sha1Sum hash, long offset, byte[] data) -> actionReceiveData(location, hash, offset, data);

			case ActionDownload(long id, String name, Sha1Sum hash, long size, LocationIdentifier from, BitSet chunkMap) -> actionDownload(id, name, hash, size, from, chunkMap);
			case ActionRemoveDownload(long id) -> actionRemoveDownload(id);

			case ActionGetDownloadsProgress() -> actionComputeDownloadsProgress();
			case ActionGetUploadsProgress() -> actionComputeUploadsProgress();

			case ActionReceiveChunkMapRequest(Location location, Sha1Sum hash, boolean isLeecher) -> actionReceiveChunkMapRequest(location, hash, isLeecher);
			case ActionReceiveChunkMap(Location location, Sha1Sum hash, List<Integer> compressedChunkMap) -> actionReceiveChunkMap(location, hash, compressedChunkMap);

			case ActionReceiveSingleChunkCrcRequest(Location location, Sha1Sum hash, int chunkNumber) -> actionReceiveChunkCrcRequest(location, hash, chunkNumber);
			case ActionReceiveSingleChunkCrc(Location location, Sha1Sum hash, int chunkNumber, Sha1Sum checkSum) -> actionReceiveChunkCrc(location, hash, chunkNumber, checkSum);
			case null ->
			{
				// This is the return from a timeout. Nothing to do.
			}
			default -> throw new IllegalStateException("Unexpected action: " + action);
		}
	}

	public void actionDownload(long id, String name, Sha1Sum hash, long size, LocationIdentifier from, BitSet chunkMap)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			leechers.computeIfAbsent(hash, sha1Sum -> {
				var file = Paths.get(settingsService.getIncomingDirectory(), DOWNLOAD_PREFIX + sha1Sum + DOWNLOAD_EXTENSION).toFile();
				log.debug("Downloading file {}, size: {}, from: {}", file, size, from);
				var fileLeecher = new FileLeecher(id, file, size, chunkMap, from != null ? FileTransferStrategy.LINEAR : fileTransferStrategy);
				if (fileLeecher.open())
				{
					var agent = new FileTransferAgent(fileTransferRsService, name, sha1Sum, fileLeecher);
					if (from != null)
					{
						agent.setTrusted(true);
						locationService.findLocationByLocationIdentifier(from).ifPresent(agent::addSeeder);
					}
					else
					{
						fileTransferRsService.activateTunnels(sha1Sum);
					}
					return agent;
				}
				else
				{
					log.error("Couldn't create file {} for download", file);
					return null;
				}
			});
		}
	}

	public void actionRemoveDownload(long id)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			fileService.findById(id).ifPresent(fileDownload -> {
				fileTransferRsService.deactivateTunnels(fileDownload.getHash());
				var leecher = leechers.get(fileDownload.getHash());
				if (leecher != null)
				{
					leecher.cancel();
					leechers.remove(fileDownload.getHash());
				}
				fileService.removeDownload(id);
			});
		}
	}

	private void actionComputeDownloadsProgress()
	{
		List<FileProgress> newDownloadList = new ArrayList<>(leechers.size());
		leechers.forEach((sha1Sum, fileTransferAgent) -> newDownloadList.add(
				new FileProgress(fileTransferAgent.getFileProvider().getId(),
						fileTransferAgent.getFileName(),
						fileTransferAgent.getFileProvider().getBytesWritten(),
						fileTransferAgent.getFileProvider().getFileSize(),
						sha1Sum.toString(),
						fileTransferAgent.isDone())));

		synchronized (downloadsProgress)
		{
			downloadsProgress.clear();
			downloadsProgress.addAll(newDownloadList);
		}
	}

	private void actionComputeUploadsProgress()
	{
		List<FileProgress> newUploadList = new ArrayList<>(seeders.size());
		seeders.forEach((sha1Sum, fileTransferAgent) -> newUploadList.add(
				new FileProgress(0L,
						fileTransferAgent.getFileName(),
						0L,
						fileTransferAgent.getFileProvider().getFileSize(),
						sha1Sum.toString(),
						fileTransferAgent.isDone())));

		synchronized (uploadsProgress)
		{
			uploadsProgress.clear();
			uploadsProgress.addAll(newUploadList);
		}
	}

	/**
	 * Add a peer to one of our downloads.
	 *
	 * @param hash     the hash
	 * @param location the source location to add for the file we're downloading
	 */
	private void actionAddPeer(Sha1Sum hash, Location location)
	{
		var leecher = leechers.get(hash);
		if (leecher != null)
		{
			leecher.addSeeder(location);
		}
	}

	private void actionRemovePeer(Sha1Sum hash, Location location)
	{
		var leecher = leechers.get(hash);
		if (leecher != null)
		{
			leecher.removePeer(location);
		}
	}

	private void actionReceiveDataRequest(Location location, Sha1Sum hash, long offset, int chunkSize)
	{
		log.debug("Received data request from {}, hash: {}, offset: {}, chunkSize: {}", location, hash, offset, chunkSize);
		FileTransferAgent agent;

		if (location.equals(ownLocation))
		{
			// Own requests must be passed to seeders
		}
		else
		{
			agent = seeders.get(hash);
			if (agent == null)
			{
				agent = localSearch(hash);
			}
			if (agent != null)
			{
				handleSeederRequest(location, agent, hash, offset, chunkSize);
			}
		}
	}

	private FileTransferAgent localSearch(Sha1Sum hash)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			return seeders.computeIfAbsent(hash, h -> fileService.findFilePathByHash(h)
					.map(Path::toFile)
					.map(file -> {
						log.debug("Serving file {} for hash {}", file, hash);
						var fileSeeder = new FileSeeder(file);
						if (!fileSeeder.open())
						{
							log.debug("Failed to open file {} for serving", file);
							return null;
						}
						return new FileTransferAgent(fileTransferRsService, file.getName(), h, fileSeeder);
					})
					.orElse(null));
		}
	}

	private void actionReceiveData(Location location, Sha1Sum hash, long offset, byte[] data)
	{
		var agent = leechers.get(hash);
		if (agent == null)
		{
			log.error("No matching agent for hash {} for receiving data", hash);
			return;
		}

		try
		{
			log.trace("Writing file {}, offset: {}, length: {}", agent.getFileName(), offset, data.length);
			// XXX: update location stats for writing (see how RS does it)
			var fileProvider = agent.getFileProvider();
			fileProvider.write(offset, data);
		}
		catch (IOException e)
		{
			log.error("Failed to write to file", e);
		}
	}

	private void actionReceiveChunkMapRequest(Location location, Sha1Sum hash, boolean isLeecher)
	{
		log.debug("Received {} chunk map request from {}, hash: {}", isLeecher ? "leecher (client)" : "seeder (server)", location, hash);
		if (isLeecher)
		{
			actionReceiveLeecherChunkMapRequest(location, hash);
		}
		else
		{
			actionReceiveSeederChunkMapRequest(location, hash);
		}
	}

	private void actionReceiveChunkMap(Location location, Sha1Sum hash, List<Integer> compressedChunkMap)
	{
		log.debug("Received chunk map from {}", location);
		var agent = leechers.get(hash);
		if (agent == null)
		{
			log.error("No matching agent for hash {} for chunk map", hash);
			return;
		}
		var chunkMap = ChunkMapUtils.toBitSet(compressedChunkMap);
		agent.addChunkMap(location, chunkMap);
	}

	private void actionReceiveLeecherChunkMapRequest(Location location, Sha1Sum hash)
	{
		var agent = leechers.get(hash);
		if (agent == null)
		{
			log.error("No matching agent for hash {} for chunk map request", hash);
			return;
		}
		var compressedChunkMap = ChunkMapUtils.toCompressedChunkMap(agent.getFileProvider().getChunkMap());
		fileTransferRsService.sendChunkMap(location, hash, false, compressedChunkMap);
	}

	private void actionReceiveSeederChunkMapRequest(Location location, Sha1Sum hash)
	{
		var agent = seeders.get(hash);
		if (agent == null)
		{
			agent = localSearch(hash);
		}

		if (agent == null)
		{
			log.error("Chunk map request succeeded but no seeder available");
			return;
		}

		var compressedChunkMap = ChunkMapUtils.toCompressedChunkMap(agent.getFileProvider().getChunkMap());
		fileTransferRsService.sendChunkMap(location, hash, true, compressedChunkMap);
	}

	private void actionReceiveChunkCrcRequest(Location location, Sha1Sum hash, int chunkNumber)
	{
		log.debug("Received chunk crc request from {}", location);
		var agent = seeders.get(hash);
		if (agent == null)
		{
			agent = localSearch(hash);
		}

		if (agent == null)
		{
			log.error("No matching agent for hash {} for chunk number {}", hash, chunkNumber);
			return;
		}

		// XXX: add a cache, queue for serving them later, etc...
		var checkSum = agent.getFileProvider().computeHash((long) chunkNumber * CHUNK_SIZE);
		if (checkSum != null)
		{
			fileTransferRsService.sendSingleChunkCrc(location, hash, chunkNumber, checkSum);
		}
	}

	private void actionReceiveChunkCrc(Location location, Sha1Sum hash, int chunkNumber, Sha1Sum checkSum)
	{
		log.debug("Received chunk crc from {}", location);
		// XXX: handle! need to check leecher...
	}

	private static void handleSeederRequest(Location location, FileTransferAgent agent, Sha1Sum hash, long offset, int chunkSize)
	{
		if (chunkSize > CHUNK_SIZE)
		{
			log.warn("Peer {} is requesting a too large chunk ({}) for hash {}, ignoring", location, chunkSize, hash);
			return;
		}
		// XXX: update location stats for reading, see how RS does it
		agent.addLeecher(location, offset, chunkSize);
	}
}
