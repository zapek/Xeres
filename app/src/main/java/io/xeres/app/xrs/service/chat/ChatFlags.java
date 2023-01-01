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

public enum ChatFlags
{
	PRIVATE,
	REQUEST_AVATAR, // set when requesting an avatar, the message must be empty and set to private too
	CONTAINS_AVATAR, // not used anymore
	AVATAR_AVAILABLE, // set if we changed our avatar
	CUSTOM_STATE, // used for ChatStatusItem
	PUBLIC,
	REQUEST_CUSTOM_STATE, // used for ChatStatusItem
	CUSTOM_STATE_AVAILABLE, // used for ChatStatusItem
	PARTIAL_MESSAGE, // "large" messages are splitted
	LOBBY, // XXX: might not be needed because we have a ChatRoomMessageItem
	CLOSING_DISTANT_CONNECTION,
	ACK_DISTANT_CONNECTION,
	KEEP_ALIVE,
	CONNECTION_REFUSED
}
