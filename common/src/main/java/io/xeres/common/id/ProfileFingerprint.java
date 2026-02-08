/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.common.id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Embeddable;

import java.util.Arrays;
import java.util.Objects;

@Embeddable
public class ProfileFingerprint implements Identifier
{
	public static final int V4_LENGTH = 20;
	public static final int LENGTH = 32;

	private byte[] identifier;

	public ProfileFingerprint()
	{

	}

	public ProfileFingerprint(byte[] identifier)
	{
		Objects.requireNonNull(identifier, "Null identifier");
		if (identifier.length != V4_LENGTH && identifier.length != LENGTH)
		{
			throw new IllegalArgumentException("Bad identifier length, expected " + V4_LENGTH + " or " + LENGTH + ", got " + identifier.length);
		}
		this.identifier = identifier;
	}

	@JsonIgnore
	@Override
	public byte[] getBytes()
	{
		return identifier;
	}

	// This is used for serialization (for example passing a ProfileFingerprint in a STOMP message)
	public void setBytes(byte[] identifier)
	{
		this.identifier = identifier;
	}

	@Override
	public int getLength()
	{
		if (identifier == null)
		{
			throw new IllegalStateException("getLength() called on ProfileFingerprint, which doesn't support null identifiers");
		}
		return identifier.length;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		var that = (ProfileFingerprint) o;
		return Arrays.equals(identifier, that.identifier);
	}

	@Override
	public int hashCode()
	{
		return Arrays.hashCode(identifier);
	}

	@Override
	public String toString()
	{
		var s = Id.toString(identifier);
		var out = new StringBuilder();
		var length = identifier.length * 2;

		for (var i = 0; i < length; i += 4)
		{
			if (i > 0)
			{
				out.append(" ");
			}
			out.append(s, i, i + 4);
		}
		return out.toString();
	}
}
