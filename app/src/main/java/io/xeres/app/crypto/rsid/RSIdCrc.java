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

package io.xeres.app.crypto.rsid;

final class RSIdCrc
{
	private RSIdCrc()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int calculate24bitsCrc(byte[] data, int length)
	{
		var crc = 0xb704ce;

		for (var i = 0; i < length; i++)
		{
			crc ^= data[i] << 16;
			for (var j = 0; j < 8; j++)
			{
				crc <<= 1;
				if ((crc & 0x1000000) != 0)
				{
					crc ^= 0x1864cfb;
				}
			}
		}
		return crc & 0xffffff;
	}
}
