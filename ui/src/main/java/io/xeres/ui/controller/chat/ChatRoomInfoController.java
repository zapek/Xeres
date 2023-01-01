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
import io.xeres.common.message.chat.ChatRoomInfo;
import io.xeres.common.message.chat.RoomType;
import io.xeres.ui.controller.Controller;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.apache.commons.lang3.StringUtils;

public class ChatRoomInfoController implements Controller
{
	@FXML
	private GridPane roomGroup;

	@FXML
	private Label roomName;

	@FXML
	private Label roomId;

	@FXML
	private Label roomTopic;

	@FXML
	private Label roomSecurity;

	@FXML
	private Label roomCount;

	@Override
	public void initialize()
	{
		// Nothing to do
	}

	public void setRoomInfo(ChatRoomInfo chatRoomInfo)
	{
		if (chatRoomInfo.isARealRoom())
		{
			roomGroup.setVisible(true);
			roomName.setText(chatRoomInfo.getName());
			roomId.setText(Id.toString(chatRoomInfo.getId()));
			roomTopic.setText(StringUtils.isNotBlank(chatRoomInfo.getTopic()) ? chatRoomInfo.getTopic() : I18nUtils.getString("chat.room.none"));
			roomSecurity.setText(String.join(", ", chatRoomInfo.getRoomType() == RoomType.PRIVATE ? I18nUtils.getString("chat.room.private") : I18nUtils.getString("chat.room.public"), chatRoomInfo.isSigned() ? I18nUtils.getString("chat.room.signed-only") : I18nUtils.getString("chat.room.anonymous-allowed")));
			roomCount.setText(String.valueOf(chatRoomInfo.getCount()));
		}
		else
		{
			roomGroup.setVisible(false);
		}
	}
}
