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

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.CHUNK_SIZE;
import static java.nio.file.StandardOpenOption.*;

/**
 * This implementation of {@link FileProvider} is for downloading a file.
 */
class FileLeecher extends FileSeeder
{
	private static final Logger log = LoggerFactory.getLogger(FileLeecher.class);
	private RandomAccessFile randomAccessFile;

	private final BitSet chunkMap;
	private final int nBits;
	private final ChunkDistributor chunkDistributor;
	private final Map<Integer, Chunk> chunks = new HashMap<>();
	private long bytesWritten;

	public FileLeecher(File file, long size, BitSet chunkMap, FileTransferStrategy fileTransferStrategy)
	{
		super(file);
		fileSize = size;
		nBits = (int) (size / CHUNK_SIZE + (size % CHUNK_SIZE != 0 ? 1 : 0));
		this.chunkMap = chunkMap != null ? chunkMap : new BitSet(nBits);
		chunkDistributor = new ChunkDistributor(this.chunkMap, nBits, fileTransferStrategy);
	}

	@Override
	public boolean open()
	{
		try
		{
			createSparseFile();
			randomAccessFile = new RandomAccessFile(file, "rw");
			ensureSparseFile();
			channel = randomAccessFile.getChannel();
			lock = channel.lock(); // Exclusive lock
			return true;
		}
		catch (IOException e)
		{
			log.error("Couldn't open file {} for writing", file, e);
			return false;
		}
	}

	/**
	 * This creates a sparse file on Windows.
	 * <p>
	 * The file must not exist and is then marked as such.
	 * (Write once, run anywhere, my ass...).
	 *
	 * @throws IOException if some I/O error happens
	 */
	private void createSparseFile() throws IOException
	{
		if (SystemUtils.IS_OS_WINDOWS && !file.exists())
		{
			try (var seekableByteChannel = Files.newByteChannel(file.toPath(), CREATE_NEW, WRITE, SPARSE))
			{
				seekableByteChannel.position(fileSize - 1);
				seekableByteChannel.write(ByteBuffer.wrap(new byte[]{(byte) 0}));
			}
		}
	}

	/**
	 * This ensures the file is sparse. Basically on Linux and MacOS, we just have to
	 * set the length, and it's sparse by default.
	 *
	 * @throws IOException if some I/O error happens
	 */
	private void ensureSparseFile() throws IOException
	{
		if (!SystemUtils.IS_OS_WINDOWS)
		{
			randomAccessFile.setLength(fileSize);
		}
	}

	@Override
	public byte[] read(long offset, int size) throws IOException
	{
		if (isChunkAvailable(offset, size))
		{
			return super.read(offset, size);
		}
		throw new IOException("File at offset " + offset + " with size " + size + " is not available yet.");
	}

	@Override
	public void write(long offset, byte[] data) throws IOException
	{
		var buf = ByteBuffer.wrap(data);
		var size = channel.write(buf, offset);
		bytesWritten += size;
		if (size != data.length)
		{
			throw new IOException("Failed to write data, requested size: " + data.length + ", actually written: " + size);
		}
		markBlockAsWritten(offset);
	}

	@Override
	public void close()
	{
		try
		{
			lock.close();
			channel.close();
			randomAccessFile.close();
		}
		catch (IOException e)
		{
			log.error("Failed to close file {} properly", file, e);
		}
	}

	@Override
	public BitSet getChunkMap()
	{
		return (BitSet) chunkMap.clone();
	}

	@Override
	public boolean isComplete()
	{
		return chunkMap.cardinality() == nBits;
	}

	@Override
	public Optional<Integer> getNeededChunk()
	{
		return chunkDistributor.getNextChunk();
	}

	@Override
	public boolean hasChunk(int index)
	{
		return chunkMap.get(index);
	}

	private boolean isChunkAvailable(long offset, int chunkSize)
	{
		int chunkStart = (int) (offset / chunkSize);
		int chunkEnd = (int) ((offset + chunkSize) / chunkSize);

		if ((offset + chunkSize) % chunkSize != 0)
		{
			chunkEnd++;
		}

		for (var i = chunkStart; i < chunkEnd; i++)
		{
			if (!chunkMap.get(i))
			{
				return false;
			}
		}
		return true;
	}

	private void markBlockAsWritten(long offset)
	{
		int chunkKey = (int) (offset / CHUNK_SIZE);
		var chunk = chunks.computeIfAbsent(chunkKey, k -> new Chunk(Math.min(CHUNK_SIZE, fileSize - offset)));
		chunk.setBlockAsWritten(offset);

		if (chunk.isComplete())
		{
			log.debug("Chunk {} is complete", chunkKey);
			chunkMap.set(chunkKey);
			chunks.remove(chunkKey);
		}
	}

	@Override
	public long getBytesWritten()
	{
		return bytesWritten;
	}
}
