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
import io.xeres.common.util.OsUtils;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.ShareClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.model.share.Share;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.DirectoryChooser;
import net.harawata.appdirs.AppDirsFactory;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import static io.xeres.common.dto.share.ShareConstants.INCOMING_SHARE;
import static javafx.scene.control.Alert.AlertType.INFORMATION;
import static javafx.scene.control.TableColumn.SortType.ASCENDING;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Component
@FxmlView(value = "/view/file/share.fxml")
public class ShareWindowController implements WindowController
{
	private static final String REMOVE_MENU_ID = "remove";
	private static final String SHOW_IN_FOLDER_MENU_ID = "showInFolder";

	private final ShareClient shareClient;

	private final ResourceBundle bundle;

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

	public ShareWindowController(ShareClient shareClient, ResourceBundle bundle)
	{
		this.shareClient = shareClient;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		createShareTableViewContextMenu();

		tableDirectory.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getPath()));
		tableDirectory.setOnEditStart(param -> {
			if (refreshHack)
			{
				refreshHack = false;
				return;
			}
			if (RemoteUtils.isRemoteUiClient())
			{
				UiUtils.alert(INFORMATION, bundle.getString("settings.directory.no-remote"));
				return;
			}
			var directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle(bundle.getString("share.select-directory"));
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
		// XXX: when clicking outside tableName, the value isn't committed but the edited value stays on display anyway (which is wrong). but we get no event at all

		// setOnEditCommit() doesn't work for CheckBoxes, so we have to do that
		tableSearchable.setCellValueFactory(param -> {
			var checkBox = new CheckBox();
			checkBox.selectedProperty().setValue(param.getValue().isSearchable());
			checkBox.selectedProperty().addListener((observableValue, oldValue, newValue) -> param.getValue().setSearchable(newValue));
			return new SimpleObjectProperty(checkBox);
		});

		tableBrowsable.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getBrowsable()));
		tableBrowsable.setCellFactory(ChoiceBoxTableCell.forTableColumn(new TrustConverter(), Trust.values()));
		tableBrowsable.setOnEditCommit(param -> getCurrentItem(param).setBrowsable(param.getNewValue()));

		addButton.setOnAction(event -> {
			var downloadDir = AppDirsFactory.getInstance().getUserDownloadsDir(null, null, null);
			var downloadPath = Paths.get(downloadDir);
			var newShare = new Share();
			newShare.setName(downloadPath.getName(downloadPath.getNameCount() - 1).toString());
			newShare.setPath(downloadDir);
			newShare.setSearchable(true);
			newShare.setBrowsable(Trust.NEVER);
			shareTableView.getItems().add(newShare);
			shareTableView.getSelectionModel().select(newShare);
			shareTableView.edit(shareTableView.getSelectionModel().getSelectedIndex(), tableName);
		});

		applyButton.setOnAction(event -> Platform.runLater(() -> {
			if (validateShares())
			{
				shareClient.createAndUpdate(shareTableView.getItems())
						.doOnSuccess(unused -> Platform.runLater(() -> UiUtils.closeWindow(event)))
						.doOnError(UiUtils::showAlertError)
						.subscribe();
			}
		}));

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
				.doOnError(UiUtils::showAlertError)
				.subscribe();
	}

	private void createShareTableViewContextMenu()
	{
		var removeItem = new MenuItem(bundle.getString("share.remove"));
		removeItem.setId(REMOVE_MENU_ID);
		removeItem.setGraphic(new FontIcon(FontAwesomeSolid.TIMES));
		removeItem.setOnAction(event -> {
			var share = (Share) event.getSource();
			shareTableView.getItems().remove(share);
		});

		var showInExplorerItem = new MenuItem(bundle.getString("download.view.show-in-folder"));
		showInExplorerItem.setId(SHOW_IN_FOLDER_MENU_ID);
		showInExplorerItem.setGraphic(new FontIcon(FontAwesomeSolid.FOLDER_OPEN));
		showInExplorerItem.setOnAction(event -> {
			if (event.getSource() instanceof Share share)
			{
				OsUtils.showFolder(Paths.get(share.getPath()).toFile());
			}
		});

		var xContextMenu = new XContextMenu<Share>(showInExplorerItem, new SeparatorMenuItem(), removeItem);
		xContextMenu.addToNode(shareTableView);
		xContextMenu.setOnShowing((contextMenu, share) -> {
			contextMenu.getItems().stream()
					.filter(menuItem -> REMOVE_MENU_ID.equals(menuItem.getId()))
					.findFirst().ifPresent(menuItem -> menuItem.setDisable(share.getId() == INCOMING_SHARE));
			return share != null;
		});
	}

	private boolean validateShares()
	{
		Set<String> shareNames = HashSet.newHashSet(shareTableView.getItems().size());

		for (var share : shareTableView.getItems())
		{
			try
			{
				if (isBlank(share.getName()))
				{
					throw new IllegalArgumentException(bundle.getString("share.error.empty-name"));
				}
				if (isBlank(share.getPath()))
				{
					throw new IllegalArgumentException(bundle.getString("share.error.empty-path"));
				}
				if (shareNames.contains(share.getName()))
				{
					throw new IllegalArgumentException(bundle.getString("share.error.not-unique"));
				}
				shareNames.add(share.getName());
			}
			catch (IllegalArgumentException e)
			{
				shareTableView.getSelectionModel().select(share);
				UiUtils.showAlertError(e);
				return false;
			}
		}
		return true;
	}

	private static <T> T getCurrentItem(TableColumn.CellEditEvent<T, ?> param)
	{
		return param.getTableView().getItems().get(param.getTablePosition().getRow());
	}
}
