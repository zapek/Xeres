/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

import java.util.Arrays;

public class Image
{
	public enum Type
	{
		UNKNOWN(0),
		PNG(1),
		JPEG(2);

		private final int value;

		Type(int value)
		{
			this.value = value;
		}

		public int getValue()
		{
			return value;
		}

		public static Type fromValue(int value)
		{
			return Arrays.stream(Type.values())
					.filter(type -> type.getValue() == value)
					.findFirst().orElse(Type.UNKNOWN);
		}
	}

	private final Type type;
	private final byte[] data;

	public Image(Type type, byte[] data)
	{
		this.type = type;
		this.data = data;
	}

	public Type getType()
	{
		return type;
	}

	public byte[] getData()
	{
		return data;
	}
}
