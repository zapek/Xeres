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

package io.xeres.app.crypto.rsid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class RSIdArmor
{
	private static final Logger log = LoggerFactory.getLogger(RSIdArmor.class);

	enum WrapMode
	{
		CONTINUOUS,
		SLICED
	}

	private RSIdArmor()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static void addPacket(int pTag, byte[] data, ByteArrayOutputStream out)
	{
		if (data != null)
		{
			// This is like PGP packets, see https://tools.ietf.org/html/rfc4880
			out.write(pTag);
			if (data.length < 192) // size is coded in one byte
			{
				// one byte
				out.write(data.length);
			}
			else if (data.length < 8384) // size is coded in 2 bytes
			{
				var octet2 = (data.length - 192) & 0xff;
				out.write(((data.length - 192 - octet2) >> 8) + 192);
				out.write(octet2);
			}
			else
			{
				// We don't support more as it makes little sense to have an oversized certificate
				throw new IllegalArgumentException("Packet data size too big: " + data.length);
			}
			out.writeBytes(data);
		}
		else
		{
			log.warn("Trying to write certificate tag {} with empty data. Skipping...", pTag);
		}
	}

	static void addCrcPacket(int pTag, ByteArrayOutputStream out)
	{
		var data = out.toByteArray();

		var crc = RSIdCrc.calculate24bitsCrc(data, data.length);

		// Perform byte swapping
		var le = new byte[3];
		le[0] = (byte) (crc & 0xff);
		le[1] = (byte) ((crc >> 8) & 0xff);
		le[2] = (byte) ((crc >> 16) & 0xff);

		addPacket(pTag, le, out);
	}

	static String wrapWithBase64(byte[] data, WrapMode wrapMode)
	{
		var base64 = Base64.getEncoder().encode(data);

		try (var out = new ByteArrayOutputStream())
		{
			for (var i = 0; i < base64.length; i++)
			{
				out.write(base64[i]);

				if (wrapMode == WrapMode.SLICED && i % 64 == 64 - 1)
				{
					out.write('\n');
				}
			}
			return out.toString(StandardCharsets.US_ASCII);
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
