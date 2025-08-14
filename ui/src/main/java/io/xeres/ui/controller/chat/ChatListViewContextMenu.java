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

package io.xeres.ui.controller.chat;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import java.util.Optional;

class ChatListViewContextMenu
{
	private static final String CLEAR_HISTORY_MENU_ID = "clearHistory";
	private static final String COPY_SELECTION_MENU_ID = "copySelection";

	private final ContextMenu contextMenu;

	public ChatListViewContextMenu()
	{
		contextMenu = new ContextMenu();
	}

	public void show(Node anchor, double screenX, double screenY)
	{
		contextMenu.show(anchor, screenX, screenY);
	}

	public void hide()
	{
		contextMenu.hide();
	}

	public void installSelectionMenu(EventHandler<ActionEvent> eventHandler)
	{
		if (findMenuEntry(COPY_SELECTION_MENU_ID).isPresent())
		{
			return;
		}

		var copySelectionItem = new MenuItem("Copy selection");
		copySelectionItem.setId(COPY_SELECTION_MENU_ID);
		copySelectionItem.setOnAction(eventHandler);

		contextMenu.getItems().addFirst(copySelectionItem);
	}

	public void removeSelectionMenu()
	{
		removeMenuEntry(COPY_SELECTION_MENU_ID);
	}

	public void installClearHistoryMenu(EventHandler<ActionEvent> eventHandler)
	{
		if (findMenuEntry(CLEAR_HISTORY_MENU_ID).isPresent())
		{
			return;
		}

		var clearItem = new MenuItem("Clear chat history");
		clearItem.setId(CLEAR_HISTORY_MENU_ID);
		clearItem.setOnAction(eventHandler);

		contextMenu.getItems().addAll(clearItem);
	}

	public void removeClearHistoryMenu()
	{
		removeMenuEntry(CLEAR_HISTORY_MENU_ID);
	}

	private void removeMenuEntry(String id)
	{
		findMenuEntry(id).ifPresent(menuItem -> contextMenu.getItems().remove(menuItem));
	}

	private Optional<MenuItem> findMenuEntry(String id)
	{
		return contextMenu.getItems().stream()
				.filter(menuItem -> menuItem.getId().equals(id))
				.findFirst();
	}
}
