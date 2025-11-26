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

package io.xeres.app.xrs.service.channel.item;

import io.netty.buffer.ByteBuf;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.TlvType;
import io.xeres.common.id.GxsId;
import io.xeres.common.util.ByteUnitUtils;
import jakarta.persistence.Entity;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.deserialize;
import static io.xeres.app.xrs.serialization.Serializer.serialize;
import static io.xeres.app.xrs.serialization.TlvType.STR_DESCR;

@Entity(name = "channel_group")
public class ChannelGroupItem extends GxsGroupItem
{
	private String description;

	private byte[] image;

	public ChannelGroupItem()
	{
		// Needed for JPA
	}

	public ChannelGroupItem(GxsId gxsId, String name)
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

	public boolean hasImage()
	{
		return image != null;
	}

	public byte[] getImage()
	{
		return image;
	}

	public void setImage(byte[] image)
	{
		if (ArrayUtils.isNotEmpty(image))
		{
			this.image = image;
		}
		else
		{
			this.image = null;
		}
	}

	@Override
	public int writeDataObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, STR_DESCR, description);
		if (hasImage())
		{
			size += serialize(buf, TlvType.IMAGE, image);
		}
		return size;
	}

	@Override
	public void readDataObject(ByteBuf buf)
	{
		description = (String) deserialize(buf, STR_DESCR);

		if (buf.isReadable())
		{
			setImage((byte[]) deserialize(buf, TlvType.IMAGE));
		}
	}

	@Override
	public ChannelGroupItem clone()
	{
		return (ChannelGroupItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "ChannelGroupItem{" +
				"description='" + description + '\'' +
				", image=" + (image != null ? ("yes, " + ByteUnitUtils.fromBytes(image.length)) : "no") +
				'}';
	}
}
