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

import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.common.id.Sha1Sum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;
import java.util.Optional;

import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.BLOCK_SIZE;
import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.CHUNK_SIZE;

/**
 * This implementation of {@link FileProvider} is for uploading a file.
 */
class FileSeeder implements FileProvider
{
	private static final Logger log = LoggerFactory.getLogger(FileSeeder.class);
	protected final File file;
	protected FileChannel channel;
	protected FileLock lock;
	protected long fileSize;
	private BitSet chunkMap;
	private ByteBuffer buf;

	public FileSeeder(File file)
	{
		this.file = file;
	}

	@Override
	public long getFileSize()
	{
		if (channel == null)
		{
			throw new IllegalStateException("FileSeeder has not been initialized yet so the file size is not known");
		}
		return fileSize;
	}

	@Override
	public boolean open()
	{
		try
		{
			channel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
			lock = channel.lock(0, Long.MAX_VALUE, true);
			if (!lock.isShared())
			{
				log.warn("Lock for file {} is not shared", file);
			}
			fileSize = channel.size();
			return true;
		}
		catch (IOException e)
		{
			log.error("Couldn't open file {} for reading", file, e);
			return false;
		}
	}

	@Override
	public byte[] read(long offset, int size) throws IOException // XXX: RS has an option to return unchecked chunks. not sure when it's used
	{
		if (size > BLOCK_SIZE)
		{
			throw new IllegalArgumentException("size must be smaller than " + BLOCK_SIZE + " bytes");
		}
		allocateBufferIfNeeded();

		buf.clear();
		buf.limit(size);

		channel.read(buf, offset);
		var a = new byte[buf.position()];
		buf.flip();
		buf.get(a);
		return a;
	}

	@Override
	public Sha1Sum computeHash(long offset)
	{
		var hashBuf = ByteBuffer.allocate(CHUNK_SIZE);

		try
		{
			channel.read(hashBuf, offset);
		}
		catch (IOException e)
		{
			log.error("Failed to compute hash: {}", e.getMessage());
			return null;
		}

		var a = new byte[hashBuf.position()];
		hashBuf.flip();
		hashBuf.get(a);

		var digest = new Sha1MessageDigest();
		digest.update(a);
		return digest.getSum();
	}

	private void allocateBufferIfNeeded()
	{
		if (buf == null)
		{
			buf = ByteBuffer.allocate(BLOCK_SIZE);
		}
	}

	@Override
	public void write(long offset, byte[] data) throws IOException
	{
		throw new IllegalArgumentException("Cannot write data to a file provider");
	}

	@Override
	public BitSet getChunkMap()
	{
		if (chunkMap == null)
		{
			var numberOfChunks = getNumberOfChunks();
			chunkMap = new BitSet(numberOfChunks);
			chunkMap.set(0, numberOfChunks);
		}
		return (BitSet) chunkMap.clone();
	}

	protected int getNumberOfChunks()
	{
		var numberOfChunks = fileSize / CHUNK_SIZE;
		if (fileSize % CHUNK_SIZE != 0)
		{
			numberOfChunks++;
		}
		if (numberOfChunks > Integer.MAX_VALUE) // RS has a higher value because of unsigned ints. 4 TB instead of 2 TB
		{
			log.error("Maximum chunk value exceeded. File size: {}. File won't be transferred properly", fileSize);
			return 0;
		}
		return (int) numberOfChunks;
	}

	@Override
	public void close()
	{
		try
		{
			lock.close();
			channel.close();
		}
		catch (IOException e)
		{
			log.error("Failed to close file {} properly", file, e);
		}
	}

	@Override
	public void closeAndDelete()
	{
		throw new IllegalStateException("Cannot delete a seeder");
	}

	@Override
	public boolean isComplete()
	{
		return true;
	}

	@Override
	public boolean hasChunk(int index)
	{
		return true;
	}

	@Override
	public Optional<Integer> getNeededChunk(BitSet chunkMap)
	{
		return Optional.empty();
	}

	@Override
	public Path getPath()
	{
		return file.toPath();
	}

	@Override
	public long getBytesWritten()
	{
		throw new IllegalStateException("FileSeeder doesn't write bytes");
	}

	@Override
	public long getId()
	{
		throw new IllegalStateException("FileSeeders don't have an id");
	}
}
