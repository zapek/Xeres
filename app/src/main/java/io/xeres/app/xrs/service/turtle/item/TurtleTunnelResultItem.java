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

/**
 * Used for acknowledging that a tunnel has been opened.
 */
public class TurtleTunnelResultItem extends Item
{
	/**
	 * The id of the tunnel. Should be identical for a tunnel between two same peers for the same hash.
	 */
	@RsSerialized
	private int tunnelId;

	/**
	 * Randomly generated request id corresponding to the initial request.
	 */
	@RsSerialized
	private int requestId;

	@SuppressWarnings("unused")
	public TurtleTunnelResultItem()
	{
	}

	public TurtleTunnelResultItem(int tunnelId, int requestId)
	{
		this.tunnelId = tunnelId;
		this.requestId = requestId;
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.TURTLE.getType();
	}

	@Override
	public int getSubType()
	{
		return 4;
	}

	public int getTunnelId()
	{
		return tunnelId;
	}

	public int getRequestId()
	{
		return requestId;
	}

	@Override
	public String toString()
	{
		return "TurtleTunnelOkItem{" +
				"tunnelId=" + tunnelId +
				", requestId=" + requestId +
				'}';
	}
}
