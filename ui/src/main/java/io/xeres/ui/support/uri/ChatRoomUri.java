/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.uri;

import io.xeres.common.id.Id;

public record ChatRoomUri(String name, long id) implements Uri
{
	static String AUTHORITY = "chat_room";
	static String PARAMETER_NAME = "name";
	static String PARAMETER_ID = "id";
	static String CHAT_ROOM_PREFIX = "L";
	static String PRIVATE_MESSAGE_PREFIX = "P";
	static String DISTANT_CHAT_PREFIX = "D";
	static String BROADCAST_PREFIX = "L";

	@Override
	public String toString()
	{
		return Uri.buildUri(AUTHORITY,
				PARAMETER_NAME, name,
				PARAMETER_ID, CHAT_ROOM_PREFIX + Id.toString(id));
	}
}
