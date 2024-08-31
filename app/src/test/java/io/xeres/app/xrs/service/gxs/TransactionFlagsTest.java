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

package io.xeres.app.xrs.service.gxs;

import org.junit.jupiter.api.Test;

import static io.xeres.app.xrs.service.gxs.item.TransactionFlags.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionFlagsTest
{
	@Test
	void Enum_Order_Fixed()
	{
		assertEquals(0, START.ordinal());
		assertEquals(1, START_ACKNOWLEDGE.ordinal());
		assertEquals(2, END_SUCCESS.ordinal());
		assertEquals(3, CANCEL.ordinal());
		assertEquals(4, END_FAIL_NUM.ordinal());
		assertEquals(5, END_FAIL_TIMEOUT.ordinal());
		assertEquals(6, END_FAIL_FULL.ordinal());
		assertEquals(8, TYPE_GROUP_LIST_RESPONSE.ordinal());
		assertEquals(9, TYPE_MESSAGE_LIST_RESPONSE.ordinal());
		assertEquals(10, TYPE_GROUP_LIST_REQUEST.ordinal());
		assertEquals(11, TYPE_MESSAGE_LIST_REQUEST.ordinal());
		assertEquals(12, TYPE_GROUPS.ordinal());
		assertEquals(13, TYPE_MESSAGES.ordinal());
		assertEquals(14, TYPE_ENCRYPTED_DATA.ordinal());

		assertEquals(15, values().length);
	}
}
