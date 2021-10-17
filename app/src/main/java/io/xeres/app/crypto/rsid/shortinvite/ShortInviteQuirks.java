/*
 * Copyright (c) 2019-2020 by David Gerber - https://zapek.com
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

package io.xeres.app.crypto.rsid.shortinvite;

public final class ShortInviteQuirks
{
	private ShortInviteQuirks()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Retroshare puts IP addresses in big-endian in certificates, but when it comes
	 * to short invites, a mistake was made and, while the port is in big-endian, the
	 * IP address is not. Since the mistake is done on output and input, it works fine
	 * within Retroshare so a workaround has to be implemented here.
	 *
	 * @param data the IP address + port
	 * @return the IP address in swapped endian + port left alone
	 */
	public static byte[] swapBytes(byte[] data)
	{
		if (data == null || data.length != 6)
		{
			return data; // don't touch anything, input is bad
		}
		var bytes = new byte[6];
		bytes[0] = data[3];
		bytes[1] = data[2];
		bytes[2] = data[1];
		bytes[3] = data[0];
		bytes[4] = data[4];
		bytes[5] = data[5];

		return bytes;
	}

	public static byte[] swapDnsBytes(byte[] data)
	{
		if (data == null || data.length < 4)
		{
			return data; // don't touch anything, input is bad
		}
		var bytes = new byte[data.length];
		System.arraycopy(data, 0, bytes, 2, data.length - 2);
		bytes[0] = data[data.length - 2];
		bytes[1] = data[data.length - 1];

		return bytes;
	}
}
