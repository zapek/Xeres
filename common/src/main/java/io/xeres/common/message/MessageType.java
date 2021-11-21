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

package io.xeres.common.message;

public enum MessageType
{
	CHAT_PRIVATE_MESSAGE,
	CHAT_ROOM_MESSAGE,
	CHAT_ROOM_LIST,
	CHAT_BROADCAST_MESSAGE,
	CHAT_TYPING_NOTIFICATION,
	CHAT_ROOM_JOIN,
	CHAT_ROOM_LEAVE,
	CHAT_ROOM_TYPING_NOTIFICATION,
	CHAT_ROOM_USER_JOIN,
	CHAT_ROOM_USER_LEAVE,
	CHAT_ROOM_USER_KEEP_ALIVE
}
