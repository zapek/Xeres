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

package io.xeres.app.database.model.gxs;

import io.xeres.app.database.model.forum.ForumMessageItemSummary;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;

import java.time.Instant;
import java.util.Objects;

public final class ForumMessageItemSummaryFake implements ForumMessageItemSummary
{
	private final long id;
	private final String name;
	private final GxsId gxsId;
	private final MessageId messageId;
	private final MessageId originalMessageId;
	private final MessageId parentId;
	private final GxsId authorId;
	private final Instant published;
	private final boolean read;

	public ForumMessageItemSummaryFake(long id, String name, GxsId gxsId, MessageId messageId, MessageId originalMessageId, MessageId parentId, GxsId authorId, Instant published, boolean read)
	{
		this.id = id;
		this.name = name;
		this.gxsId = gxsId;
		this.messageId = messageId;
		this.originalMessageId = originalMessageId;
		this.parentId = parentId;
		this.authorId = authorId;
		this.published = published;
		this.read = read;
	}

	@Override
	public long getId()
	{
		return id;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public GxsId getGxsId()
	{
		return gxsId;
	}

	@Override
	public MessageId getMessageId()
	{
		return messageId;
	}

	@Override
	public MessageId getOriginalMessageId()
	{
		return originalMessageId;
	}

	@Override
	public MessageId getParentId()
	{
		return parentId;
	}

	@Override
	public GxsId getAuthorId()
	{
		return authorId;
	}

	@Override
	public Instant getPublished()
	{
		return published;
	}

	@Override
	public boolean isRead()
	{
		return read;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this) return true;
		if (obj == null || obj.getClass() != getClass()) return false;
		var that = (ForumMessageItemSummaryFake) obj;
		return id == that.id &&
				Objects.equals(name, that.name) &&
				Objects.equals(gxsId, that.gxsId) &&
				Objects.equals(messageId, that.messageId) &&
				Objects.equals(originalMessageId, that.originalMessageId) &&
				Objects.equals(parentId, that.parentId) &&
				Objects.equals(authorId, that.authorId) &&
				Objects.equals(published, that.published) &&
				read == that.read;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, gxsId, messageId, originalMessageId, parentId, authorId, published, read);
	}

	@Override
	public String toString()
	{
		return "ForumMessageItemSummaryFake[" +
				"id=" + id + ", " +
				"name=" + name + ", " +
				"gxsId=" + gxsId + ", " +
				"messageId=" + messageId + ", " +
				"originalMessageId=" + originalMessageId + ", " +
				"parentId=" + parentId + ", " +
				"authorId=" + authorId + ", " +
				"published=" + published + ", " +
				"read=" + read + ']';
	}

}
