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
import io.xeres.app.xrs.common.SecurityKey;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Id;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static io.xeres.app.xrs.common.SecurityKey.Flags.DISTRIBUTION_ADMIN;
import static io.xeres.app.xrs.common.SecurityKey.Flags.TYPE_PUBLIC_ONLY;
import static io.xeres.app.xrs.serialization.TlvSecurityKeySetSerializer.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TlvSecurityKeySetSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(TlvSecurityKeySetSerializer.class);
	}

	@Test
	void Serialize_Single_Key()
	{
		var buf = Unpooled.buffer();
		var securityKey = new SecurityKey(new GxsId(Id.toBytes("11111111111111111111111111111111")), EnumSet.of(TYPE_PUBLIC_ONLY, DISTRIBUTION_ADMIN), 0, 1, new byte[]{1, 2, 3});
		var securityKeys = Set.of(securityKey);

		var size = serialize(buf, securityKeys);
		var result = deserialize(buf);

		assertEquals(1, result.size());
		assertTrue(result.contains(securityKey));
		assertEquals(size, getSize(securityKeys));

		buf.release();
	}

	@Test
	void Serialize_Multiple_Keys()
	{
		var buf = Unpooled.buffer();
		var key1 = new SecurityKey(new GxsId(Id.toBytes("11111111111111111111111111111111")), EnumSet.of(TYPE_PUBLIC_ONLY, DISTRIBUTION_ADMIN), 0, 1, new byte[]{1});
		var key2 = new SecurityKey(new GxsId(Id.toBytes("22222222222222222222222222222222")), EnumSet.of(TYPE_PUBLIC_ONLY), 100, 200, new byte[]{4, 5});
		var securityKeys = Set.of(key1, key2);

		var size = serialize(buf, securityKeys);
		var result = deserialize(buf);

		assertEquals(2, result.size());
		assertTrue(result.contains(key1));
		assertTrue(result.contains(key2));
		assertEquals(size, getSize(securityKeys));

		buf.release();
	}
}
