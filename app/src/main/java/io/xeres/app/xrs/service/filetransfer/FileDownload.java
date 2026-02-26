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

import io.xeres.common.id.Sha1Sum;
import io.xeres.common.util.OsUtils;
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
class FileDownload extends FileUpload
{
	private static final Logger log = LoggerFactory.getLogger(FileDownload.class);
	private RandomAccessFile randomAccessFile;

	private final long id;
	private final BitSet chunkMap;
	private final int nBits;
	private final ChunkDistributor chunkDistributor;
	private final Map<Integer, Chunk> chunks = new HashMap<>();
	private long bytesWritten;

	public FileDownload(long id, File file, long size, BitSet chunkMap, FileTransferStrategy fileTransferStrategy)
	{
		super(file);
		this.id = id;
		fileSize = size;
		nBits = (int) (size / CHUNK_SIZE + (size % CHUNK_SIZE != 0 ? 1 : 0));
		this.chunkMap = chunkMap != null ? chunkMap : new BitSet(nBits);
		bytesWritten = (long) this.chunkMap.cardinality() * CHUNK_SIZE;
		chunkDistributor = new ChunkDistributor(this.chunkMap, nBits, fileTransferStrategy);
	}

	@Override
	public boolean open()
	{
		try
		{
			createSparseFile();
			randomAccessFile = new RandomAccessFile(file, "rw");
			OsUtils.setFileVisible(file.toPath(), false);
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
		markBlocksAsWritten(offset, size);
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
	public void closeAndDelete()
	{
		close();
		try
		{
			Files.delete(file.toPath());
		}
		catch (IOException e)
		{
			log.error("Couldn't delete file {} properly: {}", file, e.getMessage());
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
	public Optional<Integer> getNeededChunk(BitSet chunkMap)
	{
		return chunkDistributor.getNextChunk(chunkMap);
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

	private void markBlocksAsWritten(long offset, int size)
	{
		int chunkKey = (int) (offset / CHUNK_SIZE);
		var chunk = chunks.computeIfAbsent(chunkKey, _ -> new Chunk(Math.min(CHUNK_SIZE, fileSize - offset)));
		chunk.setBlocksAsWritten(offset, size);

		if (chunk.isComplete())
		{
			chunkMap.set(chunkKey);
			chunks.remove(chunkKey);
		}
	}

	@Override
	public Sha1Sum computeHash(long offset)
	{
		throw new IllegalStateException("Cannot compute hashes of files being downloaded");
	}

	@Override
	public long getBytesWritten()
	{
		return bytesWritten;
	}

	@Override
	public long getId()
	{
		return id;
	}
}
