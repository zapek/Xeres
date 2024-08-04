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
import io.xeres.app.service.SettingsService;
import io.xeres.app.service.file.FileService;
import io.xeres.app.xrs.service.filetransfer.item.FileTransferChunkMapRequestItem;
import io.xeres.app.xrs.service.filetransfer.item.FileTransferDataRequestItem;
import io.xeres.app.xrs.service.filetransfer.item.FileTransferSingleChunkCrcRequestItem;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.rest.file.FileProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.CHUNK_SIZE;

/**
 * File transfer class.
 * <p>
 * <img src="doc-files/filetransfer.png" alt="File transfer diagram">
 */
class FileTransferManager implements Runnable
{
	private static final Logger log = LoggerFactory.getLogger(FileTransferManager.class);

	private static final int DEFAULT_TICK = 250;

	private final FileTransferRsService fileTransferRsService;
	private final FileService fileService;
	private final SettingsService settingsService;
	private final DatabaseSessionManager databaseSessionManager;
	private final Location ownLocation;
	private final BlockingQueue<FileTransferCommand> queue;
	private final FileTransferStrategy fileTransferStrategy;

	private final Map<Sha1Sum, FileTransferAgent> leechers = new HashMap<>();
	private final Map<Sha1Sum, FileTransferAgent> seeders = new HashMap<>();

	private final List<FileProgress> downloadsProgress = new ArrayList<>();
	private final List<FileProgress> uploadsProgress = new ArrayList<>();

