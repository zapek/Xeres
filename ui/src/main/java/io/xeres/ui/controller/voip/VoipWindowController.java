/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.voip;

import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.message.voip.VoipAction;
import io.xeres.common.message.voip.VoipMessage;
import io.xeres.ui.client.message.MessageClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.util.UiUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@FxmlView(value = "/view/voip/voip.fxml")
public class VoipWindowController implements WindowController
{
	public record Parameters(String destination, VoipMessage message)
	{
	}

	private enum Status
	{
		INCOMING_CALL,
		OUTGOING_CALL,
		IN_CALL,
		OFF
	}

	@FXML
	private Label nameLabel;

	@FXML
	private Label statusLabel;

	@FXML
	private Button answerButton;

	@FXML
	private Button rejectButton;

	private final MessageClient messageClient;

	private LocationIdentifier destinationIdentifier;
	private Status status;

	public VoipWindowController(MessageClient messageClient)
	{
		this.messageClient = messageClient;
	}

	@Override
	public void initialize()
	{
		answerButton.setOnAction(_ -> messageClient.sendToDestination(destinationIdentifier, new VoipMessage(VoipAction.ACKNOWLEDGE)));
		rejectButton.setOnAction(_ -> messageClient.sendToDestination(destinationIdentifier, new VoipMessage(VoipAction.CLOSE)));
	}

	// XXX: beware! destinationIdentifier has to be the same as the one in the voipMessage... what happens otherwise (eg. ourself initiating the window then a message for a different user comes at the same time, not possible normally)

	@Override
	public void onShown()
	{
		var userData = (Parameters) Objects.requireNonNull(UiUtils.getUserData(answerButton), "missing Parameters userdata");
		destinationIdentifier = LocationIdentifier.fromString(userData.destination);

		if (userData.message == null)
		{
			messageClient.sendToDestination(destinationIdentifier, new VoipMessage(VoipAction.RING));
			status = Status.OUTGOING_CALL;
		}
		else
		{
			// XXX: actually that part might not be needed if we design the window to always stay open during a call (which it should be... perhaps)
			status = Status.INCOMING_CALL;
		}
		updateState();
	}

	public void doAction(String identifier, VoipMessage voipMessage)
	{
		destinationIdentifier = LocationIdentifier.fromString(identifier); // XXX: normally we shouldn't receive an action with a wrong identifier but who knows...
		switch (voipMessage.getAction())
		{
			case RING -> status = Status.INCOMING_CALL;
			case ACKNOWLEDGE -> status = Status.IN_CALL;
			case CLOSE -> status = Status.OFF; // XXX: just close the window?
		}
		updateState();
	}

	private void updateState()
	{
		switch (status)
		{
			case INCOMING_CALL -> statusLabel.setText("Incoming call...");
			case OUTGOING_CALL -> statusLabel.setText("Calling...");
			case IN_CALL -> statusLabel.setText("In Call..."); //XXX: display a timer instead...
		}
	}
}
