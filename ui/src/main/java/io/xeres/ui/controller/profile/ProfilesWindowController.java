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

package io.xeres.ui.controller.profile;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.id.Id;
import io.xeres.common.pgp.Trust;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.model.profile.Profile;
import io.xeres.ui.support.contextmenu.XContextMenu;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static io.xeres.common.dto.profile.ProfileConstants.OWN_PROFILE_ID;
import static javafx.scene.control.TableColumn.SortType.ASCENDING;
import static javafx.scene.control.TableColumn.SortType.DESCENDING;

@Component
@FxmlView(value = "/view/profile/profiles.fxml")
public class ProfilesWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(ProfilesWindowController.class);
	public static final String DELETE_MENU_ID = "delete";

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

	private XContextMenu<Profile> profileXContextMenu;

	public ProfilesWindowController(ProfileClient profileClient)
	{
		this.profileClient = profileClient;
	}

	@Override
	public void initialize()
	{
		profilesTableView.setRowFactory(ProfileCell::new);
		createProfilesTableViewContextMenu();

		tableName.setCellValueFactory(new PropertyValueFactory<>("name"));
		tableIdentifier.setCellValueFactory(param -> new SimpleStringProperty(Id.toString(param.getValue().getPgpIdentifier())));
		tableAccepted.setCellValueFactory(new PropertyValueFactory<>("accepted"));
		tableTrust.setCellValueFactory(new PropertyValueFactory<>("trust"));

		profileClient.findAll().collectList()
				.doOnSuccess(profiles -> Platform.runLater(() -> {
					// Add all profiles
					profilesTableView.getItems().addAll(profiles);

					// Sort by accepted and name
					profilesTableView.getSortOrder().add(tableAccepted);
					tableAccepted.setSortType(DESCENDING);
					tableAccepted.setSortable(true);
					profilesTableView.getSortOrder().add(tableName);
					tableName.setSortType(ASCENDING);
					tableName.setSortable(true);
				}))
				.doOnError(throwable -> log.error("Error while getting the profiles: {}", throwable.getMessage(), throwable))
				.subscribe();
	}

	private void createProfilesTableViewContextMenu()
	{
		var deleteItem = new MenuItem(I18nUtils.getString("profiles.delete"));
		deleteItem.setId(DELETE_MENU_ID);
		deleteItem.setOnAction(event -> {
			var profile = (Profile) event.getSource();
			if (profile.getId() != OWN_PROFILE_ID)
			{
				profileClient.delete(profile.getId())
						.doOnSuccess(unused -> Platform.runLater(() -> profilesTableView.getItems().remove(profile)))
						.subscribe();
			}
		});

		profileXContextMenu = new XContextMenu<>(profilesTableView, deleteItem);
		profileXContextMenu.setOnShowing((contextMenu, profile) -> profile.getId() != OWN_PROFILE_ID);
	}
}
