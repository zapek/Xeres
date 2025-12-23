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

package io.xeres.app.xrs.common;

import io.netty.buffer.ByteBuf;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.app.xrs.serialization.TlvType;
import io.xeres.common.id.GxsId;
import jakarta.persistence.Entity;

import java.util.Set;

@Entity(name = "comment_message")
public class CommentMessageItem extends GxsMessageItem
{
	public static final int SUBTYPE = 0xf1;

	private String comment;

	@Override
	public int getSubType()
	{
		return SUBTYPE;
	}

	public CommentMessageItem()
	{
		// Needed by JPA
	}

	public CommentMessageItem(GxsId gxsId, String name)
	{
		setGxsId(gxsId);
		setName(name);
		updatePublished();
	}

	public String getComment()
	{
		return comment;
	}

	public void setComment(String comment)
	{
		this.comment = comment;
	}

	@Override
	public int writeDataObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		return Serializer.serialize(buf, TlvType.STR_GXS_MESSAGE_COMMENT, comment);
	}

	@Override
	public void readDataObject(ByteBuf buf)
	{
		comment = (String) Serializer.deserialize(buf, TlvType.STR_GXS_MESSAGE_COMMENT);
	}

	@Override
	public CommentMessageItem clone()
	{
		return (CommentMessageItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "CommentMessageItem{" +
				"comment='" + comment + '\'' +
				'}';
	}
}
