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
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.xrs.serialization.FieldSize;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.common.id.GxsId;
import jakarta.persistence.Entity;

import java.util.Set;

@Entity(name = "gxs_vote")
public class VoteMessageItem extends GxsGroupItem
{
	public enum Type
	{
		/**
		 * Unset vote?
		 */
		NONE,
		/**
		 * Negative vote.
		 */
		DOWN,
		/**
		 * Positive vote.
		 */
		UP
	}

	private Set<Type> type;

	public VoteMessageItem()
	{
		// Needed by JPA
	}

	public VoteMessageItem(GxsId gxsId, String name)
	{
		setGxsId(gxsId);
		setName(name);
		updatePublished();
	}

	@Override
	public int getSubType()
	{
		return 0xf2;
	}

	public Set<Type> getType()
	{
		return type;
	}

	public void setType(Set<Type> type)
	{
		this.type = type;
	}

	@Override
	public int writeDataObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		return Serializer.serialize(buf, type, FieldSize.INTEGER);
	}

	@Override
	public void readDataObject(ByteBuf buf)
	{
		type = Serializer.deserializeEnumSet(buf, Type.class, FieldSize.INTEGER);
	}

	@Override
	public VoteMessageItem clone()
	{
		return (VoteMessageItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "VoteMessageItem{" +
				"type=" + type +
				'}';
	}
}
