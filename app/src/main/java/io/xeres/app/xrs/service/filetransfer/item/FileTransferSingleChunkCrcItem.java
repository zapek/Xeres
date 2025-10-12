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

package io.xeres.app.xrs.service.filetransfer.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.Sha1Sum;

public class FileTransferSingleChunkCrcItem extends Item
{
	@RsSerialized
	private Sha1Sum hash;

	@RsSerialized
	private int chunkNumber;

	@RsSerialized
	private Sha1Sum checkSum;

	@SuppressWarnings("unused")
	public FileTransferSingleChunkCrcItem()
	{
	}

	public FileTransferSingleChunkCrcItem(Sha1Sum hash, int chunkNumber, Sha1Sum checkSum)
	{
		this.hash = hash;
		this.chunkNumber = chunkNumber;
		this.checkSum = checkSum;
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.FILE_TRANSFER.getType();
	}

	@Override
	public int getSubType()
	{
		return 9;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.HIGH.getPriority();
	}

	public Sha1Sum getHash()
	{
		return hash;
	}

	public int getChunkNumber()
	{
		return chunkNumber;
	}

	public Sha1Sum getCheckSum()
	{
		return checkSum;
	}

	@Override
	public FileTransferSingleChunkCrcItem clone()
	{
		return (FileTransferSingleChunkCrcItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "FileTransferSingleChunkCrcItem{" +
				"hash=" + hash +
				", chunkNumber=" + chunkNumber +
				", checkSum=" + checkSum +
				'}';
	}
}
