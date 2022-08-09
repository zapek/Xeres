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

package io.xeres.ui.controller.settings;

import io.xeres.ui.controller.WindowController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@FxmlView(value = "/view/settings/settings.fxml")
public class SettingsWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(SettingsWindowController.class);

	@FXML
	private ListView<SettingsGroup> listView;

	@FXML
	private AnchorPane content;

	@Override
	public void initialize()
	{
		listView.setCellFactory(SettingsCell::new);
		listView.getItems().addAll(new SettingsGroup("Networks", new ImageView("/image/settings_networks.png"), "/view/settings/settings_networks.fxml"),
				new SettingsGroup("Chat", new ImageView("/image/settings_chat.png"), null),
				new SettingsGroup("Identities", new ImageView("/image/settings_identities.png"), null),
				new SettingsGroup("Mail", new ImageView("/image/settings_mail.png"), null),
				new SettingsGroup("Transfers", new ImageView("/image/settings_transfer.png"), null));

		listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			content.getChildren().clear();
			if (newValue.fxmlView() != null)
			{

				var loader = new FXMLLoader(getClass().getResource(newValue.fxmlView()));
				Node view;
				try
				{
					view = loader.load();
				}
				catch (IOException e)
				{
					throw new IllegalArgumentException("Cannot load the view " + newValue.fxmlView(), e);
				}
				// XXX: do we need the controller? loader.getController()
				content.getChildren().add(view);
				AnchorPane.setTopAnchor(view, 0.0);
				AnchorPane.setBottomAnchor(view, 0.0);
				AnchorPane.setLeftAnchor(view, 0.0);
				AnchorPane.setRightAnchor(view, 0.0);
			}
		});

		listView.getSelectionModel().selectFirst();
	}
}
