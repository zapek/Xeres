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

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.chat.ChatFlags;

import java.time.Instant;
import java.util.Set;

import static io.xeres.app.xrs.serialization.TlvType.STR_MSG;
import static io.xeres.app.xrs.service.chat.ChatFlags.*;

public class ChatMessageItem extends Item
{
	@RsSerialized
	private Set<ChatFlags> flags;

	@RsSerialized
	private int sendTime;

	@RsSerialized(tlvType = STR_MSG)
	private String message;

	@SuppressWarnings("unused")
	public ChatMessageItem()
	{
		// Required
	}

	public ChatMessageItem(String message, Set<ChatFlags> flags)
	{
		this.message = message;
		this.sendTime = (int) Instant.now().getEpochSecond();
		this.flags = flags;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.INTERACTIVE.getPriority();
	}

	public Set<ChatFlags> getFlags()
	{
		return flags;
	}

	public int getSendTime()
	{
		return sendTime;
	}

	public String getMessage()
	{
		return message;
	}

	public boolean isPrivate()
	{
		return flags.contains(PRIVATE);
	}

	public boolean isPartial()
	{
		return flags.contains(PARTIAL_MESSAGE);
	}

	public boolean isAvatarRequest()
	{
		return flags.contains(REQUEST_AVATAR);
	}

	@Override
	public String toString()
	{
		return "ChatMessageItem{" +
				"flags=" + flags +
				", sendTime=" + sendTime +
				", message='" + message + '\'' +
				'}';
	}
}
