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

/**
 * Used to send messages from a chat client to a web socket only.
 * If a chat message has no content, it's a notification.
 */
public class ChatMessage
{
	private String content;

	public ChatMessage()
	{
		// Needed for JSON
	}

	public ChatMessage(String message)
	{
		this.content = message;
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
		return "ChatMessage{" +
				"content='" + content + "'" +
				'}';
	}
}
