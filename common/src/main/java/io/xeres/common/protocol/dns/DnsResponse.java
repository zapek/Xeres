/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.common.protocol.dns;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;

class DnsResponse
{
	private final InetAddress address;

	DnsResponse(byte[] response, int id) throws IOException
	{
		var input = new DataInputStream(new ByteArrayInputStream(response));
		var receivedId = input.readShort();
		if (receivedId != id)
		{
			throw new IOException("Wrong ID, expected " + id + ", got: " + receivedId);
		}
		if ((input.readShort() & 0x8000) == 0)
		{
			throw new IOException("Not a response");
		}
		if (input.readShort() != 1)
		{
			throw new IOException("Wrong number of query");
		}
		var answers = input.readShort();
		if (answers != 1)
		{
			throw new IOException("Wrong number of answers, wanted: 1, got: " + answers);
		}
		if (input.readShort() != 0)
		{
			throw new IOException("Wrong number of records");
		}
		if (input.readShort() != 0)
		{
			throw new IOException("Wrong number of additional records");
		}

		// Eat up the questions
		int recordCount;
		while ((recordCount = input.readByte()) > 0)
		{
			for (var i = 0; i < recordCount; i++)
			{
				input.readByte();
			}
		}
		input.readShort(); // Question type
		input.readShort(); // Question class
		input.readShort(); // Field
		if (input.readShort() != 1)
		{
			throw new IOException("Wrong type of answer");
		}
		if (input.readShort() != 1)
		{
			throw new IOException("Wrong class of answer");
		}
		input.readInt(); // TTL
		if (input.readShort() != 4)
		{
			throw new IOException("Wrong length");
		}

		var buf = new byte[4];
		for (var i = 0; i < 4; i++)
		{
			buf[i] = input.readByte();
		}
		address = InetAddress.getByAddress(buf);
	}

	public InetAddress getAddress()
	{
		return address;
	}
}
