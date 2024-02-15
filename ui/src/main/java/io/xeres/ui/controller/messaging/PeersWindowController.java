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

import io.xeres.ui.client.ConnectionClient;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.model.location.Location;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.util.ResourceBundle;

@Component
@FxmlView(value = "/view/messaging/peers.fxml")
public class PeersWindowController implements WindowController
{
	@FXML
	private Label nickname;

	@FXML
	private TreeView<PeerHolder> peersTree;

	private final ProfileClient profileClient;
	private final ConnectionClient connectionClient;
	private final WindowManager windowManager;
	private final ResourceBundle bundle;

	public PeersWindowController(ProfileClient profileClient, ConnectionClient connectionClient, WindowManager windowManager, ResourceBundle bundle)
	{
		this.profileClient = profileClient;
		this.connectionClient = connectionClient;
		this.windowManager = windowManager;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		var root = new TreeItem<>(new PeerHolder());
		root.setExpanded(true);
		peersTree.setRoot(root);
		peersTree.setShowRoot(false);

		peersTree.setCellFactory(param -> new PeerCell());
		createPeersTreeContextMenu();

		peersTree.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2)
			{
				var selectedItem = peersTree.getSelectionModel().getSelectedItem();
				if (selectedItem != null)
				{
					var peerHolder = selectedItem.getValue();
					if (peerHolder.hasLocation())
					{
						directMessage(peerHolder);
					}
				}
			}
		});

		profileClient.getOwn()
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
						profile.getLocations().stream()
								.filter(Location::isConnected)
								.forEach(location -> parent.getChildren().add(new TreeItem<>(new PeerHolder(profile, location))));
					}
				})))
				.doOnError(UiUtils::showAlertError)
				.subscribe();

		// XXX: here lies a good example of a connection that should stay open to get refreshed... ponder how to do it
	}

	private void createPeersTreeContextMenu()
	{
		var directMessage = new MenuItem(bundle.getString("peers.direct-message"));
		directMessage.setOnAction(event -> directMessage(((PeerHolder) event.getSource())));

		new XContextMenu<PeerHolder>(peersTree, directMessage);
	}

	private void directMessage(PeerHolder peerHolder)
	{
		if (peerHolder.hasLocation())
		{
			windowManager.openMessaging(peerHolder.getLocation().getLocationId().toString(), null);
		}
	}
}
