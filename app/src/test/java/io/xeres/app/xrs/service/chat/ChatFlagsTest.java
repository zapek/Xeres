/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

import static io.xeres.app.xrs.service.chat.ChatFlags.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatFlagsTest
{
	@Test
	void ChatFlags_Enum_Order()
	{
		assertEquals(0, PRIVATE.ordinal());
		assertEquals(1, REQUEST_AVATAR.ordinal());
		assertEquals(2, CONTAINS_AVATAR.ordinal());
		assertEquals(3, AVATAR_AVAILABLE.ordinal());
		assertEquals(4, CUSTOM_STATE.ordinal());
		assertEquals(5, PUBLIC.ordinal());
		assertEquals(6, REQUEST_CUSTOM_STATE.ordinal());
		assertEquals(7, CUSTOM_STATE_AVAILABLE.ordinal());
		assertEquals(8, PARTIAL_MESSAGE.ordinal());
		assertEquals(9, LOBBY.ordinal());
		assertEquals(10, CLOSING_DISTANT_CONNECTION.ordinal());
		assertEquals(11, ACK_DISTANT_CONNECTION.ordinal());
		assertEquals(12, KEEP_ALIVE.ordinal());
		assertEquals(13, CONNECTION_REFUSED.ordinal());

		assertEquals(14, values().length);
	}
}
