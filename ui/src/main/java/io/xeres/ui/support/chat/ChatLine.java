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
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

public class ChatLine
{
	private static final Logger log = LoggerFactory.getLogger(ChatLine.class);

	private final Instant instant;
	private final ChatAction action;
	private final List<Content> contents;

	public ChatLine(Instant instant, ChatAction action, Content... contents)
	{
		this.instant = instant;
		this.action = action;
		if (action.isPresenceEvent())
		{
			if (log.isDebugEnabled() && contents.length > 0)
			{
				log.debug("Chat content for action {} is not needed", action);
			}
			this.contents = List.of(new ContentText(action.getPresenceLine()));
		}
		else
		{
			this.contents = List.of(contents); // XXX: maybe turn chatContents into a list... see how the parsing goes
		}
	}

	public Instant getInstant()
	{
		return instant;
	}

	public String getAction()
	{
		return action.getAction();
	}

	public boolean hasSaid(GxsId gxsId)
	{
		return action.getType() == ChatAction.Type.SAY && gxsId.toString().equals(action.getGxsId());
	}

	public String getNicknameColor()
	{
		return switch (action.getType())
				{
					case SAY -> ColorGenerator.generateColor(action.getGxsId() != null ? action.getGxsId() : action.getNickname());
					default -> null;
				};
	}

	public boolean isActiveAction()
	{
		return switch (action.getType())
		{
			case JOIN, LEAVE, TIMEOUT:
				yield false;
			case SAY, SAY_OWN, ACTION:
				yield true;
		};
	}

	public List<Content> getChatContents()
	{
		return contents;
	}

	/**
	 * Tells if a ChatLine contains "rich" content, that is, anything else than a line of text.
	 *
	 * @return true if the content is rich content
	 */
	public boolean isRich()
	{
		return contents.size() > 1 || !(contents.get(0) instanceof ContentText);
	}
}
