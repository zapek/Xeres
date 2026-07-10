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
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.id.ProfileFingerprint;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static io.xeres.app.xrs.serialization.IdentifierSerializer.deserialize;
import static io.xeres.app.xrs.serialization.IdentifierSerializer.serialize;
import static org.junit.jupiter.api.Assertions.*;

class IdentifierSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(IdentifierSerializer.class);
	}

	@Test
	void Serialize_Identifier()
	{
		var buf = Unpooled.buffer();

		var input = LocationFakes.createLocation().getLocationIdentifier();

		var size = serialize(buf, LocationIdentifier.class, input);

		assertEquals(input.getLength(), size);
		var output = new byte[input.getLength()];
		buf.getBytes(0, output);
		assertArrayEquals(input.getBytes(), output);

		var result = (LocationIdentifier) deserialize(buf, LocationIdentifier.class);

		assertEquals(input, result);
		buf.release();
	}

	@Test
	void Serialize_Identifier_Null()
	{
		var buf = Unpooled.buffer();

		var size = serialize(buf, GxsId.class, null);
		assertEquals(GxsId.LENGTH, size);

		var result = (GxsId) deserialize(buf, GxsId.class);
		assertNull(result);
		buf.release();
	}

	@Test
	void Serialize_Identifier_Null_Dynamic()
	{
		var buf = Unpooled.buffer();

		assertThrows(IllegalStateException.class, () -> serialize(buf, ProfileFingerprint.class, null));
		buf.release();
	}
}