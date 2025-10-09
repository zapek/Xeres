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
import java.time.Instant;
import java.util.*;

/**
 * Responsible for sending/receiving one file.
 * There can be several leechers or seeders per file.
 */
class FileTransferAgent
{
	private static final Logger log = LoggerFactory.getLogger(FileTransferAgent.class);

	/**
	 * Time after which a download is considered stale.
	 */
	private static final long IDLE_TIME = Duration.ofMinutes(5).toNanos();

	private final FileTransferRsService fileTransferRsService;
	private final FileProvider fileProvider;
	private final Sha1Sum hash;
	private final String fileName;
	private boolean done;
	private long lastActivity;
	private boolean trusted;

	private final Map<Location, FileLeecher> leechers = new LinkedHashMap<>();
	private final Map<Location, FileSeeder> seeders = new LinkedHashMap<>();

	private final PriorityQueue<FilePeer> queue = new PriorityQueue<>();

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
		seeders.computeIfAbsent(peer, _ -> {
			var fileSeeder = new FileSeeder(peer);
			queue.add(fileSeeder);
			return fileSeeder;
		});
		fileTransferRsService.sendChunkMapRequest(peer, hash, false);
	}

	public void addLeecher(Location peer, long offset, int size)
	{
		leechers.computeIfAbsent(peer, _ -> {
			var fileLeecher = new FileLeecher(peer);
			queue.add(fileLeecher);
			return fileLeecher;
		}).addSliceSender(new SliceSender(fileTransferRsService, peer, fileProvider, hash, fileProvider.getFileSize(), offset, size));
	}

	public void removePeer(Location peer)
	{
		FilePeer removed = seeders.remove(peer);
		if (removed == null)
		{
			removed = leechers.remove(peer);
		}

		if (removed == null)
		{
			log.warn("Removal of peer {} failed because it's not in the list. This shouldn't happen.", peer);
		}
		queue.remove(removed);
	}

	/**
	 * Processes file transfers.
	 *
	 * @return true if processing, false if there's nothing to process
	 */
	public boolean process()
	{
		processPeers();
		return queue.isEmpty();
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
		seeder.updateChunkMap(chunkMap);
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

	/**
	 * Returns the next desired processing.
	 *
	 * @return when the next processing happens, null if there's no processing needed
	 */
	public Instant getNextProcessing()
	{
		var filePeer = queue.peek();
		if (filePeer != null)
		{
			return filePeer.getNextScheduling();
		}
		return null;
	}

	private void processPeers()
	{
		var filePeer = queue.poll();
		switch (filePeer)
		{
			case FileSeeder fileSeeder -> processSeeder(fileSeeder);
			case FileLeecher fileLeecher -> processLeecher(fileLeecher);
			case null ->
			{
				// Empty queue
			}
			default -> throw new IllegalStateException("Unhandled peer class");
		}

	}

	private void processSeeder(FileSeeder fileSeeder)
	{
		if (fileSeeder.isReceiving())
		{
			if (fileProvider.hasChunk(fileSeeder.getChunkNumber()))
			{
				log.debug("Chunk {} is complete", fileSeeder.getChunkNumber());
				fileSeeder.setReceiving(false);
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
				removePeer(fileSeeder.getLocation());
				done = true; // Prevents closing the file several times (we might have several seeders)
				return; // Don't reinsert in the queue
			}
			else
			{
				if (fileSeeder.hasChunkMap())
				{
					getNextChunk(fileSeeder.getChunkMap()).ifPresent(chunkNumber -> {
						log.debug("Requesting chunk number {} to peer {}", chunkNumber, fileSeeder.getLocation());
						fileTransferRsService.sendDataRequest(fileSeeder.getLocation(), hash, fileProvider.getFileSize(), (long) chunkNumber * FileTransferRsService.CHUNK_SIZE, FileTransferRsService.CHUNK_SIZE);
						fileSeeder.setChunkNumber(chunkNumber);
						fileSeeder.setReceiving(true);
					});
				}
			}
		}
		// Calculating the next computation would require guessing when we need to ask for the
		// next chunk. Right now we ask for 1 MB, but we should ask for smaller and progressively bigger (up to 1 MB).
		addNextScheduling(fileSeeder, Duration.ofMillis(250)); // XXX: use a real computation... not sure it needs to be done in each process*()... maybe in the processPeer() only? check...
		// XXX: also to know the bandwidth, we have to know to which tunnelId the virtual location maps to, then to which peer the tunnelId maps to and we finally got a bandwidth.
		// then we also need to take into account the number of tunnels that are shared through that peer... what a mess. maybe we should push that info when creating the FileSeeder/Leecher?
	}

	private void setFileSecurity(Path path)
	{
		if (path != null)
		{
			OsUtils.setFileSecurity(path, trusted);
		}
	}

	private void processLeecher(FileLeecher fileLeecher)
	{
		var sliceSender = fileLeecher.getSliceSender();
		var remaining = sliceSender.send();
		lastActivity = System.nanoTime();
		if (!remaining)
		{
			// We just remove the leecher here and nothing else. The fileTransferManager will close the file
			// when it's idle for some time, otherwise it would need to be reopened immediately for the
			// next slice.
			fileLeecher.removeSliceSender(sliceSender);
			if (fileLeecher.hasNoMoreSlices())
			{
				removePeer(fileLeecher.getLocation());
				return;
			}
		}
		// Here we could calculate the best time to send the next slice (8 KB) without overflowing our bandwidth
		addNextScheduling(fileLeecher, Duration.ofMillis(50)); // XXX: see above. this is 160 KB/s...
	}

	private void addNextScheduling(FilePeer filePeer, Duration duration)
	{
		filePeer.addNextScheduling(duration);
		queue.offer(filePeer);
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
			catch (FileAlreadyExistsException _)
			{
				log.warn("File name {} already exists, renaming...", fileName);
				fileName = FileNameUtils.rename(fileName);
			}
			catch (InvalidPathException _)
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
				success = true; // This is really a failure, but there's nothing else we can do
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