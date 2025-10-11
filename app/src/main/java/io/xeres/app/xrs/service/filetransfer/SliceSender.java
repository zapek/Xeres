/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

import static io.xeres.app.xrs.service.filetransfer.FileTransferRsService.BLOCK_SIZE;

/**
 * Responsible for sending a slice (1 MB or less) to a remote location.
 * It is sent by blocks of 8 KB (possibly less for the last one).
 */
class SliceSender
{
	private static final Logger log = LoggerFactory.getLogger(SliceSender.class);

	private final FileTransferRsService fileTransferRsService;
	private final Location location;
	private final FileProvider provider;
	private final Sha1Sum hash;
	private final long totalSize;
	private long offset;
	private int size;

	public SliceSender(FileTransferRsService fileTransferRsService, Location location, FileProvider provider, Sha1Sum hash, long totalSize, long offset, int size)
	{
		this.fileTransferRsService = fileTransferRsService;
		this.location = location;
		this.provider = provider;
		this.hash = hash;
		this.totalSize = totalSize;
		this.offset = offset;
		this.size = size;
	}

	/**
	 * Sends data.
	 *
	 * @return false in case of an error or when it's done sending. Basically keep calling it when it's true
	 */
	public boolean send()
	{
		var length = Math.min(BLOCK_SIZE, size);

		byte[] data;
		try
		{
			data = provider.read(offset, length);
		}
		catch (IOException e)
		{
			log.error("Failed to read file", e);
			return false;
		}
		if (data.length > 0)
		{
			fileTransferRsService.sendData(location, hash, totalSize, offset, data);
		}

		size -= data.length;
		offset += data.length;

		return size > 0 && data.length == length;
	}
}
