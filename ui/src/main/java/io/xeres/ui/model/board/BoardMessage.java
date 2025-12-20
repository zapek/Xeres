/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.model.board;

import io.micrometer.common.util.StringUtils;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;

import java.time.Instant;

public class BoardMessage
{
	private long id;
	private GxsId gxsId;
	private MessageId messageId;
	private long originalId;
	private long parentId;
	private GxsId authorId;
	private String authorName;
	private String name;
	private Instant published;
	private String content;
	private String link;
	private boolean hasImage;
	private boolean read;

	public BoardMessage()
	{
		// Needed
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

	public long getOriginalId()
	{
		return originalId;
	}

	public void setOriginalId(long originalId)
	{
		this.originalId = originalId;
	}

	public long getParentId()
	{
		return parentId;
	}

	public void setParentId(long parentId)
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

	public boolean hasLink()
	{
		return StringUtils.isNotBlank(link);
	}

	public String getLink()
	{
		return link;
	}

	public void setLink(String link)
	{
		this.link = link;
	}

	public boolean hasImage()
	{
		return hasImage;
	}

	public void setHasImage(boolean hasImage)
	{
		this.hasImage = hasImage;
	}

	public boolean isRead()
	{
		return read;
	}

	public void setRead(boolean read)
	{
		this.read = read;
	}
}
