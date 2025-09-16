/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static io.xeres.app.service.file.FileService.DOWNLOAD_EXTENSION;
import static io.xeres.app.service.file.FileService.DOWNLOAD_PREFIX;
import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.CHUNK_SIZE;

/**
 * File transfer management class.
 * <p>
 * <img src="doc-files/filetransfer.svg" alt="File transfer diagram">
 * The FileTransferManager manages several uploads and downloads. Each of them is represented by one {@link FileTransferAgent}.
 * <p>
 * A FileTransferAgent is paired with a {@link FileProvider} that is either a {@link FileDownload} or a {@link FileUpload} depending on the role of
 * that agent (respectively, download or upload a file).
 * <p>
 * Each FileTransferAgent has a list of seeders and leechers for itself.
 * <p>
 * Leechers ask for a slice between 1 byte and 1 MB. The result is always sent in packets of 8 KB max.
 * The goal is to send at the optimum speed depending on our bandwidth, the peer's bandwidth and the peer's RTT.
 * <p>
 * For requesting, ask for a chunk size of some small size, then monitor the speed and RTT while asking for more. We shouldn't
 * overflow our bandwidth nor the peer's one. We should also ask ahead of time for optimum speed including between chunks.
 */
class FileTransferManager implements Runnable
{
	private static final Logger log = LoggerFactory.getLogger(FileTransferManager.class);

	private static final int DEFAULT_TICK = 1000;

	private final FileTransferRsService fileTransferRsService;
	private final FileService fileService;
	private final SettingsService settingsService;
	private final LocationService locationService;
	private final DatabaseSessionManager databaseSessionManager;
	private final Location ownLocation;
	private final BlockingQueue<Action> queue;
	private final FileTransferStrategy fileTransferStrategy;

