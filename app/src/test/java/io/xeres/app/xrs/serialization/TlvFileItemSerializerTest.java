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
import io.xeres.app.xrs.common.FileItem;
import io.xeres.testutils.Sha1SumFakes;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static io.xeres.app.xrs.serialization.TlvFileItemSerializer.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TlvFileItemSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(TlvFileItemSerializer.class);
	}

	@Test
	void Serialize_With_Optional_Fields()
	{
		var buf = Unpooled.buffer();
		var hash = Sha1SumFakes.createSha1Sum();
		var fileItem = new FileItem(1024, hash, "test.txt", "/some/path", 5);

		var size = serialize(buf, fileItem);
		var result = deserialize(buf);

		assertEquals(fileItem.size(), result.size());
		assertEquals(fileItem.hash(), result.hash());
		assertEquals(fileItem.name(), result.name());
		assertEquals(fileItem.path(), result.path());
		assertEquals(fileItem.age(), result.age());
		assertEquals(size, getSize(fileItem));

		buf.release();
	}

	@Test
	void Serialize_Without_Optional_Fields()
	{
		var buf = Unpooled.buffer();
		var hash = Sha1SumFakes.createSha1Sum();
		var fileItem = new FileItem(0, hash, null, null, 0);

		var size = serialize(buf, fileItem);
		var result = deserialize(buf);

		assertEquals(fileItem.size(), result.size());
		assertEquals(fileItem.hash(), result.hash());
		assertEquals(fileItem.name(), result.name());
		assertEquals(fileItem.path(), result.path());
		assertEquals(fileItem.age(), result.age());
		assertEquals(size, getSize(fileItem));

		buf.release();
	}
}
