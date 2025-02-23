/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

import io.xeres.ui.client.ChatClient;
import io.xeres.ui.client.ConnectionClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.model.location.Location;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@FxmlView(value = "/view/chat/chatroom_invite.fxml")
public class ChatRoomInvitationWindowController implements WindowController
{
	@FXML
	private TreeView<PeerHolder> peersTree;

	@FXML
	private Button inviteButton;

	@FXML
	private Button cancelButton;

	private final ConnectionClient connectionClient;
	private final ChatClient chatClient;

	private final Set<CheckBoxTreeItem<PeerHolder>> invitedItems = new HashSet<>();
	private long chatRoomId;

	public ChatRoomInvitationWindowController(ConnectionClient connectionClient, ChatClient chatClient)
	{
		this.connectionClient = connectionClient;
		this.chatClient = chatClient;
	}

	@Override
	public void initialize()
	{
		var root = new CheckBoxTreeItem<>(new PeerHolder());
		root.setExpanded(true);
		root.addEventHandler(
				CheckBoxTreeItem.checkBoxSelectionChangedEvent(),
				(CheckBoxTreeItem.TreeModificationEvent<PeerHolder> e) -> {
					var item = e.getTreeItem();
					if (item.isLeaf())
					{
						checkInvite(item);
					}
				});

		peersTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); // XXX: needed?
		peersTree.setRoot(root);
		peersTree.setShowRoot(false);
		peersTree.setCellFactory(CheckBoxTreeCell.forTreeView());

		connectionClient.getConnectedProfiles().collectList()
				.doOnSuccess(profiles -> Platform.runLater(() -> profiles.forEach(profile -> {
					if (profile.getLocations().size() == 1)
					{
						root.getChildren().add(new CheckBoxTreeItem<>(new PeerHolder(profile, profile.getLocations().getFirst())));
					}
					else
					{
						var parent = new CheckBoxTreeItem<>(new PeerHolder(profile));
						parent.setExpanded(true);
						root.getChildren().add(parent);
						profile.getLocations().stream()
								.filter(Location::isConnected)
								.forEach(location -> parent.getChildren().add(new CheckBoxTreeItem<>(new PeerHolder(profile, location))));
					}
				})))
				.doAfterTerminate(() -> root.setSelected(false))
				.doOnError(UiUtils::showAlertError)
				.subscribe();

		inviteButton.setOnAction(this::invitePeers);
		cancelButton.setOnAction(UiUtils::closeWindow);

		Platform.runLater(this::handleArgument);
	}

	private void handleArgument()
	{
		var userData = UiUtils.getUserData(inviteButton);
		if (userData != null)
		{
			chatRoomId = (long) userData;
		}
	}

	private void checkInvite(CheckBoxTreeItem<PeerHolder> item)
	{
		if (item.isSelected())
		{
			invitedItems.add(item);
		}
		else
		{
			invitedItems.remove(item);
		}
		inviteButton.setDisable(invitedItems.isEmpty());
	}

	private void invitePeers(ActionEvent event)
	{
		var selectedLocations = invitedItems.stream()
				.map(peerHolderTreeItem -> peerHolderTreeItem.getValue().getLocation())
				.collect(Collectors.toSet());

		invitedItems.clear();
		peersTree.setRoot(null);

		chatClient.inviteLocationsToChatRoom(chatRoomId, selectedLocations)
				.subscribe();

		UiUtils.closeWindow(event);
	}
}
