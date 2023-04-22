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

package io.xeres.app.xrs.service.forum.item;

import io.netty.buffer.ByteBuf;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import jakarta.persistence.Entity;

import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.serialize;
import static io.xeres.app.xrs.serialization.TlvType.STR_MSG;

@Entity(name = "forum_messages")
public class ForumMessageItem extends GxsMessageItem
{
	private String content;

	public ForumMessageItem()
	{
		// Needed for JPA
	}

	public ForumMessageItem(GxsId groupId, MessageId messageId, String name)
	{
		setGxsId(groupId);
		setMessageId(messageId);
		setName(name);
	}

	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
	}

	@Override
	public int writeDataObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		return serialize(buf, STR_MSG, content);
	}

	@Override
	public void readDataObject(ByteBuf buf)
	{
		content = (String) Serializer.deserialize(buf, STR_MSG);
	}
}
