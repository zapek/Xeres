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

package io.xeres.ui.controller.file;

import io.xeres.common.file.FileType;
import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.util.ByteUnitUtils;
import io.xeres.ui.support.contextmenu.XContextMenu;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ResourceBundle;

public class FileResultView extends Tab
{
	private static final Logger log = LoggerFactory.getLogger(FileResultView.class);

	private static final String DOWNLOAD_MENU_ID = "download";
	public static final int FILE_ICON_SIZE = 24;

	private final ResourceBundle bundle;

	private final int searchId;

	@FXML
	private TableView<FileResult> filesTableView;

	@FXML
	private TableColumn<FileResult, FileResult> tableName;

	@FXML
	private TableColumn<FileResult, String> tableSize;

	@FXML
	private TableColumn<FileResult, String> tableType;

	@FXML
	private TableColumn<FileResult, String> tableHash;


	public FileResultView(String text, int searchId)
	{
		super(text);
		this.searchId = searchId;

		bundle = I18nUtils.getBundle();

		var loader = new FXMLLoader(getClass().getResource("/view/custom/file_results_view.fxml"), bundle);
		loader.setRoot(this);
		loader.setController(this);

		try
		{
			loader.load();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void initialize()
	{
		createFilesTableViewContextMenu();

		tableName.setCellFactory(param -> new FileResultNameCell(this::getGraphicForType));
		tableName.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
		tableSize.setCellValueFactory(param -> new SimpleStringProperty(ByteUnitUtils.fromBytes(param.getValue().size())));
		tableType.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().type().toString()));
		tableHash.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().hash()));
	}

	public int getSearchId()
	{
		return searchId;
	}

	public void addResult(String name, long size, String hash)
	{
		var file = new FileResult(name, size, FileType.getTypeByExtension(name), hash);

		if (!filesTableView.getItems().contains(file))
		{
			filesTableView.getItems().add(file);
		}
	}

	private Node getGraphicForType(FileType type)
	{
		var pane = new StackPane(new FontIcon(getIconCodeForType(type)));
		pane.setPrefWidth(FILE_ICON_SIZE);
		pane.setPrefHeight(FILE_ICON_SIZE);
		pane.setAlignment(Pos.CENTER);
		return pane;
	}

	private String getIconCodeForType(FileType type)
	{
		return switch (type)
		{
			case AUDIO -> "fas-music";
			case VIDEO -> "fas-film";
			case PICTURE -> "fas-image";
			case DOCUMENT -> "fas-file-alt";
			case ARCHIVE -> "fas-archive";
			case PROGRAM -> "fas-microchip";
			case COLLECTION -> "fas-layer-group";
			case SUBTITLES -> "fas-closed-captioning";
			case DIRECTORY, ANY -> "fas-file";
		};
	}

	private void createFilesTableViewContextMenu()
	{
		var downloadItem = new MenuItem("Download");
		downloadItem.setId(DOWNLOAD_MENU_ID);
		downloadItem.setOnAction(event -> {
			if (event.getSource() instanceof FileResult file)
			{
				log.debug("Downloading file {}", file.name());
				// XXX: call to download the file
			}
		});

		var fileXContextMenu = new XContextMenu<FileResult>(filesTableView, downloadItem);
		fileXContextMenu.setOnShowing((contextMenu, file) -> file != null);
	}
}
