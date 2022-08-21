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

package io.xeres.ui.controller.chat;

import io.xeres.common.id.Id;
import io.xeres.common.message.chat.RoomType;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import org.apache.commons.lang3.StringUtils;

public class ChatRoomCell extends TreeCell<RoomHolder>
{
	private final TreeView<RoomHolder> treeView;

	public ChatRoomCell(TreeView<RoomHolder> treeView)
	{
		super();
		this.treeView = treeView;
		setContextMenu(createContextMenu(this));
		TooltipUtils.install(this,
				() -> {
					var roomInfo = this.getItem().getRoomInfo();
					if (roomInfo.getId() == 0)
					{
						return null;
					}
					return "Topic: " + (StringUtils.isNotBlank(roomInfo.getTopic()) ? roomInfo.getTopic() : "[none]") + "\n" +
							"Users: " + roomInfo.getCount() + "\n" +
							"Security: " + String.join(", ", roomInfo.getRoomType() == RoomType.PRIVATE ? "private" : "public", roomInfo.isSigned() ? "signed IDs only" : "anonymous IDs allowed") + "\n" +
							"ID: " + Id.toString(this.getItem().getRoomInfo().getId());
				}
				, null);
	}

	@Override
	protected void updateItem(RoomHolder item, boolean empty)
	{
		super.updateItem(item, empty);
		if (empty)
		{
			setText(null);
			setStyle("");
		}
		else
		{
			setText(item.getRoomInfo().getName());
			if (item.getRoomInfo().hasNewMessages())
			{
				if (item.getRoomInfo().getRoomType() == RoomType.PRIVATE)
				{
					setStyle("-fx-text-fill:red; -fx-font-weight: bold;");
				}
				else
				{
					setStyle("-fx-font-weight: bold;");
				}
			}
			else
			{
				if (item.getRoomInfo().getRoomType() == RoomType.PRIVATE)
				{
					setStyle("-fx-text-fill: red;");
				}
				else
				{
					setStyle("");
				}
			}
		}
	}

	private ContextMenu createContextMenu(TreeCell<RoomHolder> cell)
	{
		var contextMenu = new ContextMenu();

		var subscribeItem = new MenuItem("Join");
		subscribeItem.setOnAction(event -> treeView.fireEvent(new ChatRoomContextMenu(ChatRoomContextMenu.JOIN, cell.getTreeItem())));

		var unsubscribeItem = new MenuItem("Leave");
		unsubscribeItem.setOnAction(event -> treeView.fireEvent(new ChatRoomContextMenu(ChatRoomContextMenu.LEAVE, cell.getTreeItem())));

		contextMenu.getItems().addAll(subscribeItem, unsubscribeItem);
		return contextMenu;
	}
}
