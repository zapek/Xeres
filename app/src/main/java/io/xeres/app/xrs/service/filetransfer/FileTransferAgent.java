/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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

/// Responsible for sending/receiving one file.
/// There can be several leechers or seeders per file.
class FileTransferAgent
{
	private static final Logger log = LoggerFactory.getLogger(FileTransferAgent.class);

	/// Time after which a download or upload is considered stale.
	private static final long IDLE_TIME = Duration.ofMinutes(5).toNanos();

	/// The minimum rate increase, in bytes per second, under which we consider
	/// the transfer to have plateaued and leave slow start.
	private static final long SLOW_START_MIN_INCREASE = 10_240;

	/// The number of seconds of measured rate we are willing to have in flight,
	/// used to cap the request size so we don't over-request slow peers.
	private static final double QUEUE_TIME_SECONDS = 0.5;

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

	/// Adds a seeder, that is, someone we can get the file from.
	///
	/// @param peer the location
	public void addSeeder(Location peer)
	{
		seeders.computeIfAbsent(peer, _ -> {
			var fileSeeder = new FileSeeder(peer);
			fileTransferRsService.enrichPeer(fileSeeder, peer);
			queue.add(fileSeeder);
			return fileSeeder;
		});
		fileTransferRsService.sendChunkMapRequest(peer, hash, false);
	}

