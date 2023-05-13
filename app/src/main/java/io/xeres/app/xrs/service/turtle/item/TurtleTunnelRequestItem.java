/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.turtle.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.Sha1Sum;

public class TurtleTunnelRequestItem extends Item implements Cloneable
{
	@RsSerialized
	private Sha1Sum fileHash;

	@RsSerialized
	private int requestId;

	@RsSerialized
	private int partialTunnelId;

	@RsSerialized
	private short depth;

	public TurtleTunnelRequestItem()
	{
		// Required
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.TURTLE.getType();
	}

	@Override
	public int getSubType()
	{
		return 3;
	}

	public Sha1Sum getFileHash()
	{
		return fileHash;
	}

	public int getRequestId()
	{
		return requestId;
	}

	public int getPartialTunnelId()
	{
		return partialTunnelId;
	}

	public short getDepth()
	{
		return depth;
	}

	public void setDepth(short depth)
	{
		this.depth = depth;
	}

	@Override
	public String toString()
	{
		return "TurtleTunnelOpenItem{" +
				"fileHash=" + fileHash +
				", requestId=" + requestId +
				", partialTunnelId=" + partialTunnelId +
				", depth=" + depth +
				'}';
	}

	@Override
	public TurtleTunnelRequestItem clone()
	{
		try
		{
			var clone = (TurtleTunnelRequestItem) super.clone();
			clone.buf = null; // the cloning is done to write multiple buffers, we don't need to copy it
			return clone;
		}
		catch (CloneNotSupportedException e)
		{
			throw new AssertionError();
		}
	}
}
