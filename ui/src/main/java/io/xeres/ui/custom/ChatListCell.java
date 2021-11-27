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

package io.xeres.ui.custom;

import io.xeres.ui.controller.chat.ChatLine;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class ChatListCell extends ListCell<ChatLine>
{
	public ChatListCell(ListView<ChatLine> listView)
	{
		super();
		setPrefWidth(0);
	}

	@Override
	protected void updateItem(ChatLine item, boolean empty)
	{
		super.updateItem(item, empty);
		if (!empty)
		{
			setGraphic(createChatListCell(item));
		}
	}

	private static Node createChatListCell(ChatLine item)
	{
		if (item.hasImage())
		{
			return new HBox(createLabel(item.getText()), new ImageView(item.getImage().getImage())); // XXX: the double getImage() is ugly...
		}
		else
		{
			return createLabel(item.getText());
		}
	}

	private static Label createLabel(String line)
	{
		Label label = new Label(line);
		label.setWrapText(true);
		return label;
	}
}
