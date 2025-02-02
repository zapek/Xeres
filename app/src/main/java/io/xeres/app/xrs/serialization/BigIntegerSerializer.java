/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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
import io.xeres.app.util.BigIntegerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

final class BigIntegerSerializer
{
	private static final Logger log = LoggerFactory.getLogger(BigIntegerSerializer.class);

	private BigIntegerSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, BigInteger value)
	{
		log.trace("Writing big integer: {}", value);
		var data = BigIntegerUtils.getAsOneComplement(value);
		buf.ensureWritable(Integer.BYTES + data.length);
		buf.writeInt(data.length);
		buf.writeBytes(data);
		return Integer.BYTES + data.length;
	}

	static BigInteger deserialize(ByteBuf buf)
	{
		var len = buf.readInt();
		log.trace("Reading big integer of size: {}", len);
		var out = new byte[len];
		buf.readBytes(out);
		return new BigInteger(out); // Negative numbers aren't supported
	}
}
