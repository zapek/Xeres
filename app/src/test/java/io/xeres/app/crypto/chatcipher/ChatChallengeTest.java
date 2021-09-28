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

package io.xeres.app.crypto.chatcipher;

import io.xeres.common.id.GxsId;
import io.xeres.common.id.Id;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatChallengeTest
{
	@Test
	void ChatChallenge_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ChatChallenge.class);
	}

	@Test
	void ChatChallenge_Code_OK()
	{
		GxsId gxsId = new GxsId(Id.toBytes("325e3801988a347347ef3e5ae24a63ba"));

		long code = ChatChallenge.code(gxsId, 1, 2);

		assertEquals(749218228209201600L, code);
	}
}
