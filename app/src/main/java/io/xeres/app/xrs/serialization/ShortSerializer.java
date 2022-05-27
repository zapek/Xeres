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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

final class ShortSerializer
{
	private static final Logger log = LoggerFactory.getLogger(ShortSerializer.class);

	private ShortSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	@SuppressWarnings("SameReturnValue")
	static int serialize(ByteBuf buf, Short sh)
	{
		Objects.requireNonNull(sh, "Null short not supported");
		log.trace("Writing short: {}", sh);
		buf.ensureWritable(2);
		buf.writeShort(sh);
		return 2;
	}

	static short deserialize(ByteBuf buf)
	{
		var val = buf.readShort();
		log.trace("Reading short: {}", val);
		return val;
	}
}
