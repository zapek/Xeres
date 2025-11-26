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
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.xrs.common.FileItem;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.TlvType;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import io.xeres.common.util.ByteUnitUtils;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.*;
import static io.xeres.app.xrs.serialization.TlvType.*;

@Entity(name = "channel_message")
public class ChannelMessageItem extends GxsMessageItem
{
	private String content;

	@ElementCollection
	private List<FileItem> files;

	private String title;

	private String comment;

	private byte[] image;

	private boolean read;

	public ChannelMessageItem()
	{
		// Needed for JPA
	}

	public ChannelMessageItem(GxsId groupId, MessageId messageId, String name)
	{
		setGxsId(groupId);
		setMessageId(messageId);
		setName(name);
		updatePublished();
	}

	@Override
	public int getSubType()
	{
		return 3;
	}

	public String getContent()
	{
		return content;
	}

	public void setContent(String content)
	{
		this.content = content;
	}

	public List<FileItem> getFiles()
	{
		return files;
	}

	public void setFiles(List<FileItem> files)
	{
		this.files = files;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getComment()
	{
		return comment;
	}

	public void setComment(String comment)
	{
		this.comment = comment;
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

	public boolean isRead()
	{
		return read;
	}

	public void setRead(boolean read)
	{
		this.read = read;
	}

	@Override
	public int writeDataObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, STR_MSG, content);
		//noinspection unchecked
		size += serialize(buf, (List<Object>) (List<?>) files, FILE_ITEM);
		if (StringUtils.isNotEmpty(title))
		{
			size += serialize(buf, STR_TITLE, title);
		}
		if (StringUtils.isNotEmpty(comment))
		{
			size += serialize(buf, STR_COMMENT, comment);
		}

		if (hasImage())
		{
			size += serialize(buf, TlvType.IMAGE, image);
		}

		return size;
	}

	@Override
	public void readDataObject(ByteBuf buf)
	{
		content = (String) deserialize(buf, STR_MSG);

		//noinspection unchecked
		files = (List<FileItem>) (List<?>) deserializeList(buf, FILE_ITEM);

		if (buf.isReadable())
		{
			title = (String) deserialize(buf, STR_TITLE);
		}
		if (buf.isReadable())
		{
			comment = (String) deserialize(buf, STR_COMMENT);
		}

		if (buf.isReadable())
		{
			setImage((byte[]) deserialize(buf, TlvType.IMAGE));
		}
	}

	@Override
	public ChannelMessageItem clone()
	{
		return (ChannelMessageItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "ChannelMessageItem{" +
				"content='" + content + '\'' +
				", image=" + (image != null ? ("yes, " + ByteUnitUtils.fromBytes(image.length)) : "no") +
				", read=" + read +
				'}';
	}
}
