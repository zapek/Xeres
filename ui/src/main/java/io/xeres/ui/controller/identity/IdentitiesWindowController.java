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

package io.xeres.ui.controller.identity;

import io.xeres.ui.controller.WindowController;
import io.xeres.ui.model.identity.Identity;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@FxmlView(value = "/view/identity/identities.fxml")
public class IdentitiesWindowController implements WindowController
{
	// XXX: need client, and controller

	@FXML
	private TableView<Identity> identitiesTableView;

	@FXML
	private TableColumn<Identity, String> tableName;

	@Override
	public void initialize() throws IOException
	{
		//identitiesTableView.setRowFactory(IdentityCell::new);

		tableName.setCellValueFactory(new PropertyValueFactory<>("name"));

		// XXX: call client
	}
}
