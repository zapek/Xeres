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

package io.xeres.app.xrs.service.filetransfer.item;

import io.xeres.app.xrs.common.FileItem;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.Sha1Sum;

import static io.xeres.app.xrs.serialization.TlvType.FILE_ITEM;

public class FileTransferDataRequestItem extends Item
{
	@RsSerialized
	private long fileOffset;

	@RsSerialized
	private int chunkSize;

	@RsSerialized(tlvType = FILE_ITEM)
	private FileItem fileItem;

	public FileTransferDataRequestItem()
	{
		// Required
	}

	public FileTransferDataRequestItem(long fileSize, Sha1Sum hash, long fileOffset, int chunkSize)
	{
		fileItem = new FileItem(fileSize, hash, null, null, 0, 0, 0, null);

		this.fileOffset = fileOffset;
		this.chunkSize = chunkSize;
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.FILE_TRANSFER.getType();
	}

	@Override
	public int getSubType()
	{
		return 1;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.HIGH.getPriority();
	}

	public long getFileOffset()
	{
		return fileOffset;
	}

	public int getChunkSize()
	{
		return chunkSize;
	}

	public FileItem getFileItem()
	{
		return fileItem;
	}
}
