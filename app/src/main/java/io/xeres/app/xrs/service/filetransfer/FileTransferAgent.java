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

import io.xeres.app.database.model.location.Location;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.util.FileNameUtils;
import io.xeres.common.util.OsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Responsible for sending/receiving a file.
 */
class FileTransferAgent
{
	private static final Logger log = LoggerFactory.getLogger(FileTransferAgent.class);

	private static final long IDLE_TIME = Duration.ofMinutes(5).toNanos();

	private final FileTransferRsService fileTransferRsService;
	private final FileProvider fileProvider;
	private final Sha1Sum hash;
	private final String fileName;
	private boolean done;
	private long lastActivity;
	private boolean trusted;

	private final Map<Location, List<ChunkSender>> leechers = new LinkedHashMap<>();
	private final Map<Location, ChunkReceiver> seeders = new LinkedHashMap<>();

	public FileTransferAgent(FileTransferRsService fileTransferRsService, String fileName, Sha1Sum hash, FileProvider fileProvider)
	{
		this.fileTransferRsService = fileTransferRsService;
		this.hash = hash;
		this.fileProvider = fileProvider;
		this.fileName = fileName;
		lastActivity = System.nanoTime();
	}

	public void setTrusted(boolean trusted)
	{
		this.trusted = trusted;
	}

	public FileProvider getFileProvider()
	{
		return fileProvider;
	}

	public String getFileName()
	{
		return fileName;
	}

	public void addSeeder(Location peer)
	{
		seeders.computeIfAbsent(peer, k -> new ChunkReceiver());
		fileTransferRsService.sendChunkMapRequest(peer, hash, false);
	}

	public void addLeecher(Location peer, long offset, int size)
	{
		leechers.computeIfAbsent(peer, k -> new LinkedList<>())
				.add(new ChunkSender(fileTransferRsService, peer, fileProvider, hash, fileProvider.getFileSize(), offset, size));
	}

	public void removePeer(Location peer)
	{
		if (seeders.remove(peer) == null && leechers.remove(peer) == null)
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
		processSeeders();
		processLeechers();
		return !(leechers.isEmpty() && seeders.isEmpty());
	}

	public void cancel()
	{
		if (!fileProvider.isComplete())
		{
			fileProvider.closeAndDelete();
		}
	}

	public void stop()
	{
		fileProvider.close();
	}

	public void addChunkMap(Location peer, BitSet chunkMap)
	{
		var seeder = seeders.get(peer);
		if (seeder == null)
		{
			log.error("Seeder not found for adding chunkmap");
			return;
		}
		seeder.setChunkMap(chunkMap);
	}

	public boolean isIdle()
	{
		// XXX: only works for uploads (seeders). Should also work for downloads but that's on the side of file provider.
		return System.nanoTime() - lastActivity > IDLE_TIME;
	}

	public boolean isDone()
	{
		return done;
	}

	private void processSeeders()
	{
		seeders.entrySet().stream()
				.skip(getRandomStreamSkip(seeders.size()))
				.findFirst().ifPresent(entry -> {
					if (entry.getValue().isReceiving())
					{
						if (fileProvider.hasChunk(entry.getValue().getChunkNumber()))
						{
							log.debug("Chunk {} is complete", entry.getValue().getChunkNumber());
							entry.getValue().setReceiving(false);
						}
					}
					else
					{
						if (fileProvider.isComplete() && !done)
						{
							log.debug("File is complete, size: {}, renaming to {}", fileProvider.getFileSize(), fileName);
							stop();
							fileTransferRsService.markDownloadAsCompleted(hash);
							fileTransferRsService.deactivateTunnels(hash);
							var newPath = renameFile(fileProvider.getPath(), fileName);
							setFileSecurity(newPath);
							seeders.remove(entry.getKey());
							done = true; // Prevents closing the file several times (we might have several seeders)
						}
						else
						{
							if (entry.getValue().hasChunkMap())
							{
								getNextChunk(entry.getValue().getChunkMap()).ifPresent(chunkNumber -> {
									log.debug("Requesting chunk number {} to peer {}", chunkNumber, entry.getKey());
									fileTransferRsService.sendDataRequest(entry.getKey(), hash, fileProvider.getFileSize(), (long) chunkNumber * FileTransferRsService.CHUNK_SIZE, FileTransferRsService.CHUNK_SIZE);
									entry.getValue().setChunkNumber(chunkNumber);
									entry.getValue().setReceiving(true);
								});
							}
						}
					}
				});
	}

	private void setFileSecurity(Path path)
	{
		if (path != null)
		{
			OsUtils.setFileSecurity(path, trusted);
		}
	}

	private void processLeechers()
	{
		leechers.entrySet().stream()
				.skip(getRandomStreamSkip(leechers.size()))
				.findFirst().ifPresent(entry -> {
					var chunkList = entry.getValue();
					var chunkSender = chunkList.getFirst();
					var remaining = chunkSender.send();
					lastActivity = System.nanoTime();
					if (!remaining)
					{
						// We just remove the leecher here and nothing else. The fileTransferManager will close the file
						// when it's idle for some time, otherwise it would need to be reopened immediately for the
						// next chunk.
						chunkList.remove(chunkSender);
						if (chunkList.isEmpty())
						{
							leechers.remove(entry.getKey());
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

	private static Path renameFile(Path filePath, String fileName)
	{
		var success = false;
		Path path = null;

		while (!success)
		{
			try
			{
				var newPath = filePath.resolveSibling(fileName);
				Files.move(filePath, newPath);
				OsUtils.setFileVisible(newPath, true);
				success = true;
				path = newPath;
			}
			catch (FileAlreadyExistsException e)
			{
				log.warn("File name {} already exists, renaming...", fileName);
				fileName = FileNameUtils.rename(fileName);
			}
			catch (InvalidPathException e)
			{
				log.warn("File name {} is invalid, trying to fix the characters...", fileName);
				var newFileName = OsUtils.sanitizeFileName(fileName);
				if (newFileName.equals(fileName))
				{
					fileName = "InvalidFileName_RenameMe";
					log.error("Couldn't find a proper name for file {}, using: {}. Rename by hand and report", filePath, fileName);
				}
				else
				{
					fileName = newFileName;
				}
			}
			catch (IOException e)
			{
				log.error("Couldn't rename the file {} to {}", filePath, fileName, e);
				success = true; // This is really a failure but there's nothing else we can do
			}
		}
		return path;
	}

	/**
	 * Gets the next available chunk.
	 *
	 * @return the chunk number
	 */
	private Optional<Integer> getNextChunk(BitSet chunkMap)
	{
		return fileProvider.getNeededChunk(chunkMap);
	}
}