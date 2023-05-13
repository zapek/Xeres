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

import io.xeres.app.xrs.serialization.RsSerialized;

public class TurtleGenericSearchRequestItem extends TurtleFileSearchRequestItem implements Cloneable
{
	@RsSerialized
	private short serviceId;

	@RsSerialized
	private byte requestType;

	@RsSerialized
	private byte[] searchData; // XXX: not sure that's correct...

	@SuppressWarnings("unused")
	public TurtleGenericSearchRequestItem()
	{
	}

	@Override
	public int getSubType()
	{
		return 11;
	}

	public short getServiceId()
	{
		return serviceId;
	}

	public byte getRequestType()
	{
		return requestType;
	}

	public byte[] getSearchData()
	{
		return searchData;
	}

	@Override
	public String toString()
	{
		return "TurtleGenericSearchRequestItem{" +
				"requestId=" + getRequestId() +
				", depth=" + getDepth() +
				", serviceId=" + serviceId +
				", requestType=" + requestType +
				'}';
	}

	@Override
	public TurtleGenericSearchRequestItem clone()
	{
		return (TurtleGenericSearchRequestItem) super.clone();
	}
}
