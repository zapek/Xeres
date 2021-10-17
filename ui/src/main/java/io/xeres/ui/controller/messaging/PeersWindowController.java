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

package io.xeres.ui.controller.messaging;

import io.xeres.ui.client.ConnectionClient;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@FxmlView(value = "/view/messaging/peers.fxml")
public class PeersWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(PeersWindowController.class);

	@FXML
	private Label nickname;

	@FXML
	private TreeView<PeerHolder> peersTree;

	private final ProfileClient profileClient;
	private final ConnectionClient connectionClient;
	private final WindowManager windowManager;

	public PeersWindowController(ProfileClient profileClient, ConnectionClient connectionClient, WindowManager windowManager)
	{
		this.profileClient = profileClient;
		this.connectionClient = connectionClient;
		this.windowManager = windowManager;
	}

	@Override
	public void initialize()
	{
		TreeItem<PeerHolder> root = new TreeItem<>(new PeerHolder());
		root.setExpanded(true);
		peersTree.setRoot(root);
		peersTree.setShowRoot(false);

		peersTree.setCellFactory(param ->
		{
			TreeCell<PeerHolder> cell = new TreeCell<>()
			{
				@Override
				protected void updateItem(PeerHolder profileHolder, boolean empty)
				{
					super.updateItem(profileHolder, empty);
					if (empty)
					{
						setText(null);
					}
					else
					{
						setText(profileHolder.getProfile().getName()); // XXX: add some logic for leaves, etc...
					}
				}
			};
			// XXX: add context menu here, maybe
			return cell;
		});

		peersTree.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) // XXX: add another condition to make sure we're double clicking on a leaf
			{
				var profileHolder = peersTree.getSelectionModel().getSelectedItem().getValue();
				if (profileHolder.hasLocation())
				{
					windowManager.openMessaging(profileHolder.getLocation().getLocationId().toString(), null);
				}
			}
		});

		profileClient.getOwnProfile()
				.doOnSuccess(profile -> Platform.runLater(() -> nickname.setText(profile.getName())))
				.subscribe();

		connectionClient.getConnectedProfiles().collectList()
				.doOnSuccess(profiles -> Platform.runLater(() -> profiles.forEach(profile -> {
					if (profile.getLocations().size() == 1)
					{
						root.getChildren().add(new TreeItem<>(new PeerHolder(profile, profile.getLocations().get(0))));
					}
					else
					{
						var parent = new TreeItem<>(new PeerHolder(profile));
						root.getChildren().add(parent);
						profile.getLocations().forEach(location -> parent.getChildren().add(new TreeItem<>(new PeerHolder(profile, location))));
					}
				})))
				.doOnError(throwable -> log.error("Error while getting profiles: {}", throwable.getMessage(), throwable))
				.subscribe();

		// XXX: here lies a good example of a connection that should stay open to get refreshed... ponder how to do it
	}
}
