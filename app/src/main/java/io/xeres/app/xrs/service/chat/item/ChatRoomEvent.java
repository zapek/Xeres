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

package io.xeres.app.xrs.service.chat.item;

public enum ChatRoomEvent
{
	PEER_LEFT(1),
	PEER_STATUS(2),
	PEER_JOINED(3),
	PEER_CHANGE_NICKNAME(4),
	KEEP_ALIVE(5);

	private final int code;

	ChatRoomEvent(int code)
	{
		this.code = code;
	}

	public byte getCode()
	{
		return (byte) code;
	}
}
