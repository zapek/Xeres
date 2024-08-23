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

package io.xeres.ui.controller.messaging;

import io.xeres.common.message.chat.ChatMessage;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.chat.ChatCommand;
import io.xeres.ui.support.util.UiUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

@Component
@FxmlView(value = "/view/messaging/broadcast.fxml")
public class BroadcastWindowController implements WindowController
{
	@FXML
	private Button send;

	@FXML
	private Button cancel;

	@FXML
	private TextArea textArea;

	private final MessageClient messageClient;

	public BroadcastWindowController(MessageClient messageClient)
	{
		this.messageClient = messageClient;
	}

	@Override
	public void initialize()
	{
		send.setOnAction(event ->
		{
			var message = new ChatMessage(ChatCommand.parseCommands(textArea.getText()));
			messageClient.sendBroadcast(message);
			cancel.fire();
		});

		textArea.textProperty().addListener(observable -> send.setDisable(textArea.getText().isBlank()));
		cancel.setOnAction(UiUtils::closeWindow);
	}
}
