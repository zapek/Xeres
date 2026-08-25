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
import io.xeres.app.xrs.common.FileSet;
import io.xeres.testutils.Sha1SumFakes;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.xeres.app.xrs.serialization.TlvFileSetSerializer.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TlvFileSetSerializerTest
{
	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(TlvFileSetSerializer.class);
	}

	@Test
	void Serialize_With_Title_And_Comment()
	{
		var buf = Unpooled.buffer();
		var hash1 = Sha1SumFakes.createSha1Sum();
		var hash2 = Sha1SumFakes.createSha1Sum();
		var fileItem1 = new FileItem(1024, hash1, "file1.txt", "/path1", 1);
		var fileItem2 = new FileItem(2048, hash2, "file2.txt", "/path2", 2);
		var fileSet = new FileSet(List.of(fileItem1, fileItem2), "My Title", "My Comment");

		var size = serialize(buf, fileSet);
		var result = deserialize(buf);

		assertEquals(fileSet.fileItems().size(), result.fileItems().size());
		assertEquals(fileSet.fileItems().getFirst().name(), result.fileItems().getFirst().name());
		assertEquals(fileSet.fileItems().getFirst().size(), result.fileItems().getFirst().size());
		assertEquals(fileSet.fileItems().getFirst().hash(), result.fileItems().getFirst().hash());
		assertEquals(fileSet.fileItems().get(1).name(), result.fileItems().get(1).name());
		assertEquals(fileSet.fileItems().get(1).size(), result.fileItems().get(1).size());
		assertEquals(fileSet.fileItems().get(1).hash(), result.fileItems().get(1).hash());
		assertEquals(fileSet.title(), result.title());
		assertEquals(fileSet.comment(), result.comment());
		assertEquals(size, getSize(fileSet));

		buf.release();
	}

	@Test
	void Serialize_Without_Title_And_Comment()
	{
		var buf = Unpooled.buffer();
		var hash = Sha1SumFakes.createSha1Sum();
		var fileItem = new FileItem(512, hash, "data.bin", null, 0);
		var fileSet = new FileSet(List.of(fileItem), null, null);

		var size = serialize(buf, fileSet);
		var result = deserialize(buf);

		assertEquals(fileSet.fileItems().size(), result.fileItems().size());
		assertEquals(fileSet.fileItems().getFirst().name(), result.fileItems().getFirst().name());
		assertEquals(fileSet.fileItems().getFirst().size(), result.fileItems().getFirst().size());
		assertEquals(fileSet.fileItems().getFirst().hash(), result.fileItems().getFirst().hash());
		assertEquals(fileSet.title(), result.title());
		assertEquals(fileSet.comment(), result.comment());
		assertEquals(size, getSize(fileSet));

		buf.release();
	}
}
