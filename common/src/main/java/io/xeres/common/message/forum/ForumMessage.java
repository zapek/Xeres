/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.common.message.forum;

import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;

import java.time.Instant;

public class ForumMessage
{
	private long id;
	private GxsId gxsId;
	private MessageId messageId;
	private MessageId parentId;
	private GxsId authorId;
	private String authorName;
	private String name;
	private Instant published;
	private String content;

	public ForumMessage()
	{
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	public void setGxsId(GxsId gxsId)
	{
		this.gxsId = gxsId;
	}

	public MessageId getMessageId()
	{
		return messageId;
	}

	public void setMessageId(MessageId messageId)
	{
		this.messageId = messageId;
	}

	public MessageId getParentId()
	{
		return parentId;
	}

	public void setParentId(MessageId parentId)
	{
		this.parentId = parentId;
	}

	public GxsId getAuthorId()
	{
		return authorId;
	}

	public void setAuthorId(GxsId authorId)
	{
		this.authorId = authorId;
	}

	public String getAuthorName()
	{
		return authorName;
	}

	public void setAuthorName(String authorName)
	{
		this.authorName = authorName;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Instant getPublished()
	{
		return published;
	}

	public void setPublished(Instant published)
	{
		this.published = published;
	}

	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
	}
}
