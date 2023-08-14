/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.forum;

import io.xeres.common.message.forum.ForumMessage;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.control.TableView;

import java.io.Serial;

public class ForumMessageContextMenu extends Event
{
	public static final EventType<ForumMessageContextMenu> ALL = new EventType<>("FORUM_MESSAGE_CONTEXT_MENU_ALL");
	public static final EventType<ForumMessageContextMenu> REPLY = new EventType<>("FORUM_MESSAGE_CONTEXT_MENU_REPLY");

	private final transient TableView<ForumMessage> tableView;

	@Serial
	private static final long serialVersionUID = -641225667890421501L;

	public ForumMessageContextMenu(EventType<ForumMessageContextMenu> eventType, TableView<ForumMessage> tableView)
	{
		super(eventType);
		this.tableView = tableView;
	}

	public TableView<ForumMessage> getTableView()
	{
		return tableView;
	}
}
