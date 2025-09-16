/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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
public class Sha1Sum implements Identifier, Cloneable, Comparable<Sha1Sum>
{
	public static final int LENGTH = 20;

	private byte[] identifier;

	public Sha1Sum()
	{
		// Needed for JPA
	}

	public Sha1Sum(byte[] sum)
	{
		Objects.requireNonNull(sum, "Null sha1 sum");
		if (sum.length != LENGTH)
		{
			throw new IllegalArgumentException("Bad sha1 sum length, expected " + LENGTH + ", got " + sum.length);
		}
		identifier = sum;
	}

	/**
	 * Creates a {@link Sha1Sum} from a string.
	 *
	 * @param from a string representing the Sha1Sum in hexadecimal form (lowercase, no prefix)
	 * @return the Sha1Sum or an empty Sha1Sum if the string was invalid
	 */
	public static Sha1Sum fromString(String from)
	{
		return new Sha1Sum(Identifier.parseString(from, LENGTH));
	}

	@Override
	public byte[] getBytes()
	{
		return identifier;
	}

	// This is used for serialization (for example passing a GxsId in a STOMP message)
	public void setBytes(byte[] identifier)
	{
		this.identifier = identifier;
	}

	@JsonIgnore
	@Override
	public int getLength()
	{
		return LENGTH;
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
		var that = (Sha1Sum) o;
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
		return Id.toString(identifier);
	}

	@Override
	public Sha1Sum clone()
	{
		try
		{
			var clone = (Sha1Sum) super.clone();
			clone.identifier = identifier.clone();
			return clone;
		}
		catch (CloneNotSupportedException _)
		{
			throw new AssertionError();
		}
	}

	@Override
	public int compareTo(Sha1Sum o)
	{
		return Arrays.compare(identifier, o.identifier);
	}
}
