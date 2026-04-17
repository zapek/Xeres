/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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
import io.xeres.common.id.MsgId;

import java.time.Instant;
import java.util.Objects;

public final class ForumMessageItemSummaryFake implements ForumMessageItemSummary
{
	private final long id;
	private final String name;
	private final GxsId gxsId;
	private final MsgId msgId;
	private final MsgId originalMsgId;
	private final MsgId parentMsgId;
	private final GxsId authorGxsId;
	private final Instant published;
	private final boolean read;

	public ForumMessageItemSummaryFake(long id, String name, GxsId gxsId, MsgId msgId, MsgId originalMsgId, MsgId parentMsgId, GxsId authorGxsId, Instant published, boolean read)
	{
		this.id = id;
		this.name = name;
		this.gxsId = gxsId;
		this.msgId = msgId;
		this.originalMsgId = originalMsgId;
		this.parentMsgId = parentMsgId;
		this.authorGxsId = authorGxsId;
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
	public MsgId getMsgId()
	{
		return msgId;
	}

	@Override
	public MsgId getOriginalMsgId()
	{
		return originalMsgId;
	}

	@Override
	public MsgId getParentMsgId()
	{
		return parentMsgId;
	}

	@Override
	public GxsId getAuthorGxsId()
	{
		return authorGxsId;
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
				Objects.equals(msgId, that.msgId) &&
				Objects.equals(originalMsgId, that.originalMsgId) &&
				Objects.equals(parentMsgId, that.parentMsgId) &&
				Objects.equals(authorGxsId, that.authorGxsId) &&
				Objects.equals(published, that.published) &&
				read == that.read;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name, gxsId, msgId, originalMsgId, parentMsgId, authorGxsId, published, read);
	}

	@Override
	public String toString()
	{
		return "ForumMessageItemSummaryFake[" +
				"id=" + id + ", " +
				"name=" + name + ", " +
				"gxsId=" + gxsId + ", " +
				"msgId=" + msgId + ", " +
				"originalMsgId=" + originalMsgId + ", " +
				"parentMsgId=" + parentMsgId + ", " +
				"authorMsgId=" + authorGxsId + ", " +
				"published=" + published + ", " +
				"read=" + read + ']';
	}

}
