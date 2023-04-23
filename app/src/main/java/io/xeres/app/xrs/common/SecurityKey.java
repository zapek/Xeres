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

package io.xeres.app.xrs.common;

import io.xeres.app.crypto.rsa.RSA;
import io.xeres.common.id.GxsId;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class SecurityKey
{
	private final GxsId gxsId;
	private final Set<Flags> flags;
	private final int startTs;
	private final int endTs;
	private final byte[] data;

	public SecurityKey(PublicKey key, Set<Flags> flags, int startTs, int endTs) throws IOException
	{
		gxsId = RSA.getGxsId(key);
		this.flags = flags;
		this.startTs = startTs;
		this.endTs = endTs;
		data = RSA.getPublicKeyAsPkcs1(key);
	}

	public SecurityKey(GxsId gxsId, Set<Flags> flags, int startTs, int endTs, byte[] data)
	{
		this.gxsId = gxsId;
		this.flags = flags;
		this.startTs = startTs;
		this.endTs = endTs;
		this.data = data;
	}

	public GxsId gxsId()
	{
		return gxsId;
	}

	public Set<Flags> flags()
	{
		return flags;
	}

	public int startTs()
	{
		return startTs;
	}

	public int endTs()
	{
		return endTs;
	}

	public byte[] data()
	{
		return data;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;
		if (obj == null || obj.getClass() != getClass()) return false;
		var that = (SecurityKey) obj;
		return Objects.equals(gxsId, that.gxsId) &&
				Objects.equals(flags, that.flags) &&
				startTs == that.startTs &&
				endTs == that.endTs &&
				Arrays.equals(data, that.data);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(gxsId, flags, startTs, endTs, Arrays.hashCode(data));
	}

	@Override
	public String toString()
	{
		return "SecurityKey[" +
				"gxsId=" + gxsId + ", " +
				"flags=" + flags + ", " +
				"startTs=" + startTs + ", " +
				"endTs=" + endTs + ", " +
				"data=" + Arrays.toString(data) + ']';
	}

	public enum Flags
	{
		TYPE_PUBLIC_ONLY, // 0x1
		TYPE_FULL, // 0x2
		UNUSED_3, // 0x4
		UNUSED_4, // 0x8
		UNUSED_5, // 0x10
		DISTRIBUTION_PUBLISHING, // 0x20
		DISTRIBUTION_ADMIN, // 0x40
		UNUSED_8; // 0x80

		public static Set<Flags> ofTypes()
		{
			return EnumSet.of(TYPE_PUBLIC_ONLY, TYPE_FULL);
		}

		public static Set<Flags> ofDistributions()
		{
			return EnumSet.of(DISTRIBUTION_PUBLISHING, DISTRIBUTION_ADMIN);
		}
	}
}
