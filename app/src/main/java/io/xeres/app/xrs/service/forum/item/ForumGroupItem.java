/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.serialize;
import static io.xeres.app.xrs.serialization.TlvType.*;

@Entity(name = "forum_group")
public class ForumGroupItem extends GxsGroupItem
{
	private String description;

	@ElementCollection
	@AttributeOverride(name = "identifier", column = @Column(name = "admin"))
	private Set<GxsId> admins = new HashSet<>();

	@ElementCollection
	@AttributeOverride(name = "identifier", column = @Column(name = "pinned_post"))
	private Set<MessageId> pinnedPosts = new HashSet<>();

	@Transient
	private boolean oldVersion; // Needed because RS added admins and pinnedPosts later, and it would break signature verification otherwise

	public ForumGroupItem()
	{
		// Needed for JPA
	}

	public ForumGroupItem(GxsId gxsId, String name)
	{
		setGxsId(gxsId);
		setName(name);
		updatePublished();
	}

	@Override
	public int getSubType()
	{
		return 2;
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
	public int writeDataObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, STR_DESCR, description);
		if (!oldVersion)
		{
			size += serialize(buf, SET_GXS_ID, admins);
			size += serialize(buf, SET_GXS_MSG_ID, pinnedPosts);
		}
		return size;
	}

	@Override
	public void readDataObject(ByteBuf buf)
	{
		description = (String) Serializer.deserialize(buf, STR_DESCR);

		if (buf.isReadable())
		{
			//noinspection unchecked
			admins = (Set<GxsId>) Serializer.deserialize(buf, SET_GXS_ID);
			//noinspection unchecked
			pinnedPosts = (Set<MessageId>) Serializer.deserialize(buf, SET_GXS_MSG_ID);
		}
		else
		{
			oldVersion = true;
		}
	}

	@Override
	public ForumGroupItem clone()
	{
		return (ForumGroupItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "ForumGroupItem{" +
				"description='" + description + '\'' +
				", admins=" + admins +
				", pinnedPosts=" + pinnedPosts +
				", oldVersion=" + oldVersion +
				'}';
	}
}
