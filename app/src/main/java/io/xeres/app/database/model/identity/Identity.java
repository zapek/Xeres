/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.database.model.identity;

import io.xeres.app.xrs.service.gxsid.item.GxsIdGroupItem;
import io.xeres.common.identity.Type;

import javax.persistence.*;

@Table(name = "identities")
@Entity
public class Identity
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "gxs_id")
	private GxsIdGroupItem gxsIdGroupItem;

	private Type type;

	protected Identity()
	{

	}

	protected Identity(GxsIdGroupItem gxsIdGroupItem, Type type)
	{
		this.gxsIdGroupItem = gxsIdGroupItem;
		setType(type);
	}

	public static Identity createOwnIdentity(GxsIdGroupItem gxsIdGroupItem, Type type)
	{
		if (type != Type.SIGNED && type != Type.ANONYMOUS)
		{
			throw new IllegalArgumentException("Wrong identity type");
		}
		return new Identity(gxsIdGroupItem, type);
	}

	public static Identity createFriendIdentity(GxsIdGroupItem gxsIdGroupItem)
	{
		return new Identity(gxsIdGroupItem, Type.FRIEND);
	}

	public static Identity createIdentity(GxsIdGroupItem gxsIdGroupItem)
	{
		return new Identity(gxsIdGroupItem, Type.OTHER);
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public Type getType()
	{
		return type;
	}

	public void setType(Type type)
	{
		this.type = type;
	}

	public GxsIdGroupItem getGxsIdGroupItem()
	{
		return gxsIdGroupItem;
	}

	/**
	 * Checks if an identity must be kept in the list of own identities and friends.
	 *
	 * @return true if important enough
	 */
	public boolean isNotable()
	{
		return type != Type.OTHER;
	}

	@Override
	public String toString()
	{
		return "Identity{" +
				"id=" + id +
				", gxsIdGroupItem=" + gxsIdGroupItem +
				", type=" + type +
				'}';
	}
}
