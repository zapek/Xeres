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
import io.xeres.app.database.model.gxs.ForumGroupItemFakes;
import io.xeres.app.database.model.gxs.ForumMessageItemFakes;
import io.xeres.app.database.model.gxs.IdentityGroupItemFakes;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static io.xeres.app.xrs.serialization.GxsMetaAndDataSerializer.serialize;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GxsMetaAndDataSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(GxsMetaAndDataSerializer.class);
	}

	@Test
	void Serialize_IdentityGroupItem()
	{
		var buf = Unpooled.buffer();
		var identityGroupItem = IdentityGroupItemFakes.createIdentityGroupItem();
		var result = new GxsMetaAndDataResult();

		var size = serialize(buf, identityGroupItem, EnumSet.noneOf(SerializationFlags.class), result);
		assertEquals(192, size);

		buf.release();
	}

	@Test
	void Serialize_ForumGroupItem()
	{
		var buf = Unpooled.buffer();
		var forumGroupItem = ForumGroupItemFakes.createForumGroupItem();
		var result = new GxsMetaAndDataResult();

		var size = serialize(buf, forumGroupItem, EnumSet.noneOf(SerializationFlags.class), result);
		assertEquals(172, size);

		buf.release();
	}

	@Test
	void Serialize_ForumMessageItem()
	{
		var buf = Unpooled.buffer();
		var forumMessageItem = ForumMessageItemFakes.createForumMessageItem();
		var result = new GxsMetaAndDataResult();

		var size = serialize(buf, forumMessageItem, EnumSet.noneOf(SerializationFlags.class), result);
		assertEquals(154, size);

		buf.release();
	}
}