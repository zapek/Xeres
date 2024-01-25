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
import io.xeres.ui.client.ShareClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.model.share.Share;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.DirectoryChooser;
import net.harawata.appdirs.AppDirsFactory;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static javafx.scene.control.Alert.AlertType.INFORMATION;
import static javafx.scene.control.TableColumn.SortType.ASCENDING;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component
@FxmlView(value = "/view/file/share.fxml")
public class ShareWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(ShareWindowController.class);

	private final ShareClient shareClient;

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

	@FXML
	private Button applyButton;

	@FXML
	private Button addButton;

	@FXML
	private Button cancelButton;

	private boolean refreshHack;

	public ShareWindowController(ShareClient shareClient)
	{
		this.shareClient = shareClient;
	}

	@Override
	public void initialize() throws IOException
	{
		tableDirectory.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getPath()));
		tableDirectory.setOnEditStart(param -> {
			if (refreshHack)
			{
				refreshHack = false;
				return;
			}
			if (JavaFxApplication.isRemoteUiClient())
			{
				UiUtils.alert(INFORMATION, "Cannot chose a directory in remote mode");
				return;
			}
			var directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Select directory to share");
			if (!isEmpty(param.getOldValue()))
			{
				var previousPath = Path.of(param.getOldValue());
				if (Files.exists(previousPath))
				{
					directoryChooser.setInitialDirectory(previousPath.toFile());
				}
			}
			var selectedDirectory = directoryChooser.showDialog(UiUtils.getWindow(shareTableView));
			if (selectedDirectory != null && selectedDirectory.isDirectory())
			{
				getCurrentItem(param).setPath(selectedDirectory.getPath());
				refreshHack = true; // refresh() calls setOnEditStart() again so we need that workaround
				param.getTableView().refresh();

			}

			// We clear the selection so that the directory selector can be triggered again. Go figure...
			Platform.runLater(param.getTableView().getSelectionModel()::clearSelection);
		});
		tableDirectory.setOnEditCommit(param -> getCurrentItem(param).setPath(param.getNewValue()));

		tableName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getName()));
		tableName.setCellFactory(TextFieldTableCell.forTableColumn());
		tableName.setOnEditCommit(param -> getCurrentItem(param).setName(param.getNewValue()));

		// setOnEditCommit() doesn't work for CheckBoxes, so we have to do that
		tableSearchable.setCellValueFactory(param -> {
			var checkBox = new CheckBox();
			checkBox.selectedProperty().setValue(param.getValue().isSearchable());
			checkBox.selectedProperty().addListener((observableValue, oldValue, newValue) -> param.getValue().setSearchable(newValue));
			return new SimpleObjectProperty(checkBox);
		});

		tableBrowsable.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getBrowsable()));
		tableBrowsable.setCellFactory(ChoiceBoxTableCell.forTableColumn(Trust.values()));
		tableBrowsable.setOnEditCommit(param -> getCurrentItem(param).setBrowsable(param.getNewValue()));


		addButton.setOnAction(event -> {
			var newShare = new Share();
			newShare.setName("Change Me");
			newShare.setPath(AppDirsFactory.getInstance().getUserDownloadsDir(null, null, null));
			newShare.setSearchable(true);
			newShare.setBrowsable(Trust.NEVER);
			shareTableView.getItems().add(newShare);
		});

		// XXX: set action on apply!

		cancelButton.setOnAction(UiUtils::closeWindow);

		shareClient.findAll().collectList()
				.doOnSuccess(shares -> Platform.runLater(() -> {
					// Add all shares
					shareTableView.getItems().addAll(shares);

					// Sort by visible name
					shareTableView.getSortOrder().add(tableName);
					tableName.setSortType(ASCENDING);
					tableName.setSortable(true);
				}))
				.doOnError(throwable -> log.error("Error while getting the shares: {}", throwable.getMessage(), throwable))
				.subscribe();
	}

	private static <T> T getCurrentItem(TableColumn.CellEditEvent<T, ?> param)
	{
		return param.getTableView().getItems().get(param.getTablePosition().getRow());
	}
}
