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

import static io.xeres.app.xrs.common.SecurityKey.Flags.DISTRIBUTION_ADMIN;
import static io.xeres.app.xrs.common.SecurityKey.Flags.TYPE_PUBLIC_ONLY;
import static io.xeres.app.xrs.serialization.TlvSecurityKeySerializer.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TlvSecurityKeySerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(TlvSecurityKeySerializer.class);
	}

	@Test
	void Serialize_TlvSecurityKey()
	{
		var buf = Unpooled.buffer();
		var input = new SecurityKey(new GxsId(Id.toBytes("11111111111111111111111111111111")), EnumSet.of(TYPE_PUBLIC_ONLY, DISTRIBUTION_ADMIN), 1000, 2000, new byte[]{1, 2, 3});

		var size = serialize(buf, input);
		assertEquals(getSize(input), size);

		var result = deserialize(buf);
		assertEquals(input.getKeyGxsId(), result.getKeyGxsId());
		assertEquals(input.getFlags(), result.getFlags());
		assertEquals(input.getValidFromInTs(), result.getValidFromInTs());
		assertEquals(input.getValidToInTs(), result.getValidToInTs());
		assertArrayEquals(input.getData(), result.getData());

		buf.release();
	}
}