	/// Adds a leecher, that is, someone who wants to download our file.
	/// @param peer the location
	/// @param offset the requested offset of the file
	/// @param size the requested size of the chunk
	/// @return the leecher
	public FileLeecher addLeecher(Location peer, long offset, int size)
	{
		var fileLeecher = leechers.computeIfAbsent(peer, _ -> {
			var newLeecher = new FileLeecher(peer);
			fileTransferRsService.enrichPeer(newLeecher, peer);
			queue.add(newLeecher);
			return newLeecher;
		});
		fileLeecher.addSliceSender(new SliceSender(fileTransferRsService, peer, fileProvider, hash, fileProvider.getFileSize(), offset, size, fileLeecher.getSendRate()));
		return fileLeecher;
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

	/// Processes file transfers.
	public void process()
	{
		processPeers();
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

	/// Records that data has been received from a peer.
	///
	/// @param peer  the peer the data was received from
	/// @param bytes the number of bytes received
	public void recordReceive(Location peer, long bytes)
	{
		var seeder = seeders.get(peer);
		if (seeder != null)
		{
			seeder.getReceiveRate().addBytes(bytes);
			seeder.addReceivedChunkBytes(bytes);
		}
	}

	/// Tells if an agent idle. That is, nothing has been sent or received
	/// for more than 5 minutes.
	///
	/// @return true if idle
	public boolean isIdle()
	{
		return System.nanoTime() - lastActivity > IDLE_TIME;
	}

	public boolean isDone() // XXX: isDone what? it's only when it's done loading, should be clearer
	{
		return done;
	}

	/// Returns the next desired processing.
	///
	/// @return when the next processing happens, null if there's no processing needed
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
		lastActivity = System.nanoTime();

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

		if (!fileSeeder.hasChunkMap())
		{
			addNextScheduling(fileSeeder, Duration.ofMillis(250));
			return;
		}

		// If a slice is in flight, wait for it to arrive before requesting more.
		if (fileSeeder.isSliceInFlight())
		{
			if (fileProvider.hasChunk(fileSeeder.getCurrentChunk()) || sliceReceived(fileSeeder, fileSeeder.getCurrentChunk()))
			{
				fileSeeder.setSliceInFlight(false);
			}
			else
			{
				addNextScheduling(fileSeeder, Duration.ofMillis(250));
				return;
			}
		}

		// If the current chunk is complete, advance to the next one.
		if (fileProvider.hasChunk(fileSeeder.getCurrentChunk()))
		{
			log.debug("Chunk {} is complete", fileSeeder.getCurrentChunk());
			if (!selectChunk(fileSeeder))
			{
				addNextScheduling(fileSeeder, Duration.ofMillis(250));
				return;
			}
		}
		else if (fileSeeder.getRequestSize() == 0)
		{
			// No chunk has been selected yet.
			if (!selectChunk(fileSeeder))
			{
				addNextScheduling(fileSeeder, Duration.ofMillis(250));
				return;
			}
		}

		// Request the next slice of the current chunk.
		requestNextSlice(fileSeeder);

		// Pace the next request by how long it takes to fill this slice at the
		// measured receive rate. Fast peers are polled more often, slow ones less.
		addNextScheduling(fileSeeder, BandwidthScheduler.delayFor(fileSeeder.getReceiveRate().getBytesPerSecond(), fileSeeder.getRequestSize(), Duration.ofMillis(250)));
	}

	/// Selects the next chunk to download from this peer and initializes the
	/// slice-request state for it.
	///
	/// @param fileSeeder the seeder
	/// @return true if a chunk was selected, false if there's nothing left to request from this peer
	private boolean selectChunk(FileSeeder fileSeeder)
	{
		var chunkNumber = getNextChunk(fileSeeder.getChunkMap());
		if (chunkNumber.isEmpty())
		{
			return false;
		}
		fileSeeder.setCurrentChunk(chunkNumber.get());
		fileSeeder.setNextSliceOffset((long) chunkNumber.get() * FileTransferRsService.CHUNK_SIZE);
		fileSeeder.setRequestSize(FileTransferRsService.getInitialRequestSize());
		fileSeeder.resetReceivedChunkBytes();
		fileSeeder.setSlowStart(true);
		fileSeeder.setSliceInFlight(false);
		fileSeeder.setPreviousRate(0);
		return true;
	}

	/// Requests the next slice of the current chunk from the peer, using the
	/// currently configured request size, then grows the request size for the
	/// following slice.
	///
	/// @param fileSeeder the seeder
	private void requestNextSlice(FileSeeder fileSeeder)
	{
		var chunkNumber = fileSeeder.getCurrentChunk();
		var fileSize = fileProvider.getFileSize();
		var chunkEnd = Math.min(fileSize, (long) (chunkNumber + 1) * FileTransferRsService.CHUNK_SIZE);
		var remaining = chunkEnd - fileSeeder.getNextSliceOffset();
		if (remaining <= 0)
		{
			return;
		}

		// The block accounting (markBlocksAsWritten) requires writes to start on
		// a block boundary, so the request size must be a multiple of the block
		// size, except for the final partial block of a chunk.
		int size;
		if (remaining <= FileTransferRsService.BLOCK_SIZE)
		{
			size = (int) remaining;
		}
		else
		{
			size = Math.min(fileSeeder.getRequestSize(), (int) remaining);
			if (size % FileTransferRsService.BLOCK_SIZE != 0)
			{
				size -= size % FileTransferRsService.BLOCK_SIZE;
			}
			size = Math.max(FileTransferRsService.BLOCK_SIZE, size);
		}
		log.debug("Requesting {} bytes at offset {} (chunk {}) from peer {}", size, fileSeeder.getNextSliceOffset(), chunkNumber, fileSeeder.getLocation());
		fileTransferRsService.sendDataRequest(fileSeeder.getLocation(), hash, fileSize, fileSeeder.getNextSliceOffset(), size);
		fileSeeder.setNextSliceOffset(fileSeeder.getNextSliceOffset() + size);
		fileSeeder.setSliceInFlight(true);
		growRequestSize(fileSeeder);
	}

	/// Tells if the previously requested slice has fully arrived.
	///
	/// @param fileSeeder  the seeder
	/// @param chunkNumber the current chunk
	/// @return true if the in-flight slice has been received
	private boolean sliceReceived(FileSeeder fileSeeder, int chunkNumber)
	{
		var chunkBase = (long) chunkNumber * FileTransferRsService.CHUNK_SIZE;
		var requested = fileSeeder.getNextSliceOffset() - chunkBase;
		return fileSeeder.getReceivedChunkBytes() >= requested;
	}

	/// Grows the request size for the next slice, following a TCP-like
	/// slow start (double) then congestion avoidance (additive), backing off
	/// (halve) when the measured rate drops. The size is also capped by the
	/// measured rate so slow peers aren't over-requested.
	///
	/// @param fileSeeder the seeder
	private void growRequestSize(FileSeeder fileSeeder)
	{
		var current = fileSeeder.getRequestSize();
		var rate = fileSeeder.getReceiveRate().getBytesPerSecond();
		var previousRate = fileSeeder.getPreviousRate();

		var slowedDown = previousRate > 0 && rate > 0 && rate < previousRate;
		var growing = rate > previousRate + SLOW_START_MIN_INCREASE;

		int next;
		if (fileSeeder.isSlowStart())
		{
			if (previousRate == 0)
			{
				// No rate history yet: keep growing by slow start.
				next = Math.min(current * 2, FileTransferRsService.CHUNK_SIZE);
			}
			else if (slowedDown)
			{
				// Back off and resume slow start.
				next = Math.max(FileTransferRsService.getInitialRequestSize(), current / 2);
			}
			else if (growing)
			{
				// Slow start: double the request size.
				next = Math.min(current * 2, FileTransferRsService.CHUNK_SIZE);
			}
			else
			{
				// Rate plateaued: leave slow start, grow additively.
				fileSeeder.setSlowStart(false);
				next = Math.min(current + FileTransferRsService.BLOCK_SIZE, FileTransferRsService.CHUNK_SIZE);
			}
		}
		else
		{
			// Congestion avoidance: grow additively.
			next = Math.min(current + FileTransferRsService.BLOCK_SIZE, FileTransferRsService.CHUNK_SIZE);
		}

		// Cap by the measured rate so we don't over-request slow peers. The cap
		// is rounded down to a block multiple (never below one block) so that
		// the request sizes keep writes block-aligned.
		if (rate > 0)
		{
			var rateCap = (int) Math.min(FileTransferRsService.CHUNK_SIZE, rate * QUEUE_TIME_SECONDS);
			var alignedCap = Math.max(FileTransferRsService.BLOCK_SIZE, rateCap - rateCap % FileTransferRsService.BLOCK_SIZE);
			next = Math.min(next, alignedCap);
		}

		fileSeeder.setPreviousRate(rate);
		fileSeeder.setRequestSize(next);
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
		// Pace the next 8 KB block by the measured send rate, capped by the fair
		// share of our upload bandwidth so that all leechers together don't
		// overflow our link. When the own bandwidth is unknown, only the measured
		// send rate paces the block.
		var effectiveRate = sliceSender.getSendRate();
		var ownUploadRate = fileTransferRsService.getOwnUploadBandwidthBytesPerSecond();
		if (ownUploadRate > 0)
		{
			var fairShareRate = Math.max(1L, ownUploadRate / Math.max(1, leechers.size()));
			effectiveRate = effectiveRate == 0 ? fairShareRate : Math.min(effectiveRate, fairShareRate);
		}
		addNextScheduling(fileLeecher, BandwidthScheduler.delayFor(effectiveRate, FileTransferRsService.BLOCK_SIZE, Duration.ofMillis(50)));
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

	/// Gets the next available chunk.
	///
	/// @return the chunk number
	private Optional<Integer> getNextChunk(BitSet chunkMap)
	{
		return fileProvider.getNeededChunk(chunkMap);
	}
}