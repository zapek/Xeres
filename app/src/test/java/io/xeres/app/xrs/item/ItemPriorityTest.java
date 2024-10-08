/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

import org.junit.jupiter.api.Test;

import static io.xeres.app.xrs.item.ItemPriority.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemPriorityTest
{
	@Test
	void Enum_Value_Fixed()
	{
		assertEquals(2, BACKGROUND.getPriority());
		assertEquals(3, DEFAULT.getPriority());
		assertEquals(5, NORMAL.getPriority());
		assertEquals(6, HIGH.getPriority());
		assertEquals(7, INTERACTIVE.getPriority());
		assertEquals(8, IMPORTANT.getPriority());
		assertEquals(9, REALTIME.getPriority());

		assertEquals(7, values().length);
	}
}
