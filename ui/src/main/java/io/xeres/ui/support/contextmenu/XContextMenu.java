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

package io.xeres.ui.support.contextmenu;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.util.function.BiPredicate;

/**
 * @param <T>
 */

/**
 * This class simplifies context menu handling for the following classes:
 * <ul>
 *     <li>ListView</li>
 *     <li>TreeView</li>
 *     <li>TableView</li>
 *     <li>TreeTableView</li>
 * </ul>
 *
 * @param <T> the item of the *View class
 */
public class XContextMenu<T>
{
	private final ContextMenu contextMenu;
	private boolean showContextMenu = true;

	public XContextMenu(Control node, MenuItem... menuItems)
	{
		EventHandler<ActionEvent> action = event -> {
			var selectedMenuItem = (MenuItem) event.getTarget();

			var popup = selectedMenuItem.getParentPopup();
			if (popup != null)
			{
				doItemAction(event, selectedMenuItem, getItem(popup.getOwnerNode()));
			}
		};

		for (var menuItem : menuItems)
		{
			if (menuItem.getUserData() != null)
			{
				throw new IllegalStateException("The user data of MenuItem '" + menuItem.getText() + "' is already set");
			}
			menuItem.setUserData(menuItem.getOnAction());
			menuItem.setOnAction(action);
		}

		contextMenu = new ContextMenu(menuItems);
		node.setOnContextMenuRequested(event -> {
			// Using event.getSource() instead of the context menu itself (the default with setContextMenu())
			// allows to find out on which node the context menu was activated.
			// We need the following workarounds to allow closing the menu with the primary button
			contextMenu.setAutoHide(true); // Workaround #1
			contextMenu.show((Node) event.getSource(), event.getScreenX(), event.getScreenY());
			if (!showContextMenu)
			{
				contextMenu.hide(); // We hide immediately so that the menu is not shown (but we still fire the onShowing event as we need to know if the user wants to not show a context menu)
			}
			event.consume();
		});
		// Workaround #2
		node.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
			contextMenu.hide(); // Hide the context menu on ANY mouse click, otherwise SECONDARY clicking around would reuse the same menu
		});
	}

	/**
	 * Allows to manipulate the context menu, usually disabling menu items.
	 *
	 * @param onShowing return true to show the menu
	 */
	public void setOnShowing(BiPredicate<ContextMenu, T> onShowing)
	{
		// The even only contains the ContextMenu as source and target, which we already have,
		// so we need to find it again with getItem().
		contextMenu.setOnShowing(event -> showContextMenu = onShowing.test(contextMenu, getItem(contextMenu.getOwnerNode())));
	}

	private void doItemAction(ActionEvent event, MenuItem selectedMenuItem, T sourceItem)
	{
		@SuppressWarnings("unchecked") var onAction = (EventHandler<ActionEvent>) selectedMenuItem.getUserData();
		onAction.handle(event.copyFor(sourceItem, event.getTarget())); // The source is set to the item it was activated upon (for example a listview's item and not the listview itself)
	}

	private T getItem(Node ownerNode)
	{
		if (ownerNode instanceof TreeView<?> treeView)
		{
			@SuppressWarnings("unchecked") var treeItem = (TreeItem<T>) treeView.getSelectionModel().getSelectedItem();
			return treeItem.getValue();
		}
		else if (ownerNode instanceof TableView<?> tableView)
		{
			@SuppressWarnings("unchecked") var tableItem = (T) tableView.getSelectionModel().getSelectedItem();
			return tableItem;
		}
		else if (ownerNode instanceof TreeTableView<?> treeTableView)
		{
			@SuppressWarnings("unchecked") var treeTableItem = (T) treeTableView.getSelectionModel().getSelectedItem();
			return treeTableItem;
		}
		else if (ownerNode instanceof ListView<?> listView)
		{
			@SuppressWarnings("unchecked") var listViewItem = (T) listView.getSelectionModel().getSelectedItem();
			return listViewItem;
		}
		else
		{
			throw new IllegalArgumentException("Unrecognized node in context menu creation: " + ownerNode);
		}
	}
}
