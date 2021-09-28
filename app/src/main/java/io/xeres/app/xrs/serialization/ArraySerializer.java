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

package io.xeres.app.xrs.serialization;

import io.netty.buffer.ByteBuf;

final class ArraySerializer
{
	private ArraySerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, Class<?> javaClass, Object object)
	{
		if (javaClass.equals(byte[].class))
		{
			return ByteArraySerializer.serialize(buf, (byte[]) object);
		}
		else
		{
			throw new IllegalArgumentException("Unhandled array type " + javaClass.getSimpleName()); // XXX: handle other types (see what RS uses...)
		}
	}

	static Object deserialize(ByteBuf buf, Class<?> javaClass)
	{
		if (javaClass.equals(byte[].class))
		{
			return ByteArraySerializer.deserialize(buf);
		}
		else
		{
			throw new IllegalArgumentException("Unhandled array type " + javaClass.getSimpleName()); // XXX
		}
	}
}
