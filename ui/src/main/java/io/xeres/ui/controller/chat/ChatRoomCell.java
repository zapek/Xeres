/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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
import io.xeres.ui.client.ChatClient;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.application.Platform;
import javafx.scene.control.TreeCell;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class ChatRoomCell extends TreeCell<RoomHolder>
{
	private static final ResourceBundle bundle = I18nUtils.getBundle();

	private final ChatClient chatClient;

	public ChatRoomCell(ChatClient chatClient)
	{
		super();
		this.chatClient = chatClient;
	}

	@Override
	protected void updateItem(RoomHolder item, boolean empty)
	{
		super.updateItem(item, empty);
		if (empty)
		{
			setText(null);
			setStyle("");
			TooltipUtils.uninstall(this);
		}
		else
		{
			setText(item.getRoomInfo().getName());
			if (item.getRoomInfo().hasNewMessages())
			{
				if (item.getRoomInfo().getRoomType() == RoomType.PRIVATE)
				{
					setStyle("-fx-text-fill: teal; -fx-font-weight: bold;");
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
					setStyle("-fx-text-fill: teal;");
				}
				else
				{
					setStyle("");
				}
			}
			if (item.getRoomInfo().isReal())
			{
				TooltipUtils.install(this,
						delayedTooltip -> {
							var roomInfo = getItem().getRoomInfo();
							if (roomInfo.getId() == 0)
							{
								return;
							}
							chatClient.getChatRoom(roomInfo.getId())
									.doOnSuccess(chatRoomInfo -> Platform.runLater(() -> {
										assert chatRoomInfo != null;
										delayedTooltip.show(MessageFormat.format(bundle.getString("chat.room.info"),
												chatRoomInfo.getName(),
												(StringUtils.isNotBlank(chatRoomInfo.getTopic()) ? chatRoomInfo.getTopic() : bundle.getString("chat.room.none")),
												chatRoomInfo.getCount(),
												String.join(", ", chatRoomInfo.getRoomType() == RoomType.PRIVATE ? bundle.getString("chat.room.private") : bundle.getString("chat.room.public"), chatRoomInfo.isSigned() ? bundle.getString("chat.room.signed-only") : bundle.getString("chat.room.anonymous-allowed")),
												Id.toString(chatRoomInfo.getId())));
									}))
									.subscribe();
						});
			}
			else
			{
				TooltipUtils.uninstall(this);
			}
		}
	}
}
