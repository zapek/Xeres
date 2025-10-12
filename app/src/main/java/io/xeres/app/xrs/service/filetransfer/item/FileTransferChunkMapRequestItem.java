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

public class FileTransferChunkMapRequestItem extends Item
{
	@RsSerialized
	private boolean isLeecher;

	@RsSerialized
	private Sha1Sum hash;

	@SuppressWarnings("unused")
	public FileTransferChunkMapRequestItem()
	{
	}

	public FileTransferChunkMapRequestItem(Sha1Sum hash, boolean isLeecher)
	{
		this.hash = hash;
		this.isLeecher = isLeecher;
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.FILE_TRANSFER.getType();
	}

	@Override
	public int getSubType()
	{
		return 4;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.HIGH.getPriority();
	}

	public boolean isLeecher()
	{
		return isLeecher;
	}

	public Sha1Sum getHash()
	{
		return hash;
	}

	@Override
	public FileTransferChunkMapRequestItem clone()
	{
		return (FileTransferChunkMapRequestItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "FileTransferChunkMapRequestItem{" +
				"isLeecher=" + isLeecher +
				", hash=" + hash +
				'}';
	}
}
