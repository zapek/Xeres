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

package io.xeres.common.id;

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

	@Override
	public byte[] getBytes()
	{
		return identifier;
	}

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
			return (Sha1Sum) super.clone();
		}
		catch (CloneNotSupportedException e)
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
