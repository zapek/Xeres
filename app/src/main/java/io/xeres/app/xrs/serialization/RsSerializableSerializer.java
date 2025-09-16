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

package io.xeres.app.xrs.serialization;

import io.netty.buffer.ByteBuf;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.Set;

final class RsSerializableSerializer
{
	private RsSerializableSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, RsSerializable rsSerializable)
	{
		return rsSerializable.writeObject(buf, EnumSet.noneOf(SerializationFlags.class));
	}

	static int serialize(ByteBuf buf, RsSerializable rsSerializable, Set<SerializationFlags> flags)
	{
		return rsSerializable.writeObject(buf, flags);
	}

	static Object deserialize(ByteBuf buf, Class<?> javaClass)
	{
		try
		{
			var instanceObject = javaClass.getDeclaredConstructor().newInstance();
			((RsSerializable) instanceObject).readObject(buf);
			return instanceObject;
		}
		catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException _)
		{
			throw new IllegalStateException("Unhandled class " + javaClass.getSimpleName());
		}
	}

	static void deserialize(ByteBuf buf, RsSerializable rsSerializable)
	{
		rsSerializable.readObject(buf);
	}
}
