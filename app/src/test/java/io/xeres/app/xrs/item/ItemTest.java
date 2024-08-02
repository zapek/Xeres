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

package io.xeres.app.xrs.item;

import io.xeres.app.xrs.service.chat.item.ChatRoomMessageItem;
import io.xeres.app.xrs.service.filetransfer.item.TurtleChunkCrcItem;
import io.xeres.app.xrs.service.filetransfer.item.TurtleFileDataItem;
import io.xeres.testutils.Sha1SumFakes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemTest
{
	@Test
	void Item_Bounce_Clone()
	{
		var bounce = new ChatRoomMessageItem("Test");
		var bounceClone = bounce.clone();

		assertEquals(bounce.getMessage(), bounceClone.getMessage());
		assertEquals(bounce.getFlags(), bounceClone.getFlags());
	}

	@Test
	void Item_TurtleChunkCrcItem_Clone()
	{
		var sha1Sum = Sha1SumFakes.createSha1Sum();

		var crcItem = new TurtleChunkCrcItem(1, sha1Sum);
		var crcClone = crcItem.clone();

		assertEquals(crcItem.getChunkNumber(), crcClone.getChunkNumber());
		assertArrayEquals(crcItem.getChecksum().getBytes(), crcClone.getChecksum().getBytes());
	}

	@Test
	void Item_TurtleFileDataItem_Clone()
	{
		byte[] data = {1, 2, 3};

		var turtleFileDataItem = new TurtleFileDataItem(1, data);
		var turtleFileDataItemClone = turtleFileDataItem.clone();

		assertEquals(turtleFileDataItem.getChunkOffset(), turtleFileDataItemClone.getChunkOffset());
		assertArrayEquals(turtleFileDataItem.getChunkData(), turtleFileDataItemClone.getChunkData());
	}
}
