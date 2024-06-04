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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.List;

class FileCreator extends FileProvider
{
	private static final Logger log = LoggerFactory.getLogger(FileCreator.class);
	private RandomAccessFile randomAccessFile;

	private BitSet chunkMap;

	public FileCreator(File file)
	{
		super(file);
	}

	@Override
	public void setFileSize(long size)
	{
		fileSize = size;
		chunkMap = new BitSet((int) (size / 1024 / 1024)); // a chunk represents 1 MB
	}

	@Override
	public boolean open()
	{
		try
		{
			randomAccessFile = new RandomAccessFile(file, "rw");
			randomAccessFile.setLength(fileSize); // XXX: check if that creates a sparse file on windows (and on linux)
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

	@Override
	public byte[] read(Location requester, long offset, int size) throws IOException
	{
		if (isChunkAvailable(offset, size))
		{
			return super.read(requester, offset, size);
		}
		throw new IOException("File at offset " + offset + " with size " + size + " is not available yet.");
	}

	public void write(Location requester, long offset, byte[] data) throws IOException
	{
		// XXX: update the status of the peer
		var buf = ByteBuffer.wrap(data);
		var size = channel.write(buf, offset);
		if (size != data.length)
		{
			throw new IOException("Failed to write data, requested size: " + data.length + ", actually written: " + size);
		}
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

	public List<Integer> getCompressedChunkMap()
	{
		// XXX: implement
		return List.of();
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
}
