/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.model.forum;

import io.xeres.common.id.GxsId;
import io.xeres.ui.controller.common.GxsGroup;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class ForumGroup implements GxsGroup
{
	private long id;
	private String name;
	private GxsId gxsId;
	private String description;
	private boolean subscribed;
	private boolean external;
	private final IntegerProperty unreadCount = new SimpleIntegerProperty(0);

	public ForumGroup()
	{
	}

	public ForumGroup(String name)
	{
		this.name = name;
	}

	@Override
	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	@Override
	public boolean isReal()
	{
		return id != 0L;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@Override
	public GxsId getGxsId()
	{
		return gxsId;
	}

	public void setGxsId(GxsId gxsId)
	{
		this.gxsId = gxsId;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	@Override
	public boolean isSubscribed()
	{
		return subscribed;
	}

	@Override
	public void setSubscribed(boolean subscribed)
	{
		this.subscribed = subscribed;
	}

	@Override
	public boolean isExternal()
	{
		return external;
	}

	public void setExternal(boolean external)
	{
		this.external = external;
	}

	@Override
	public boolean hasNewMessages()
	{
		return unreadCount.get() > 0 && gxsId != null;
	}

	public int getUnreadCount()
	{
		return unreadCount.get();
	}

	@Override
	public void setUnreadCount(int unreadCount)
	{
		this.unreadCount.set(unreadCount);
	}

	@Override
	public void addUnreadCount(int value)
	{
		unreadCount.set(unreadCount.get() + value);
	}

	@Override
	public void subtractUnreadCount(int value)
	{
		unreadCount.set(unreadCount.get() - value);
	}

	public IntegerProperty unreadCountProperty()
	{
		return unreadCount;
	}

	@Override
	public String toString()
	{
		return getName();
	}
}
