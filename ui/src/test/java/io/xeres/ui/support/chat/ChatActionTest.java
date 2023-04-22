/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.chat;

import io.xeres.testutils.GxsIdFakes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatActionTest
{
	@Test
	void ChatAction_HasMessageLine_OK()
	{
		var gxsId = GxsIdFakes.createGxsId();
		var action = new ChatAction(ChatAction.Type.JOIN, "Foobar", gxsId);

		assertTrue(action.isPresenceEvent());
		assertEquals("Foobar (" + gxsId + ")", action.getPresenceLine());
	}

	@Test
	void ChatAction_HasMessageLine_None()
	{
		var action = new ChatAction(ChatAction.Type.SAY, "Foobar", GxsIdFakes.createGxsId());

		assertFalse(action.isPresenceEvent());
	}
}