	public FileTransferManager(FileTransferRsService fileTransferRsService, FileService fileService, SettingsService settingsService, DatabaseSessionManager databaseSessionManager, Location ownLocation, BlockingQueue<FileTransferCommand> queue, FileTransferStrategy fileTransferStrategy)
	{
		this.fileTransferRsService = fileTransferRsService;
		this.fileService = fileService;
		this.settingsService = settingsService;
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
				FileTransferCommand command = (leechers.isEmpty() && seeders.isEmpty()) ? queue.take() : queue.poll(DEFAULT_TICK, TimeUnit.MILLISECONDS); // XXX: change the timeout value... or better... have a way to compute the next one
				processCommand(command);
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

	private void processCommand(FileTransferCommand command)
	{
		switch (command)
		{
			case FileTransferCommandItem commandItem -> processItem(commandItem);
			case FileTransferCommandAction commandAction -> processAction(commandAction);
			case null, default ->
			{
				// Nothing to do
			}
		}

	}

	private void processLeechers()
	{
		leechers.forEach((hash, agent) -> agent.process());
	}

	private void processSeeders()
	{
		seeders.entrySet().removeIf(agent -> !agent.getValue().process());
	}

	private void processItem(FileTransferCommandItem commandItem)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			log.debug("Processing item {}", commandItem.item());
			if (commandItem.item() instanceof FileTransferDataRequestItem item)
			{
				handleReceiveDataRequest(commandItem.location(), item);
			}
			else if (commandItem.item() instanceof FileTransferChunkMapRequestItem item)
			{
				if (item.isLeecher())
				{
					handleReceiveLeecherChunkMapRequest(commandItem.location(), item);
				}
				else
				{
					handleReceiveSeederChunkMapRequest(commandItem.location(), item);
				}
			}
			else if (commandItem.item() instanceof FileTransferSingleChunkCrcRequestItem item)
			{
				handleReceiveChunkCrcRequest(commandItem.location(), item);
			}
		}
	}

	private void processAction(FileTransferCommandAction commandAction)
	{
		log.debug("Processing action {}", commandAction.action());
		if (commandAction.action() instanceof ActionReceiveData action)
		{
			actionReceiveData(action);
		}
		else if (commandAction.action() instanceof ActionDownload action)
		{
			actionDownloadFile(action);
		}
		else if (commandAction.action() instanceof ActionGetDownloadsProgress)
		{
			actionComputeDownloadsProgress();
		}
		else if (commandAction.action() instanceof ActionGetUploadsProgress)
		{
			actionComputeUploadsProgress();
		}
		else if (commandAction.action() instanceof ActionAddPeer addPeer)
		{
			actionAddPeer(addPeer.hash(), addPeer.location());
		}
		else if (commandAction.action() instanceof ActionRemovePeer removePeer)
		{
			actionRemovePeer(removePeer.hash(), removePeer.location());
		}
	}

	private void actionDownloadFile(ActionDownload actionDownload)
	{
		leechers.computeIfAbsent(actionDownload.hash(), hash -> {
			var file = Paths.get(settingsService.getIncomingDirectory(), hash + FileService.DOWNLOAD_EXTENSION).toFile();
			log.debug("Downloading file {}, size: {}", file, actionDownload.size());
			var fileLeecher = new FileLeecher(file, actionDownload.size(), actionDownload.chunkMap(), actionDownload.from() != null ? FileTransferStrategy.LINEAR : fileTransferStrategy);
			if (fileLeecher.open())
			{
				var agent = new FileTransferAgent(fileTransferRsService, actionDownload.name(), hash, fileLeecher);
				if (actionDownload.from() != null)
				{
					agent.addPeerForReceiving(actionDownload.from());
				}
				else
				{
					fileTransferRsService.activateTunnels(actionDownload.hash());
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

	private void actionComputeDownloadsProgress()
	{
		List<FileProgress> newDownloadList = new ArrayList<>(leechers.size());
		leechers.forEach((sha1Sum, fileTransferAgent) -> newDownloadList.add(
				new FileProgress(fileTransferAgent.getFileName(),
						fileTransferAgent.getFileProvider().getBytesWritten(),
						fileTransferAgent.getFileProvider().getFileSize(),
						sha1Sum.toString())));

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
				new FileProgress(fileTransferAgent.getFileName(),
						0,
						fileTransferAgent.getFileProvider().getFileSize(),
						sha1Sum.toString())));

		synchronized (uploadsProgress)
		{
			uploadsProgress.clear();
			uploadsProgress.addAll(newUploadList);
		}
	}

	private void actionAddPeer(Sha1Sum hash, Location location)
	{
		var leecher = leechers.get(hash);
		if (leecher != null)
		{
			leecher.addPeerForReceiving(location);
			//fileTransferRsService.sendChunkMapRequest(location, hash, true); // XXX: this shouldn't be done here but in the download loop (in FileTransferAgent)
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

	private void handleReceiveDataRequest(Location location, FileTransferDataRequestItem item)
	{
		log.debug("Received data request from {}: {}", location, item);
		FileTransferAgent agent;

		if (location.equals(ownLocation))
		{
			// Own requests must be passed to seeders
		}
		else
		{
			agent = leechers.get(item.getFileItem().hash());
			if (agent == null)
			{
				agent = localSearch(item.getFileItem().hash());
			}
			if (agent != null)
			{
				handleSeederRequest(location, agent, item.getFileItem().hash(), item.getFileItem().size(), item.getFileOffset(), item.getChunkSize());
			}
		}
	}

	private FileTransferAgent localSearch(Sha1Sum hash)
	{
		return seeders.computeIfAbsent(hash, h -> fileService.findFilePathByHash(h)
				.map(Path::toFile)
				.map(file -> {
					log.debug("Serving file {}", file);
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

	private void actionReceiveData(ActionReceiveData action)
	{
		var agent = leechers.get(action.hash());
		if (agent == null)
		{
			log.error("No matching agent for hash {} for receiving data", action.hash());
			return;
		}

		try
		{
			log.debug("Writing file {}, offset: {}, length: {}", agent.getFileProvider(), action.offset(), action.data().length);
			// XXX: update location stats for writing (see how RS does it)
			var fileProvider = agent.getFileProvider();
			fileProvider.write(action.offset(), action.data());
		}
		catch (IOException e)
		{
			log.error("Failed to write to file", e);
		}
	}

	private void handleReceiveLeecherChunkMapRequest(Location location, FileTransferChunkMapRequestItem item)
	{
		var agent = leechers.get(item.getHash());
		if (agent == null)
		{
			log.error("No matching agent for hash {} for chunk map request", item.getHash());
			return;
		}
		var compressedChunkMap = toCompressedChunkMap(agent.getFileProvider().getChunkMap());
		fileTransferRsService.sendChunkMap(location, item.getHash(), false, compressedChunkMap);
	}

	private void handleReceiveSeederChunkMapRequest(Location location, FileTransferChunkMapRequestItem item)
	{
		var agent = seeders.get(item.getHash());
		if (agent == null)
		{
			agent = localSearch(item.getHash());
		}

		if (agent == null)
		{
			log.error("Search request succeeded but no seeder available");
			return;
		}

		var compressedChunkMap = toCompressedChunkMap(agent.getFileProvider().getChunkMap());
		fileTransferRsService.sendChunkMap(location, item.getHash(), true, compressedChunkMap);
	}

	private void handleReceiveChunkCrcRequest(Location location, FileTransferSingleChunkCrcRequestItem item)
	{
		// XXX: not sure what to do yet, complicated (look at the list of seeders first, etc...)
	}

	private void handleSeederRequest(Location location, FileTransferAgent agent, Sha1Sum hash, long size, long offset, int chunkSize)
	{
		if (chunkSize > CHUNK_SIZE)
		{
			log.warn("Peer {} is requesting a too large chunk ({}) for hash {}, ignoring", location, chunkSize, hash);
			return;
		}
		// XXX: update location stats for reading, see how RS does it
		agent.addPeerForSending(location, offset, chunkSize);
	}

	/**
	 * Converts the chunkMap to the format used by RS. Note that there might
	 * be spurious unset chunks at the end. This is normal and RS also does that
	 * because the file size is taken into account when searching chunks.
	 *
	 * @param chunkMap the chunk map
	 * @return a compressed chunk map
	 */
	static List<Integer> toCompressedChunkMap(BitSet chunkMap)
	{
		var intBuf = ByteBuffer.wrap(alignArray(chunkMap.toByteArray()))
				.order(ByteOrder.LITTLE_ENDIAN)
				.asIntBuffer();
		var ints = new int[intBuf.remaining()];
		intBuf.get(ints);
		return Arrays.stream(ints).boxed().toList();
	}

	/**
	 * Aligns the array to an integer (32-bits) boundary.
	 *
	 * @param src the source array
	 * @return the array aligned to an integer boundary
	 */
	private static byte[] alignArray(byte[] src)
	{
		if (src.length % 4 != 0)
		{
			byte[] dst = new byte[src.length + (4 - src.length % 4)];
			System.arraycopy(src, 0, dst, 0, src.length);
			return dst;
		}
		return src;
	}
}
