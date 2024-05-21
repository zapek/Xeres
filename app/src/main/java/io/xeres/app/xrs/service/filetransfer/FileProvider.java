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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;

class FileProvider
{
	private static final Logger log = LoggerFactory.getLogger(FileProvider.class);
	protected final File file;
	protected FileChannel channel;
	protected FileLock lock;
	protected long size;

	public FileProvider(File file)
	{
		this.file = file;
	}

	public void setFileSize(long size)
	{
		throw new IllegalArgumentException("Cannot set the file size of a read only file");
	}

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
			size = channel.size();
			return true;
		}
		catch (IOException e)
		{
			log.error("Couldn't open file {} for reading", file, e);
			return false;
		}
	}

	public byte[] read(Location requester, long offset, int chunkSize) throws IOException // XXX: RS has an option to return unchecked chunks. not sure when it's used
	{
		// XXX: update the status of the peer

		int bufferSize = (int) Math.min(chunkSize, size);
		var buf = ByteBuffer.allocate(bufferSize);
		channel.read(buf, offset);
		buf.flip();
		return buf.array();
	}

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
}
