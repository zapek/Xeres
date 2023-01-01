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

import java.util.Objects;

final class FloatSerializer
{
	private static final Logger log = LoggerFactory.getLogger(FloatSerializer.class);

	private FloatSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	@SuppressWarnings("SameReturnValue")
	static int serialize(ByteBuf buf, Float f)
	{
		Objects.requireNonNull(f, "Null float not supported");
		log.trace("Writing float: {}", f);
		buf.ensureWritable(4);
		buf.writeFloat(f);
		return 4;
	}

	static float deserialize(ByteBuf buf)
	{
		var val = buf.readFloat();
		log.trace("Reading float: {}", val);
		return val;
	}
}
