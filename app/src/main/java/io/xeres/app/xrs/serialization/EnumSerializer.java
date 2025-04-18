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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

final class EnumSerializer
{
	private static final Logger log = LoggerFactory.getLogger(EnumSerializer.class);

	private EnumSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	@SuppressWarnings("SameReturnValue")
	static int serialize(ByteBuf buf, Enum<? extends Enum<?>> e)
	{
		Objects.requireNonNull(e, "Null enum not supported");
		log.trace("Writing enum ordinal value: {}", e.ordinal());
		buf.ensureWritable(Integer.BYTES);
		buf.writeInt(e.ordinal());
		return Integer.BYTES;
	}

	static int getSize()
	{
		return Integer.BYTES;
	}

	@SuppressWarnings("unchecked")
	static <E extends Enum<E>> E deserialize(ByteBuf buf, Class<?> e)
	{
		var val = buf.readInt();
		log.trace("Reading enum ordinal value: {}, class: {}", val, e.getSimpleName());
		return (E) e.getEnumConstants()[val];
	}
}
