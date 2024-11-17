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
import io.xeres.common.id.Id;
import io.xeres.common.message.chat.RoomType;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.scene.control.TreeCell;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class ChatRoomCell extends TreeCell<RoomHolder>
{
	private final ResourceBundle bundle = I18nUtils.getBundle();

	public ChatRoomCell()
	{
		super();
		TooltipUtils.install(this,
				() -> {
					var roomInfo = getItem().getRoomInfo();
					if (roomInfo.getId() == 0)
					{
						return null;
					}
					return MessageFormat.format(bundle.getString("chat.room.info"),
							(StringUtils.isNotBlank(roomInfo.getTopic()) ? roomInfo.getTopic() : bundle.getString("chat.room.none")),
							roomInfo.getCount(),
							String.join(", ", roomInfo.getRoomType() == RoomType.PRIVATE ? bundle.getString("chat.room.private") : bundle.getString("chat.room.public"), roomInfo.isSigned() ? bundle.getString("chat.room.signed-only") : bundle.getString("chat.room.anonymous-allowed")),
							Id.toString(getItem().getRoomInfo().getId()));
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
}
