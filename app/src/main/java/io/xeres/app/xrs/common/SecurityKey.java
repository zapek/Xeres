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

import io.xeres.app.database.converter.SecurityKeyFlagsConverter;
import io.xeres.common.id.GxsId;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigInteger;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@Embeddable
public final class SecurityKey implements Comparable<SecurityKey>
{
	@Embedded
	@NotNull
	@AttributeOverride(name = "identifier", column = @Column(name = "key_id"))
	private GxsId keyId;

	@Convert(converter = SecurityKeyFlagsConverter.class)
	private Set<Flags> flags = EnumSet.noneOf(SecurityKey.Flags.class);

	@NotNull
	private Instant validFrom;

	private Instant validTo; // if null, there's no expiration

	private byte[] data;

	public SecurityKey()
	{
	}

	public SecurityKey(@NotNull GxsId keyId, Set<Flags> flags, @NotNull Instant validFrom, Instant validTo, byte[] data)
	{
		this.keyId = keyId;
		this.flags = flags;
		this.validFrom = validFrom;
		this.validTo = validTo;
		this.data = data;
	}

	public SecurityKey(@NotNull GxsId keyId, Set<Flags> flags, int validFrom, int validTo, byte[] data)
	{
		this.keyId = keyId;
		this.flags = flags;
		this.validFrom = Instant.ofEpochSecond(validFrom);
		this.validTo = validTo == 0 ? null : Instant.ofEpochSecond(validTo);
		this.data = data;
	}

	public @NotNull GxsId getKeyId()
	{
		return keyId;
	}

	public void setKeyId(@NotNull GxsId keyId)
	{
		this.keyId = keyId;
	}

	public Set<Flags> getFlags()
	{
		return flags;
	}

	public void setFlags(Set<Flags> flags)
	{
		this.flags = flags;
	}

	public @NotNull Instant getValidFrom()
	{
		return validFrom;
	}

	public void setValidFrom(@NotNull Instant validFrom)
	{
		this.validFrom = validFrom;
	}

	public int getValidFromInTs()
	{
		return (int) validFrom.getEpochSecond();
	}

	public void setValidFrom(int validFrom)
	{
		this.validFrom = Instant.ofEpochSecond(validFrom);
	}

	public Instant getValidTo()
	{
		return validTo;
	}

	public void setValidTo(Instant validTo)
	{
		this.validTo = validTo;
	}

	public int getValidToInTs()
	{
		if (validTo == null)
		{
			return 0; // no expiration
		}
		return (int) validTo.getEpochSecond();
	}

	public void setValidTo(int validTo)
	{
		if (validTo == 0)
		{
			this.validTo = null;
		}
		else
		{
			this.validTo = Instant.ofEpochSecond(validTo);
		}
	}

	public byte[] getData()
	{
		return data;
	}

	public void setData(byte[] data)
	{
		this.data = data;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
		{
			return true;
		}
		if (obj == null || obj.getClass() != getClass())
		{
			return false;
		}
		var that = (SecurityKey) obj;
		return Objects.equals(keyId, that.keyId);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(keyId);
	}

	@Override
	public String toString()
	{
		return "SecurityKey[" +
				"gxsId=" + keyId + ", " +
				"flags=" + flags + ", " +
				"validFrom=" + validFrom + ", " +
				"validTo=" + validTo;
	}

	@Override
	public int compareTo(SecurityKey other)
	{
		return new BigInteger(1, keyId.getBytes()).compareTo(new BigInteger(1, other.getKeyId().getBytes()));
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
