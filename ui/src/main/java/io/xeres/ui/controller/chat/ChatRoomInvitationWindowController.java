/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

import io.xeres.ui.client.ConnectionClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.controller.messaging.PeerCell;
import io.xeres.ui.controller.messaging.PeerHolder;
import io.xeres.ui.model.location.Location;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@FxmlView(value = "/view/chat/chatroom_invite.fxml")
public class ChatRoomInvitationWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(ChatRoomInvitationWindowController.class);

	@FXML
	private TreeView<PeerHolder> peersTree;

	@FXML
	private Button inviteButton;

	@FXML
	private Button cancelButton;

	private final ConnectionClient connectionClient;

	public ChatRoomInvitationWindowController(ConnectionClient connectionClient)
	{
		this.connectionClient = connectionClient;
	}

	@Override
	public void initialize() throws IOException
	{
		// XXX: needs to know to WHICH chatroom we're inviting to

		var root = new TreeItem<>(new PeerHolder());
		root.setExpanded(true);
		peersTree.setRoot(root);
		peersTree.setShowRoot(false);

		peersTree.setCellFactory(PeerCell::new); // XXX: needs a different one

		connectionClient.getConnectedProfiles().collectList()
				.doOnSuccess(profiles -> Platform.runLater(() -> profiles.forEach(profile -> {
					if (profile.getLocations().size() == 1)
					{
						root.getChildren().add(new TreeItem<>(new PeerHolder(profile, profile.getLocations().get(0))));
					}
					else
					{
						var parent = new TreeItem<>(new PeerHolder(profile));
						parent.setExpanded(true);
						root.getChildren().add(parent);
						profile.getLocations().stream()
								.filter(Location::isConnected)
								.forEach(location -> parent.getChildren().add(new TreeItem<>(new PeerHolder(profile, location))));
					}
				})))
				.doOnError(throwable -> log.error("Error while getting profiles: {}", throwable.getMessage(), throwable))
				.subscribe();

		// XXX: send invitations on inviteButton
		cancelButton.setOnAction(UiUtils::closeWindow);
	}
}
