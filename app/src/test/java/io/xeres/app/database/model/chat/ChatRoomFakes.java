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

package io.xeres.app.database.model.chat;

import io.xeres.app.database.model.identity.Identity;
import io.xeres.app.database.model.identity.IdentityFakes;
import io.xeres.common.identity.Type;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.concurrent.ThreadLocalRandom;

public final class ChatRoomFakes
{
	private ChatRoomFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ChatRoom createChatRoom()
	{
		return createChatRoom(ThreadLocalRandom.current().nextLong(), IdentityFakes.createOwnIdentity("test", Type.SIGNED), RandomStringUtils.randomAlphabetic(8), RandomStringUtils.randomAlphabetic(8), 0);
	}

	public static ChatRoom createChatRoom(Identity identity)
	{
		return createChatRoom(ThreadLocalRandom.current().nextLong(), identity, RandomStringUtils.randomAlphabetic(8), RandomStringUtils.randomAlphabetic(8), 0);
	}

	public static ChatRoom createChatRoom(long roomId, Identity identity, String name, String topic, int flags)
	{
		return new ChatRoom(roomId, identity, name, topic, flags);
	}
}
