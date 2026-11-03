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

package io.xeres.ui.controller.mail;

import io.xeres.common.id.GxsId;
import io.xeres.common.mail.MailType;
import io.xeres.ui.controller.common.GxsGroup;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.Instant;

public class MailFolder implements GxsGroup
{
	private final MailType type;
	private final SimpleObjectProperty<FontIcon> icon;
	private final SimpleIntegerProperty unreadCount;

	public MailFolder(MailType type, FontIcon icon)
	{
		this.type = type;
		this.icon = new SimpleObjectProperty<>(icon);
		unreadCount = new SimpleIntegerProperty(0);
	}

	@Override
	public boolean isReal()
	{
		return false;
	}

	@Override
	public long getId()
	{
		return type.ordinal();
	}

	@Override
	public GxsId getGxsId()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return type.toString();
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
		return true;
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

	public MailType getType()
	{
		return type;
	}

	public FontIcon getIcon()
	{
		return icon.get();
	}

	public SimpleObjectProperty<FontIcon> iconProperty()
	{
		return icon;
	}

	public void setIcon(FontIcon icon)
	{
		this.icon.set(icon);
	}

	public int getUnreadCount()
	{
		return unreadCount.get();
	}

	public SimpleIntegerProperty unreadCountProperty()
	{
		return unreadCount;
	}

	@Override
	public void setUnreadCount(int unreadCount)
	{
		this.unreadCount.set(unreadCount);
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
