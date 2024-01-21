/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.share;

import io.xeres.common.pgp.Trust;
import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.model.share.Share;
import io.xeres.ui.support.util.UiUtils;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.DirectoryChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static javafx.scene.control.Alert.AlertType.INFORMATION;

@Component
@FxmlView(value = "/view/file/share.fxml")
public class ShareWindowController implements WindowController
{
	@FXML
	private TableView<Share> shareTableView;

	@FXML
	private TableColumn<Share, String> tableDirectory; // XXX: or path?

	@FXML
	private TableColumn<Share, String> tableName;

	@FXML
	private TableColumn<Share, Boolean> tableSearchable;

	@FXML
	private TableColumn<Share, Trust> tableBrowsable;

	private boolean refreshHack;

	@Override
	public void initialize() throws IOException
	{
		tableDirectory.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getPath()));
		tableDirectory.setOnEditStart(param -> {
			if (refreshHack)
			{
				refreshHack = false;
				// XXX: do something to make sure it's selectable again
				return;
			}
			if (JavaFxApplication.isRemoteUiClient())
			{
				UiUtils.alert(INFORMATION, "Cannot chose a directory in remote mode");
				return;
			}
			var directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Select directory to share");
			// XXX: set initial directory? yes, to param.getOldValue() ... (if it still exists...)
			var selectedDirectory = directoryChooser.showDialog(UiUtils.getWindow(shareTableView));
			if (selectedDirectory != null && selectedDirectory.isDirectory())
			{
				getCurrentItem(param).setPath(selectedDirectory.getPath()); // XXX: canonical?
				refreshHack = true; // refresh() calls setOnEditStart() again so we need that workaround
				tableDirectory.setOnEditStart(null);
				param.getTableView().refresh();
			}
		});
		tableDirectory.setOnEditCommit(param -> getCurrentItem(param).setPath(param.getNewValue()));

		tableName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
		tableName.setCellFactory(TextFieldTableCell.forTableColumn());
		tableName.setOnEditCommit(param -> getCurrentItem(param).setName(param.getNewValue()));

		tableSearchable.setCellValueFactory(param -> new SimpleBooleanProperty(param.getValue().isSearchable()));
		tableSearchable.setCellFactory(CheckBoxTableCell.forTableColumn(tableSearchable));
		tableSearchable.setOnEditCommit(param -> getCurrentItem(param).setSearchable(param.getNewValue()));

		tableBrowsable.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getBrowsable()));
		tableBrowsable.setCellFactory(ChoiceBoxTableCell.forTableColumn(Trust.values()));
		tableBrowsable.setOnEditCommit(param -> getCurrentItem(param).setBrowsable(param.getNewValue()));

		var share = new Share();
		share.setName("Incoming");
		share.setPath("C:\\temp");
		share.setBrowsable(Trust.UNKNOWN);
		share.setSearchable(false);
		shareTableView.getItems().add(share);
	}

	private static <T> T getCurrentItem(TableColumn.CellEditEvent<T, ?> param)
	{
		return param.getTableView().getItems().get(param.getTablePosition().getRow());
	}
}
