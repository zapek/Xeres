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

import io.xeres.app.database.model.identity.GxsIdFakes;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.message.chat.RoomType;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.concurrent.ThreadLocalRandom;

public final class ChatRoomFakes
{
	private ChatRoomFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ChatRoom createChatRoomEntity()
	{
		return createChatRoomEntity(ThreadLocalRandom.current().nextLong(), GxsIdFakes.createOwnIdentity(), RandomStringUtils.randomAlphabetic(8), RandomStringUtils.randomAlphabetic(8), 0);
	}

	public static ChatRoom createChatRoomEntity(IdentityGroupItem identityGroupItem)
	{
		return createChatRoomEntity(ThreadLocalRandom.current().nextLong(), identityGroupItem, RandomStringUtils.randomAlphabetic(8), RandomStringUtils.randomAlphabetic(8), 0);
	}

	public static ChatRoom createChatRoomEntity(long roomId, IdentityGroupItem identityGroupItem, String name, String topic, int flags)
	{
		return new ChatRoom(roomId, identityGroupItem, name, topic, flags);
	}

	public static io.xeres.app.xrs.service.chat.ChatRoom createChatRoom()
	{
		return createChatRoom(ThreadLocalRandom.current().nextLong(), RandomStringUtils.randomAlphabetic(8), RandomStringUtils.randomAlphabetic(8), RoomType.PUBLIC, 5, false);
	}

	public static io.xeres.app.xrs.service.chat.ChatRoom createChatRoom(long id, String name, String topic, RoomType roomType, int userCount, boolean isSigned)
	{
		return new io.xeres.app.xrs.service.chat.ChatRoom(id, name, topic, roomType, userCount, isSigned);
	}
}
