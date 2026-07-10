/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

import io.netty.buffer.Unpooled;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvAddressSerializer.deserialize;
import static io.xeres.app.xrs.serialization.TlvAddressSerializer.serialize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TlvAddressSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(TlvAddressSerializer.class);
	}

	@Test
	void Serialize_TlvAddress()
	{
		var buf = Unpooled.buffer();
		var peerAddress = PeerAddress.fromAddress("192.168.1.1:1234");

		var size = serialize(buf, peerAddress);
		assertEquals(TLV_HEADER_SIZE * 2 + 6, size);

		var result = deserialize(buf);
		assertEquals(PeerAddress.Type.IPV4, result.getType());
		assertTrue(result.isValid());
		assertTrue(result.getAddress().isPresent());
		assertEquals("192.168.1.1:1234", result.getAddress().get());

		buf.release();
	}
}