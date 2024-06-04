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

import io.xeres.app.xrs.common.FileData;
import io.xeres.app.xrs.common.FileItem;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.Sha1Sum;

import static io.xeres.app.xrs.serialization.TlvType.FILE_DATA;

public class FileTransferDataItem extends Item
{
	@RsSerialized(tlvType = FILE_DATA)
	private FileData fileData;

	public FileTransferDataItem()
	{
		// Needed
	}

	public FileTransferDataItem(long offset, long size, Sha1Sum hash, byte[] data)
	{
		var fileItem = new FileItem(size, hash, "", "", 0, 0, 0, null);
		fileData = new FileData(fileItem, offset, data);
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.FILE_TRANSFER.getType();
	}

	@Override
	public int getSubType()
	{
		return 2;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.NORMAL.getPriority();
	}

	public FileData getFileData()
	{
		return fileData;
	}
}
