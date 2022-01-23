/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

import static io.xeres.ui.support.chat.ChatAction.Type.JOIN;
import static io.xeres.ui.support.chat.ChatAction.Type.LEAVE;

public class ChatAction
{
	public enum Type
	{
		JOIN,
		LEAVE,
		SAY
	}

	private final Type type;
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
					case LEAVE -> "<--";
					case SAY -> "<" + nickname + ">";
				};
	}

	public Type getType()
	{
		return type;
	}

	public String getNickname()
	{
		return nickname;
	}

	public String getGxsId()
	{
		return gxsId;
	}

	public boolean hasMessageLine()
	{
		return type == JOIN || type == LEAVE;
	}

	public String getMessageLine()
	{
		if (!hasMessageLine())
		{
			throw new IllegalStateException("no message line available, type: " + type);
		}
		return nickname + " (" + gxsId + ")";
	}
}
