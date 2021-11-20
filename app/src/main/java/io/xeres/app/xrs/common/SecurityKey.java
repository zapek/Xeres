/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.common;

import io.xeres.common.id.GxsId;

import java.util.EnumSet;
import java.util.Set;

public class SecurityKey
{
	public enum Flags
	{
		TYPE_PUBLIC_ONLY,
		TYPE_FULL,
		UNUSED_3,
		UNUSED_4,
		DISTRIBUTION_PUBLISHING,
		DISTRIBUTION_ADMIN,
		DISTRIBUTION_IDENTITY;

		public static Set<Flags> ofTypes()
		{
			return EnumSet.of(TYPE_PUBLIC_ONLY, TYPE_FULL);
		}

		public static Set<Flags> ofDistributions()
		{
			return EnumSet.of(DISTRIBUTION_PUBLISHING, DISTRIBUTION_ADMIN, DISTRIBUTION_IDENTITY);
		}
	}

	private final GxsId gxsId;
	private final Set<Flags> flags;
	private final int startTs;
	private final int endTs;
	private final byte[] data;

	public SecurityKey(GxsId gxsId, Set<Flags> flags, int startTs, int endTs, byte[] data)
	{
		this.gxsId = gxsId;
		this.flags = flags;
		this.startTs = startTs;
		this.endTs = endTs;
		this.data = data;
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	public Set<Flags> getFlags()
	{
		return flags;
	}

	public int getStartTs()
	{
		return startTs;
	}

	public int getEndTs()
	{
		return endTs;
	}

	public byte[] getData()
	{
		return data;
	}
}
