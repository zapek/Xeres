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

package io.xeres.app.xrs.serialization;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

final class EnumSetSerializer
{
	private static final Logger log = LoggerFactory.getLogger(EnumSetSerializer.class);

	private EnumSetSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, Set<? extends Enum<?>> enumSet, RsSerialized annotation)
	{
		Objects.requireNonNull(annotation, "Annotation is needed for EnumSet");
		var fieldSize = annotation.fieldSize();

		return serialize(buf, enumSet, fieldSize);
	}

	static int serialize(ByteBuf buf, Set<? extends Enum<?>> enumSet, FieldSize fieldSize)
	{
		Objects.requireNonNull(enumSet, "Null enumset not supported");
		return switch (fieldSize)
				{
					case INTEGER -> serializeEnumSetInt(buf, enumSet);
					case BYTE -> serializeEnumSetByte(buf, enumSet);
					case SHORT -> serializeEnumSetShort(buf, enumSet);
				};
	}

	private static int serializeEnumSetInt(ByteBuf buf, Set<? extends Enum<?>> enumSet)
	{
		if (enumSet.size() > 32)
		{
			throw new IllegalArgumentException("EnumSet cannot have more than 32 entries");
		}
		var size = 4;

		log.trace("Enumset (int): {}", enumSet);
		buf.ensureWritable(size);
		var value = 0;
		for (Enum<?> anEnum : enumSet)
		{
			value |= 1 << anEnum.ordinal();
		}
		buf.writeInt(value);
		return size;
	}

	private static int serializeEnumSetByte(ByteBuf buf, Set<? extends Enum<?>> enumSet)
	{
		if (enumSet.size() > 8)
		{
			throw new IllegalArgumentException("EnumSet for a byte cannot have more than 8 entries");
		}
		var size = 1;

		log.trace("Enumset (byte): {}", enumSet);
		buf.ensureWritable(size);
		byte value = 0;
		for (Enum<?> anEnum : enumSet)
		{
			value |= 1 << anEnum.ordinal();
		}
		buf.writeByte(value);
		return size;
	}

	private static int serializeEnumSetShort(ByteBuf buf, Set<? extends Enum<?>> enumSet)
	{
		if (enumSet.size() > 16)
		{
			throw new IllegalArgumentException("EnumSet for a short cannot have more than 16 entries");
		}
		var size = 2;

		log.trace("Enumset (short): {}", enumSet);
		buf.ensureWritable(size);
		short value = 0;
		for (Enum<?> anEnum : enumSet)
		{
			value |= 1 << anEnum.ordinal();
		}
		buf.writeShort(value);
		return size;
	}

	static <E extends Enum<E>> Set<E> deserialize(ByteBuf buf, ParameterizedType type, RsSerialized annotation)
	{
		Objects.requireNonNull(annotation, "Annotation is needed for EnumSet");
		@SuppressWarnings("unchecked")
		var enumClass = (Class<E>) type.getActualTypeArguments()[0];

		var fieldSize = annotation.fieldSize();

		return deserialize(buf, enumClass, fieldSize);
	}

	static <E extends Enum<E>> Set<E> deserialize(ByteBuf buf, Class<E> e, FieldSize fieldSize)
	{
		return switch (fieldSize)
				{
					case INTEGER -> deserializeEnumSetInt(buf, e);
					case BYTE -> deserializeEnumSetByte(buf, e);
					case SHORT -> deserializeEnumSetShort(buf, e);
				};
	}

	private static <E extends Enum<E>> Set<E> deserializeEnumSetInt(ByteBuf buf, Class<E> e)
	{
		var value = buf.readInt();
		log.trace("Reading enumSet (int): {}", value);
		var enumSet = EnumSet.noneOf(e);
		for (var enumConstant : e.getEnumConstants())
		{
			if ((value & (1 << enumConstant.ordinal())) != 0)
			{
				enumSet.add(enumConstant);
			}
		}
		return enumSet;
	}

	private static <E extends Enum<E>> Set<E> deserializeEnumSetByte(ByteBuf buf, Class<E> e)
	{
		var value = buf.readByte();
		log.trace("Reading enumSet (byte): {}", value);
		var enumSet = EnumSet.noneOf(e);
		for (var enumConstant : e.getEnumConstants())
		{
			if ((value & 0xff & (1 << enumConstant.ordinal())) != 0)
			{
				enumSet.add(enumConstant);
			}
		}
		return enumSet;
	}

	private static <E extends Enum<E>> Set<E> deserializeEnumSetShort(ByteBuf buf, Class<E> e)
	{
		var value = buf.readShort();
		log.trace("Reading enumSet (long): {}", value);
		var enumSet = EnumSet.noneOf(e);
		for (var enumConstant : e.getEnumConstants())
		{
			if ((value & (1 << enumConstant.ordinal())) != 0)
			{
				enumSet.add(enumConstant);
			}
		}
		return enumSet;
	}
}
