/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.common;

import io.xeres.common.id.GxsId;

public interface GxsGroup
{
	/**
	 * Checks if it's a real Gxs Group, that means not a tree directory.
	 *
	 * @return true if it's a real gxs group
	 */
	boolean isReal();

	long getId();

	GxsId getGxsId();

	String getName();

	String getDescription();

	/**
	 * Checks if the group comes from other people than us.
	 * @return true if it's an external group, that is now a group created by us
	 */
	boolean isExternal();

	boolean isSubscribed();

	void setSubscribed(boolean subscribed);

	boolean hasNewMessages();

	void setUnreadCount(int unreadCount);

	void addUnreadCount(int value);

	void subtractUnreadCount(int value);
}
