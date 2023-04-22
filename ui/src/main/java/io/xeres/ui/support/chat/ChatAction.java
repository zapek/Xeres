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

package io.xeres.ui.support.chat;

import io.xeres.common.id.GxsId;

import java.util.Objects;
import java.util.stream.Stream;

import static io.xeres.ui.support.chat.ChatAction.Type.*;

public class ChatAction
{
	public enum Type
	{
		JOIN,
		LEAVE,
		SAY,
		SAY_OWN,
		ACTION,
		TIMEOUT
	}

	private Type type;
	private final String nickname;
	private final String gxsId;

	public ChatAction(Type type, String nickname, GxsId gxsId)
	{
		Objects.requireNonNull(type);
		Objects.requireNonNull(nickname);

		this.type = type;
		this.nickname = nickname;
		this.gxsId = gxsId != null ? gxsId.toString() : null; // XXX: fix to always require gxsId...
	}

	public String getAction()
	{
		return switch (type)
				{
					case JOIN -> "-->";
					case LEAVE, TIMEOUT -> "<--";
					case SAY, SAY_OWN -> "<" + nickname + ">";
					case ACTION -> "*";
				};
	}

	public Type getType()
	{
		return type;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	public String getNickname()
	{
		return nickname;
	}

	public String getGxsId()
	{
		return gxsId;
	}

	/**
	 * Checks if it's a presence event. Those events don't have any user content (the user cannot say anything in them).
	 * @return true if it's a presence event (join, leave or timeout).
	 */
	public boolean isPresenceEvent()
	{
		return Stream.of(JOIN, LEAVE, TIMEOUT).anyMatch(v -> type == v);
	}

	/**
	 * Gets a presence content, to put in a line.
	 * @return the presence content
	 */
	public String getPresenceLine()
	{
		if (!isPresenceEvent())
		{
			throw new IllegalStateException("no presence line available, type: " + type);
		}
		var reason = "";
		if (type == TIMEOUT)
		{
			reason = " [Ping timeout]";
		}
		return nickname + " (" + gxsId + ")" + reason;
	}
}
