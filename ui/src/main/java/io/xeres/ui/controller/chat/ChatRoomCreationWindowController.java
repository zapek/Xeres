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

import io.xeres.common.rest.chat.ChatRoomVisibility;
import io.xeres.ui.client.ChatClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

@Component
@FxmlView(value = "/view/chat/chatroom_create.fxml")
public class ChatRoomCreationWindowController implements WindowController
{
	@FXML
	private Button createButton;

	@FXML
	private Button cancelButton;

	@FXML
	private TextField roomName;

	@FXML
	private TextField topic;

	@FXML
	private ChoiceBox<String> visibility;

	@FXML
	private CheckBox security;

	private final ChatClient chatClient;

	public ChatRoomCreationWindowController(ChatClient chatClient)
	{
		this.chatClient = chatClient;
	}

	@Override
	public void initialize()
	{
		roomName.textProperty().addListener(observable -> createButton.setDisable(roomName.getText().isBlank()));
		topic.textProperty().addListener(observable -> createButton.setDisable(topic.getText().isBlank()));

		visibility.setItems(FXCollections.observableArrayList("Public", "Private"));
		visibility.getSelectionModel().select(0);

		createButton.setOnAction(event -> chatClient.createChatRoom(roomName.getText(),
						topic.getText(),
						ChatRoomVisibility.fromSelection(visibility.getSelectionModel().getSelectedIndex()),
						security.isSelected())
				.doOnSuccess(aVoid -> Platform.runLater(() -> UiUtils.closeWindow(roomName)))
				.subscribe());
		cancelButton.setOnAction(UiUtils::closeWindow);
	}
}
