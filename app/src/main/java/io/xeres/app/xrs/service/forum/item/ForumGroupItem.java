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

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.serialize;
import static io.xeres.app.xrs.serialization.TlvType.SET_GXS_ID;
import static io.xeres.app.xrs.serialization.TlvType.SET_GXS_MSG_ID;

@Entity(name = "forum_groups")
public class ForumGroupItem extends GxsGroupItem
{
	private String description;

	@ElementCollection
	private Set<GxsId> adminList;

	@ElementCollection
	private Set<MessageId> pinnedPosts;

	public ForumGroupItem()
	{
		// Needed for JPA
	}

	@Override
	public int writeGroupObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, (byte) 2);
		size += serialize(buf, (short) RsServiceType.FORUMS.getType());
		size += serialize(buf, (byte) 2);
		var sizeOffset = buf.writerIndex();
		size += serialize(buf, 0); // write size at end

		size += serialize(buf, description);
		size += serialize(buf, SET_GXS_ID, adminList);
		size += serialize(buf, SET_GXS_MSG_ID, pinnedPosts);

		buf.setInt(sizeOffset, size); // write total size

		return size;
	}

	@Override
	public void readGroupObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		// XXX: we have to read the following but... shouldn't there be something else to do it?
		buf.readByte(); // 0x2 (packet version)
		buf.readShort(); // 0x0211 (service)
		buf.readByte(); // 0x2 (packet subtype?)
		buf.readInt(); // size

		description = Serializer.deserializeString(buf);
		//noinspection unchecked
		adminList = (Set<GxsId>) Serializer.deserialize(buf, SET_GXS_ID);
		//noinspection unchecked
		pinnedPosts = (Set<MessageId>) Serializer.deserialize(buf, SET_GXS_MSG_ID);
	}

	@Override
	public int writeObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;
		size += writeMetaObject(buf, serializationFlags);
		size += writeGroupObject(buf, serializationFlags);
		return size;
	}

	@Override
	public void readObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		readMetaObject(buf, serializationFlags);
		readGroupObject(buf, serializationFlags);
	}
}
