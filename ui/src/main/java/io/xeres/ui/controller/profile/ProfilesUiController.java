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

package io.xeres.ui.controller.profile;

import io.xeres.common.id.Id;
import io.xeres.common.pgp.Trust;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.model.profile.Profile;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@FxmlView(value = "/view/profile/profiles.fxml")
public class ProfilesUiController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(ProfilesUiController.class);

	private final ProfileClient profileClient;

	@FXML
	private TableView<Profile> profilesTableView;

	@FXML
	private TableColumn<Profile, String> tableName;

	@FXML
	private TableColumn<Profile, String> tableIdentifier;

	@FXML
	private TableColumn<Profile, Boolean> tableAccepted;

	@FXML
	private TableColumn<Profile, Trust> tableTrust;

	public ProfilesUiController(ProfileClient profileClient)
	{
		this.profileClient = profileClient;
	}

	public void initialize()
	{
		tableName.setCellValueFactory(new PropertyValueFactory<>("name"));
		tableIdentifier.setCellValueFactory(param -> new SimpleStringProperty(Id.toString(param.getValue().getPgpIdentifier())));
		tableAccepted.setCellValueFactory(new PropertyValueFactory<>("accepted"));
		tableTrust.setCellValueFactory(new PropertyValueFactory<>("trust"));

		profileClient.getProfiles().collectList()
				.doOnSuccess(profiles -> Platform.runLater(() -> profilesTableView.getItems().addAll(profiles)))
				.doOnError(throwable -> log.error("Error while getting the profiles: {}", throwable.getMessage(), throwable))
				.subscribe();
	}
}
