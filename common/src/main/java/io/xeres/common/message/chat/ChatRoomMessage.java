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

public class ChatRoomMessage
{
	private long roomId;
	private String senderNickname;
	private GxsId gxsId;
	private String content;

	public ChatRoomMessage()
	{
		// Needed for JSON
	}

	public ChatRoomMessage(String senderNickname, GxsId gxsId, String content)
	{
		this.senderNickname = senderNickname;
		this.gxsId = gxsId;
		this.content = content;
	}

	public long getRoomId()
	{
		return roomId;
	}

	public void setRoomId(long roomId)
	{
		this.roomId = roomId;
	}

	public String getSenderNickname()
	{
		return senderNickname;
	}

	public void setSenderNickname(String senderNickname)
	{
		this.senderNickname = senderNickname;
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	public void setGxsId(GxsId gxsId)
	{
		this.gxsId = gxsId;
	}

	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
	}

	public boolean isEmpty()
	{
		return content == null;
	}

	@Override
	public String toString()
	{
		return "ChatRoomMessage{" +
				"roomId=" + roomId +
				", senderNickname='" + senderNickname + '\'' +
				", gxsId'" + gxsId + '\'' +
				", content='" + content + '\'' +
				'}';
	}
}
