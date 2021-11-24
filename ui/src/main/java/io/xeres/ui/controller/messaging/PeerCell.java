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

package io.xeres.ui.controller.messaging;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;

public class PeerCell extends TreeCell<PeerHolder>
{
	private final TreeView<PeerHolder> treeView;

	public PeerCell(TreeView<PeerHolder> treeView)
	{
		super();
		this.treeView = treeView;
		setContextMenu(createContextMenu(this));
	}

	@Override
	protected void updateItem(PeerHolder item, boolean empty)
	{
		super.updateItem(item, empty);
		setText(empty ? null : item.getProfile().getName()); // XXX: add some logic for children (leaves), etc...
	}

	private ContextMenu createContextMenu(TreeCell<PeerHolder> cell)
	{
		var contextMenu = new ContextMenu();

		var directMessage = new MenuItem("Direct message");
		directMessage.setOnAction(event -> treeView.fireEvent(new PeerContextMenu(PeerContextMenu.DIRECT_MESSAGE, cell.getTreeItem())));

		contextMenu.getItems().addAll(directMessage);
		return contextMenu;
	}
}
