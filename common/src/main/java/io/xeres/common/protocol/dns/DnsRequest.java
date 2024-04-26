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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

class DnsRequest
{
	private final ByteArrayOutputStream array;
	private final short id;

	DnsRequest(String hostname) throws IOException
	{
		id = (short) ThreadLocalRandom.current().nextInt();

		array = new ByteArrayOutputStream();
		var out = new DataOutputStream(array);

		// ID
		out.writeShort(id);
		// Write Query Flags (recursion desired)
		out.writeShort(0x0100);
		// Question Count
		out.writeShort(0x0001);
		// Answer Record Count
		out.writeShort(0x0000);
		// Authority Record Count
		out.writeShort(0x0000);
		// Additional Record Count
		out.writeShort(0x0000);

		// Query Name
		var domainParts = hostname.split("\\.");

		for (String domainPart : domainParts)
		{
			var domainBytes = domainPart.getBytes(StandardCharsets.UTF_8);
			out.writeByte(domainBytes.length);
			out.write(domainBytes);
		}
		out.writeByte(0x00); // Terminator

		// Query Type 0x01 = A record (host addresses)
		out.writeShort(0x0001);

		// Query Class 0x01 = Internet Address
		out.writeShort(0x0001);
	}

	byte[] toByteArray()
	{
		return array.toByteArray();
	}

	public int getId()
	{
		return id;
	}
}
