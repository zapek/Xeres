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

package io.xeres.ui.controller.profile;

import io.xeres.ui.model.profile.Profile;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.control.TableView;

public class ProfileContextMenu extends Event
{
	public static final EventType<ProfileContextMenu> ALL = new EventType<>("PROFILE_CONTEXT_MENU_ALL");
	public static final EventType<ProfileContextMenu> DELETE = new EventType<>("PROFILE_CONTEXT_MENU_DELETE");

	private final TableView<Profile> tableView;

	public ProfileContextMenu(EventType<ProfileContextMenu> eventType, TableView<Profile> tableView)
	{
		super(eventType);
		this.tableView = tableView;
	}

	public TableView<Profile> getTableView()
	{
		return tableView;
	}
}
