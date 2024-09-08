/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.text.MessageFormat;

public class ChatUserCell extends ListCell<ChatRoomUser>
{
	private static final int DEFAULT_AVATAR_SIZE = 32;

	public ChatUserCell()
	{
		super();
		TooltipUtils.install(this,
				() -> MessageFormat.format(I18nUtils.getString("chat.room.user-info"), super.getItem().nickname(), super.getItem().gxsId()),
				() -> super.getItem().image());
	}

	@Override
	protected void updateItem(ChatRoomUser item, boolean empty)
	{
		super.updateItem(item, empty);
		setText(empty ? null : item.nickname());
		setGraphic(empty ? null : getAvatar(item));
	}

	private static Node getAvatar(ChatRoomUser item)
	{
		if (item.image() != null)
		{
			var image = new ImageView(item.image().getImage());
			image.setFitWidth(DEFAULT_AVATAR_SIZE);
			image.setFitHeight(DEFAULT_AVATAR_SIZE);
			return image;
		}
		else
		{
			var font = new FontIcon(FontAwesomeSolid.USER);
			var pane = new StackPane(font);
			pane.setPrefWidth(DEFAULT_AVATAR_SIZE);
			pane.setPrefHeight(DEFAULT_AVATAR_SIZE);
			pane.setAlignment(Pos.CENTER);
			return pane;
		}
	}
}
