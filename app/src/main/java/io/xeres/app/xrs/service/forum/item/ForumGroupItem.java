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

package io.xeres.app.xrs.service.forum.item;

import io.netty.buffer.ByteBuf;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;

import java.util.HashSet;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.serialize;
import static io.xeres.app.xrs.serialization.TlvType.SET_GXS_ID;
import static io.xeres.app.xrs.serialization.TlvType.SET_GXS_MSG_ID;

@Entity(name = "forum_groups")
public class ForumGroupItem extends GxsGroupItem
{
	private String description;

	@ElementCollection
	private Set<GxsId> adminList = new HashSet<>();

	@ElementCollection
	private Set<MessageId> pinnedPosts = new HashSet<>();

	public ForumGroupItem()
	{
		// Needed for JPA
	}

	public ForumGroupItem(GxsId gxsId, String name)
	{
		setGxsId(gxsId);
		setName(name);
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	@Override
	public int writeGroupObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, description);
		size += serialize(buf, SET_GXS_ID, adminList);
		size += serialize(buf, SET_GXS_MSG_ID, pinnedPosts);

		return size;
	}

	@Override
	public void readGroupObject(ByteBuf buf)
	{
		description = Serializer.deserializeString(buf);
		//noinspection unchecked
		adminList = (Set<GxsId>) Serializer.deserialize(buf, SET_GXS_ID);
		//noinspection unchecked
		pinnedPosts = (Set<MessageId>) Serializer.deserialize(buf, SET_GXS_MSG_ID);
	}

	@Override
	public RsServiceType getServiceType()
	{
		return RsServiceType.FORUMS;
	}
}
