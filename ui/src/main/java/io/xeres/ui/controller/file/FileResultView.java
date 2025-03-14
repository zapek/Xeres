/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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
import io.xeres.common.id.Sha1Sum;
import io.xeres.ui.client.FileClient;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.uri.FileUri;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ResourceBundle;

public class FileResultView extends Tab
{
	private static final Logger log = LoggerFactory.getLogger(FileResultView.class);

	private static final String DOWNLOAD_MENU_ID = "download";
	private static final String COPY_LINK_MENU_ID = "copyLink";

	public static final int FILE_ICON_SIZE = 24;

	private final FileClient fileClient;
	private final ResourceBundle bundle;

	private final int searchId;

	@FXML
	private TableView<FileResult> filesTableView;

	@FXML
	private TableColumn<FileResult, FileResult> tableName;

	@FXML
	private TableColumn<FileResult, Long> tableSize;

	@FXML
	private TableColumn<FileResult, String> tableType;

	@FXML
	private TableColumn<FileResult, String> tableHash;

	@FXML
	private ProgressBar progressBar;

	public FileResultView(FileClient fileClient, String text, int searchId)
	{
		super(text);
		this.fileClient = fileClient;
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
		tableSize.setCellFactory(param -> new FileResultSizeCell());
		tableSize.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().size()));
		tableType.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().type().toString()));
		tableHash.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().hash()));

		showProgress();
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

	private static String getIconCodeForType(FileType type)
	{
		return switch (type)
		{
			case AUDIO -> "mdi2f-file-music";
			case VIDEO -> "mdi2f-file-video";
			case PICTURE -> "mdi2f-file-image";
			case DOCUMENT -> "mdi2f-file-document";
			case ARCHIVE -> "mdi2f-file-cabinet";
			case PROGRAM -> "mdi2a-application";
			case COLLECTION -> "mdi2l-layers";
			case SUBTITLES -> "mdi2c-closed-caption";
			case DIRECTORY, ANY -> "mdi2f-file";
		};
	}

	private void createFilesTableViewContextMenu()
	{
		var downloadItem = new MenuItem(bundle.getString("download"));
		downloadItem.setId(DOWNLOAD_MENU_ID);
		downloadItem.setGraphic(new FontIcon(MaterialDesignF.FILE_DOWNLOAD));
		downloadItem.setOnAction(event -> {
			if (event.getSource() instanceof FileResult file)
			{
				log.debug("Downloading file {}", file.name());
				fileClient.download(file.name(), Sha1Sum.fromString(file.hash()), file.size(), null)
						.subscribe();
			}
		});

		var copyLinkItem = new MenuItem(bundle.getString("copy-link"));
		copyLinkItem.setId(COPY_LINK_MENU_ID);
		copyLinkItem.setGraphic(new FontIcon(MaterialDesignL.LINK_VARIANT));
		copyLinkItem.setOnAction(event -> {
			if (event.getSource() instanceof FileResult file)
			{
				var fileUri = new FileUri(file.name(), file.size(), Sha1Sum.fromString(file.hash()));
				ClipboardUtils.copyTextToClipboard(fileUri.toString());
			}
		});

		var xContextMenu = new XContextMenu<FileResult>(downloadItem, new SeparatorMenuItem(), copyLinkItem);
		xContextMenu.addToNode(filesTableView);
		xContextMenu.setOnShowing((contextMenu, file) -> file != null);
	}

	private void showProgress()
	{
		var task = new Task<Void>()
		{
			@Override
			protected Void call() throws Exception
			{
				for (var d = 0.0; d <= 1.0; d += 0.001)
				{
					Thread.sleep(20);
					double finalD = d;
					Platform.runLater(() -> progressBar.setProgress(finalD));
				}
				Platform.runLater(() -> {
					progressBar.setProgress(1.0);
					filesTableView.setPlaceholder(new Label(bundle.getString("no-results")));
				});
				return null;
			}
		};
		Thread.ofVirtual().name("Search Progress Indicator Task").start(task);
	}
}
