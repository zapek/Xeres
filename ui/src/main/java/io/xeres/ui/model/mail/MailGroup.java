/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.model.mail;

import io.xeres.common.id.GxsId;
import io.xeres.ui.controller.common.GxsGroup;

import java.time.Instant;

/// This class is not really a GxsGroup but the interface is implemented for convenience (UI).
public class MailGroup implements GxsGroup
{
	public MailGroup()
	{
	}

	@Override
	public boolean isReal()
	{
		return false;
	}

	@Override
	public long getId()
	{
		return 0;
	}

	@Override
	public GxsId getGxsId()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return "";
	}

	@Override
	public String getDescription()
	{
		return "";
	}

	@Override
	public boolean isExternal()
	{
		return false;
	}

	@Override
	public int getVisibleMessageCount()
	{
		return 0;
	}

	@Override
	public Instant getLastActivity()
	{
		return null;
	}

	@Override
	public boolean isSubscribed()
	{
		return false;
	}

	@Override
	public void setSubscribed(boolean subscribed)
	{

	}

	@Override
	public boolean hasNewMessages()
	{
		return false;
	}

	@Override
	public void setUnreadCount(int unreadCount)
	{

	}

	@Override
	public void addUnreadCount(int value)
	{

	}

	@Override
	public void subtractUnreadCount(int value)
	{

	}
}
