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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

class FileTransferAgent
{
	private static final Logger log = LoggerFactory.getLogger(FileTransferAgent.class);

	private final FileTransferRsService fileTransferRsService;
	private final FileProvider fileProvider;
	private final Sha1Sum hash;
	private String fileName;

	private final Map<Location, ChunkSender> senders = new LinkedHashMap<>();
	private final Map<Location, ChunkReceiver> receivers = new LinkedHashMap<>();

	public FileTransferAgent(FileTransferRsService fileTransferRsService, String fileName, Sha1Sum hash, FileProvider fileProvider)
	{
		this(fileTransferRsService, hash, fileProvider);
		this.fileName = fileName;
	}

	public FileTransferAgent(FileTransferRsService fileTransferRsService, Sha1Sum hash, FileProvider fileProvider)
	{
		this.fileTransferRsService = fileTransferRsService;
		this.hash = hash;
		this.fileProvider = fileProvider;
	}

	public FileProvider getFileProvider()
	{
		return fileProvider;
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
						log.debug("Receiving file...");
						if (isChunkReceived(entry.getValue().getChunkNumber()))
						{
							log.debug("Chunk fully received");
							entry.getValue().setReceiving(false);
						}
					}
					else
					{
						if (fileProvider.isComplete())
						{
							log.debug("File is complete, renaming to {}", fileName);
							fileProvider.close();
							var filePath = fileProvider.getPath();
							try
							{
								Files.move(filePath, filePath.resolveSibling(fileName));
							}
							catch (FileAlreadyExistsException e)
							{
								log.error("Name already exists!");
								// XXX: rename the file with (1) or so and try again... what does RS do?
							}
							catch (IOException e)
							{
								throw new RuntimeException(e);
							}
							receivers.remove(entry.getKey());
						}
						else
						{
							var chunkNumber = getNextChunk();
							log.debug("Getting chunk number {}", chunkNumber);
							fileTransferRsService.sendDataRequest(entry.getKey(), hash, fileProvider.getFileSize(), (long) chunkNumber * FileTransferRsService.CHUNK_SIZE, FileTransferRsService.CHUNK_SIZE);
							entry.getValue().setChunkNumber(chunkNumber);
							entry.getValue().setReceiving(true);
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

	/**
	 * Gets the next available chunk.
	 *
	 * @return the chunk number
	 */
	private int getNextChunk()
	{
		if (fileProvider.isComplete())
		{
			throw new IllegalStateException("Cannot find the next chunk of a complete file");
		}
		var compressedChunkMap = fileProvider.getCompressedChunkMap();

		for (int i = 0; i < compressedChunkMap.size(); i++)
		{
			var slice = compressedChunkMap.get(i);
			if (slice != 0xffffffff)
			{
				return i * 32 + getChunkIndex(slice);
			}
		}
		throw new IllegalStateException("Cannot find the next chunk of an incomplete file. This is impossible");
	}

	private int getChunkIndex(int slice)
	{
		for (int i = 0; i < 32; i++)
		{
			if ((slice & 1 << i) == 0)
			{
				return i;
			}
		}
		throw new IllegalStateException("Slice has no chunk available, this is impossible");
	}

	private boolean isChunkReceived(int chunkNumber)
	{
		var compressedChunkMap = fileProvider.getCompressedChunkMap();

		var slice = compressedChunkMap.get(chunkNumber / 32);

		return (slice & 1 << (chunkNumber % 32)) != 0;
	}
}