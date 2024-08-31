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

package io.xeres.app.xrs.service.chat;

import org.junit.jupiter.api.Test;

import static io.xeres.app.xrs.service.chat.RoomFlags.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RoomFlagsTest
{
	@Test
	void Enum_Order_Fixed()
	{
		assertEquals(0, AUTO_SUBSCRIBE.ordinal());
		assertEquals(1, DEPRECATED.ordinal());
		assertEquals(2, PUBLIC.ordinal());
		assertEquals(3, CHALLENGE.ordinal());
		assertEquals(4, PGP_SIGNED.ordinal());

		assertEquals(5, values().length);
	}
}