	private final Map<Sha1Sum, FileTransferAgent> downloads = new HashMap<>(); // files that we are downloading (client)
	private final Map<Sha1Sum, FileTransferAgent> uploads = new HashMap<>(); // files that we are uploading (serving)

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
				var action = getNextAction();
				processAction(action);
				processDownloads();
				processUploads();
			}
			catch (InterruptedException _)
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
		downloads.forEach((hash, download) -> fileService.suspendDownload(hash, download.getFileProvider().getChunkMap()));
	}

	private Action getNextAction() throws InterruptedException
	{
		if (downloads.isEmpty() && uploads.isEmpty())
		{
			return queue.take();
		}
		else
		{
			return queue.poll(computeOptimalWaitingTime(), TimeUnit.MILLISECONDS);
		}
	}

	private long computeOptimalWaitingTime()
	{
		var now = Instant.now();
		int minWaitingTime = DEFAULT_TICK;

		var agents = Stream.concat(downloads.values().stream(), uploads.values().stream())
				.toList();

		for (var agent : agents)
		{
			minWaitingTime = Math.min(minWaitingTime, durationBetween(now, agent.getNextDelay()));
			if (minWaitingTime == 0)
			{
				break;
			}
		}
		log.debug("Calculated optimal time: {}", minWaitingTime);
		return minWaitingTime;
	}

	private static int durationBetween(Instant now, Instant nextDelay)
	{
		var duration = Duration.between(now, nextDelay);
		if (duration.isNegative())
		{
			return 0;
		}
		return safeLongToInt(duration.toMillis());
	}

	private static int safeLongToInt(long value)
	{
		if (value > Integer.MAX_VALUE)
		{
			return Integer.MAX_VALUE;
		}
		return (int) value;
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

	private void processDownloads()
	{
		downloads.forEach((_, download) -> download.process());
	}

	private void processUploads()
	{
		uploads.entrySet().removeIf(upload -> stopStalledUpload(upload.getValue()));
		uploads.forEach((_, upload) -> upload.process());
	}

	private boolean stopStalledUpload(FileTransferAgent upload)
	{
		if (upload.isIdle())
		{
			upload.stop();
			return true;
		}
		return false;
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
			downloads.computeIfAbsent(hash, sha1Sum -> {
				var file = Paths.get(settingsService.getIncomingDirectory(), DOWNLOAD_PREFIX + sha1Sum + DOWNLOAD_EXTENSION).toFile();
				log.debug("Downloading file {}, size: {}, from: {}", file, size, from);
				var fileDownload = new FileDownload(id, file, size, chunkMap, from != null ? FileTransferStrategy.LINEAR : fileTransferStrategy);
				if (fileDownload.open())
				{
					var download = new FileTransferAgent(fileTransferRsService, name, sha1Sum, fileDownload);
					if (from != null)
					{
						download.setTrusted(true);
						locationService.findLocationByLocationIdentifier(from).ifPresent(download::addSeeder);
					}
					else
					{
						fileTransferRsService.activateTunnels(sha1Sum);
					}
					return download;
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
				var download = downloads.get(fileDownload.getHash());
				if (download != null)
				{
					download.cancel();
					downloads.remove(fileDownload.getHash());
				}
				fileService.removeDownload(id);
			});
		}
	}

	private void actionComputeDownloadsProgress()
	{
		List<FileProgress> newDownloadList = new ArrayList<>(downloads.size());
		downloads.forEach((sha1Sum, download) -> newDownloadList.add(
				new FileProgress(download.getFileProvider().getId(),
						download.getFileName(),
						download.getFileProvider().getBytesWritten(),
						download.getFileProvider().getFileSize(),
						sha1Sum.toString(),
						download.isDone())));

		synchronized (downloadsProgress)
		{
			downloadsProgress.clear();
			downloadsProgress.addAll(newDownloadList);
		}
	}

	private void actionComputeUploadsProgress()
	{
		List<FileProgress> newUploadList = new ArrayList<>(uploads.size());
		uploads.forEach((sha1Sum, upload) -> newUploadList.add(
				new FileProgress(0L,
						upload.getFileName(),
						0L,
						upload.getFileProvider().getFileSize(),
						sha1Sum.toString(),
						upload.isDone())));

		synchronized (uploadsProgress)
		{
			uploadsProgress.clear();
			uploadsProgress.addAll(newUploadList);
		}
	}

	/**
	 * Adds a peer to one of our downloads.
	 *
	 * @param hash     the hash of the file being downloaded
	 * @param location the source location to add
	 */
	private void actionAddPeer(Sha1Sum hash, Location location)
	{
		var download = downloads.get(hash);
		if (download != null)
		{
			download.addSeeder(location);
		}
	}

	/**
	 * Removes a peer from one of our downloads.
	 *
	 * @param hash     the hash of the file being downloaded
	 * @param location the source location to remove
	 */
	private void actionRemovePeer(Sha1Sum hash, Location location)
	{
		var download = downloads.get(hash);
		if (download != null)
		{
			download.removePeer(location);
		}
	}

	private void actionReceiveDataRequest(Location location, Sha1Sum hash, long offset, int chunkSize)
	{
		log.debug("Received data request from {}, hash: {}, offset: {}, chunkSize: {}", location, hash, offset, chunkSize);
		FileTransferAgent upload;

		//noinspection StatementWithEmptyBody
		if (location.equals(ownLocation))
		{
			// Own requests must be passed to seeders
		}
		else
		{
			upload = uploads.get(hash);
			if (upload == null)
			{
				upload = localSearch(hash);
			}
			if (upload != null)
			{
				handleLeecherRequest(location, upload, hash, offset, chunkSize);
			}
		}
	}

	private FileTransferAgent localSearch(Sha1Sum hash)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			return uploads.computeIfAbsent(hash, h -> fileService.findFilePathByHash(h)
					.map(Path::toFile)
					.map(file -> {
						log.debug("Serving file {} for hash {}", file, hash);
						var upload = new FileUpload(file);
						if (!upload.open())
						{
							log.debug("Failed to open file {} for serving", file);
							return null;
						}
						return new FileTransferAgent(fileTransferRsService, file.getName(), h, upload);
					})
					.orElse(null));
		}
	}

	private void actionReceiveData(Location location, Sha1Sum hash, long offset, byte[] data)
	{
		var download = downloads.get(hash);
		if (download == null)
		{
			log.error("No matching download agent for hash {}", hash);
			return;
		}

		try
		{
			log.trace("Writing file {}, offset: {}, length: {}", download.getFileName(), offset, data.length);
			// XXX: update location stats for writing (see how RS does it)
			var fileProvider = download.getFileProvider();
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
		var download = downloads.get(hash);
		if (download == null)
		{
			log.error("No matching download agent for hash {} for chunk map", hash);
			return;
		}
		var chunkMap = ChunkMapUtils.toBitSet(compressedChunkMap);
		download.addChunkMap(location, chunkMap);
	}

	private void actionReceiveLeecherChunkMapRequest(Location location, Sha1Sum hash)
	{
		var download = downloads.get(hash);
		if (download == null)
		{
			log.error("No matching download agent for hash {} for chunk map request", hash);
			return;
		}
		var compressedChunkMap = ChunkMapUtils.toCompressedChunkMap(download.getFileProvider().getChunkMap());
		fileTransferRsService.sendChunkMap(location, hash, false, compressedChunkMap);
	}

	private void actionReceiveSeederChunkMapRequest(Location location, Sha1Sum hash)
	{
		var upload = uploads.get(hash);
		if (upload == null)
		{
			upload = localSearch(hash);
		}

		if (upload == null)
		{
			log.error("Chunk map request succeeded but no seeder available");
			return;
		}

		var compressedChunkMap = ChunkMapUtils.toCompressedChunkMap(upload.getFileProvider().getChunkMap());
		fileTransferRsService.sendChunkMap(location, hash, true, compressedChunkMap);
	}

	private void actionReceiveChunkCrcRequest(Location location, Sha1Sum hash, int chunkNumber)
	{
		log.debug("Received chunk crc request from {}", location);
		var upload = uploads.get(hash);
		if (upload == null)
		{
			upload = localSearch(hash);
		}

		if (upload == null)
		{
			log.error("No matching upload agent for hash {} for chunk number {}", hash, chunkNumber);
			return;
		}

		// XXX: add a cache, queue for serving them later, etc...
		var checkSum = upload.getFileProvider().computeHash((long) chunkNumber * CHUNK_SIZE);
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

	private static void handleLeecherRequest(Location location, FileTransferAgent upload, Sha1Sum hash, long offset, int chunkSize)
	{
		if (chunkSize > CHUNK_SIZE)
		{
			log.warn("Peer {} is requesting a too large chunk ({}) for hash {}, ignoring", location, chunkSize, hash);
			return;
		}
		// XXX: update location stats for reading, see how RS does it
		upload.addLeecher(location, offset, chunkSize);
	}
}
