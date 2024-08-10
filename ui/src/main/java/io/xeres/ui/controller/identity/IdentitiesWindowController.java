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

package io.xeres.ui.controller.identity;

import io.xeres.common.id.Id;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.IdentityClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.model.identity.Identity;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static javafx.scene.control.TableColumn.SortType.ASCENDING;

@Component
@FxmlView(value = "/view/identity/identities.fxml")
public class IdentitiesWindowController implements WindowController
{
	private final IdentityClient identityClient;

	private final GeneralClient generalClient;

	@FXML
	private TableView<Identity> identitiesTableView;

	@FXML
	private TableColumn<Identity, String> tableName;

	@FXML
	private TableColumn<Identity, String> tableGxsId;

	@FXML
	private TableColumn<Identity, String> tableUpdated;

	@FXML
	private TableColumn<Identity, Long> tableImage;

	public IdentitiesWindowController(IdentityClient identityClient, GeneralClient generalClient)
	{
		this.identityClient = identityClient;
		this.generalClient = generalClient;
	}

	@Override
	public void initialize()
	{
		tableName.setCellValueFactory(new PropertyValueFactory<>("name"));
		tableGxsId.setCellValueFactory(param -> new SimpleStringProperty(Id.toString(param.getValue().getGxsId())));
		tableUpdated.setCellValueFactory(param ->
				new SimpleStringProperty(
						DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
								.withZone(ZoneId.systemDefault())
								.format(param.getValue().getUpdated())));
		tableImage.setCellFactory(param -> new ImageCell(generalClient));
		tableImage.setCellValueFactory(new PropertyValueFactory<>("id"));

		identityClient.getIdentities().collectList()
				.doOnSuccess(identities -> Platform.runLater(() -> {
					// Add all identities
					identitiesTableView.getItems().addAll(identities);

					// Sort by name
					identitiesTableView.getSortOrder().add(tableName);
					tableName.setSortType(ASCENDING);
					tableName.setSortable(true);
				}))
				.doOnError(UiUtils::showAlertError)
				.subscribe();
	}
}
