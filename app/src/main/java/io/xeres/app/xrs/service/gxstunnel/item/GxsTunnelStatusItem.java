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

package io.xeres.app.xrs.service.gxstunnel.item;

import io.xeres.app.xrs.serialization.RsSerialized;

import java.util.EnumSet;
import java.util.Set;

public class GxsTunnelStatusItem extends GxsTunnelItem
{
	public enum Status
	{
		UNUSED_1,
		UNUSED_2,
		UNUSED_3,
		UNUSED_4,
		UNUSED_5,
		UNUSED_6,
		UNUSED_7,
		UNUSED_8,
		UNUSED_9,
		UNUSED_10,
		CLOSING_DISTANT_CONNECTION,
		ACK_DISTANT_CONNECTION,
		KEEP_ALIVE
	}

	@RsSerialized
	private Set<Status> status;

	@SuppressWarnings("unused")
	public GxsTunnelStatusItem()
	{
	}

	public GxsTunnelStatusItem(Status status)
	{
		this.status = EnumSet.of(status);
	}

	@Override
	public int getSubType()
	{
		return 3;
	}

	public Status getStatus()
	{
		// XXX: we should add some warning when a status we don't know is wedged in there
		return status.stream().findFirst().orElse(Status.UNUSED_1);
	}
}
