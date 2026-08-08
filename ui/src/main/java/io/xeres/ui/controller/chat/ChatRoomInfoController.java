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
import io.xeres.common.message.chat.ChatRoomInfo;
import io.xeres.common.message.chat.RoomType;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.custom.DisclosedHyperlink;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import org.apache.commons.lang3.StringUtils;

import java.util.ResourceBundle;

import static io.xeres.ui.controller.help.HelpWindowController.SECTION_INTERFACE_CHATS;

public class ChatRoomInfoController implements Controller
{
	@FXML
	private StackPane emptyGroup;

	@FXML
	private DisclosedHyperlink helpLink;

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

	private WindowManager windowManager;
	private final ResourceBundle bundle = I18nUtils.getBundle();

	public void setWindowManager(WindowManager windowManager)
	{
		this.windowManager = windowManager;
	}

	@Override
	public void initialize()
	{
		helpLink.setOnAction(_ -> windowManager.openHelp(SECTION_INTERFACE_CHATS));

		// Clear the display first
		setRoomInfo(null);
	}

	public void setRoomInfo(ChatRoomInfo chatRoomInfo)
	{
		if (chatRoomInfo != null && chatRoomInfo.isReal())
		{
			UiUtils.setAbsent(emptyGroup);
			UiUtils.setPresent(roomGroup);
			roomName.setText(chatRoomInfo.getName());
			roomId.setText(Id.toString(chatRoomInfo.getId()));
			roomTopic.setText(StringUtils.isNotBlank(chatRoomInfo.getTopic()) ? chatRoomInfo.getTopic() : bundle.getString("chat.room.none"));
			roomSecurity.setText(String.join(", ", chatRoomInfo.getRoomType() == RoomType.PRIVATE ? bundle.getString("chat.room.private") : bundle.getString("chat.room.public"), chatRoomInfo.isSigned() ? bundle.getString("chat.room.signed-only") : bundle.getString("chat.room.anonymous-allowed")));
			roomCount.setText(String.valueOf(chatRoomInfo.getCount()));
		}
		else
		{
			UiUtils.setAbsent(roomGroup);
			UiUtils.setPresent(emptyGroup);
		}
	}
}
