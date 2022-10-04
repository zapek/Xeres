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

import javax.persistence.Entity;
import java.util.Set;

@Entity(name = "forum_groups")
public class ForumGroupItem extends GxsGroupItem
{
	public ForumGroupItem()
	{
	}

	@Override
	public int writeGroupObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		return 0; // XXX
	}

	@Override
	public void readGroupObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		// XXX
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
