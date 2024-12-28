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

package io.xeres.common.message.chat;

import io.xeres.common.id.GxsId;

public class ChatRoomUserEvent
{
	private GxsId gxsId;
	private String nickname;
	private long identityId;

	public ChatRoomUserEvent()
	{
		// Needed for JSON
	}

	public ChatRoomUserEvent(GxsId gxsId, String nickname, long identityId)
	{
		this.gxsId = gxsId;
		this.nickname = nickname;
		this.identityId = identityId;
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	public void setGxsId(GxsId gxsId)
	{
		this.gxsId = gxsId;
	}

	public String getNickname()
	{
		return nickname != null ? nickname : ""; // Workaround against users having a null nickname
	}

	public void setNickname(String nickname)
	{
		this.nickname = nickname;
	}

	public long getIdentityId()
	{
		return identityId;
	}

	public void setIdentityId(long identityId)
	{
		this.identityId = identityId;
	}
}
