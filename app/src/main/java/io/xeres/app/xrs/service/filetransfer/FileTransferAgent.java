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

import io.xeres.app.database.model.location.Location;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.util.FileNameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Responsible for sending/receiving a file.
 */
class FileTransferAgent
{
	private static final Logger log = LoggerFactory.getLogger(FileTransferAgent.class);

	private final FileTransferRsService fileTransferRsService;
	private final FileProvider fileProvider;
	private final Sha1Sum hash;
	private final String fileName;

	private final Map<Location, ChunkSender> senders = new LinkedHashMap<>();
	private final Map<Location, ChunkReceiver> receivers = new LinkedHashMap<>();

	public FileTransferAgent(FileTransferRsService fileTransferRsService, String fileName, Sha1Sum hash, FileProvider fileProvider)
	{
		this.fileTransferRsService = fileTransferRsService;
		this.hash = hash;
		this.fileProvider = fileProvider;
		this.fileName = fileName;
	}

	public FileProvider getFileProvider()
	{
		return fileProvider;
	}

	public String getFileName()
	{
		return fileName;
	}

	public void addPeerForReceiving(Location peer)
	{
		receivers.computeIfAbsent(peer, k -> new ChunkReceiver());
	}

	public void addPeerForSending(Location peer, long offset, int size)
	{
		senders.computeIfAbsent(peer, k -> new ChunkSender(fileTransferRsService, peer, fileProvider, hash, fileProvider.getFileSize(), offset, size));
	}

	public void removePeer(Location peer)
	{
		if (receivers.remove(peer) == null && senders.remove(peer) == null)
		{
			log.warn("Removal of peer {} failed because it's not in the list. This shouldn't happen.", peer);
		}
	}

	/**
	 * Processes file transfers.
	 *
	 * @return true if processing, false if there's nothing to process
	 */
	public boolean process()
	{
		processDownloads();
		processUploads();
		return !(senders.isEmpty() && receivers.isEmpty());
	}

	private void processDownloads()
	{
		receivers.entrySet().stream()
				.skip(getRandomStreamSkip(receivers.size()))
				.findFirst().ifPresent(entry -> {
					if (entry.getValue().isReceiving())
					{
						if (fileProvider.hasChunk(entry.getValue().getChunkNumber()))
						{
							log.debug("Chunk {} fully received", entry.getValue().getChunkNumber());
							entry.getValue().setReceiving(false);
						}
					}
					else
					{
						if (fileProvider.isComplete())
						{
							log.debug("File is complete, size: {}, renaming to {}", fileProvider.getFileSize(), fileName);
							fileProvider.close();
							fileTransferRsService.markDownloadAsCompleted(hash);
							fileTransferRsService.deactivateTunnels(hash);
							renameFile(fileProvider.getPath(), fileName);
							receivers.remove(entry.getKey());
						}
						else
						{
							getNextChunk().ifPresent(chunkNumber -> {
								log.debug("Requesting chunk number {} to peer {}", chunkNumber, entry.getKey());
								fileTransferRsService.sendDataRequest(entry.getKey(), hash, fileProvider.getFileSize(), (long) chunkNumber * FileTransferRsService.CHUNK_SIZE, FileTransferRsService.CHUNK_SIZE);
								entry.getValue().setChunkNumber(chunkNumber);
								entry.getValue().setReceiving(true);
							});
						}
					}
				});
	}

	private void processUploads()
	{
		senders.entrySet().stream()
				.skip(getRandomStreamSkip(senders.size()))
				.findFirst().ifPresent(entry -> {
					var chunkSender = entry.getValue();
					var remaining = chunkSender.send();
					if (!remaining)
					{
						senders.remove(entry.getKey());
						if (senders.isEmpty())
						{
							fileProvider.close();
						}
					}
				});
	}

	private static int getRandomStreamSkip(int size)
	{
		if (size == 0)
		{
			return 0;
		}
		return ThreadLocalRandom.current().nextInt(size);
	}

	private static void renameFile(Path filePath, String fileName)
	{
		var success = false;

		while (!success)
		{
			try
			{
				Files.move(filePath, filePath.resolveSibling(fileName));
				success = true;
			}
			catch (FileAlreadyExistsException e)
			{
				log.warn("File name {} already exists, renaming...", fileName);
				fileName = FileNameUtils.rename(fileName);
			}
			catch (IOException e)
			{
				log.error("Couldn't rename the file {} to {}", filePath, fileName, e);
				success = true; // This is really a failure but there's nothing else we can do
			}
		}
	}

	/**
	 * Gets the next available chunk.
	 *
	 * @return the chunk number
	 */
	private Optional<Integer> getNextChunk()
	{
		return fileProvider.getNeededChunk();
	}
}