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

import io.xeres.common.rest.file.FileProgress;
import io.xeres.common.util.ExecutorUtils;
import io.xeres.common.util.OsUtils;
import io.xeres.ui.client.FileClient;
import io.xeres.ui.client.SettingsClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.TabActivation;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.concurrent.ScheduledExecutorService;

import static io.xeres.ui.controller.file.FileProgressDisplay.State.*;
import static javafx.scene.control.Alert.AlertType.ERROR;

@Component
@FxmlView(value = "/view/file/download.fxml")
public class FileDownloadViewController implements Controller, TabActivation
{
	private static final Logger log = LoggerFactory.getLogger(FileDownloadViewController.class);

	private static final int UPDATE_IN_SECONDS = 2;

	private static final String CANCEL_MENU_ID = "cancel";
	private static final String OPEN_MENU_ID = "open";
	private static final String SHOW_IN_FOLDER_MENU_ID = "showInFolder";

	private final FileClient fileClient;
	private final SettingsClient settingsClient;
	private final ResourceBundle bundle;

	@FXML
	private TableView<FileProgressDisplay> downloadTableView;

	@FXML
	private TableColumn<FileProgressDisplay, String> tableName;

	@FXML
	private TableColumn<FileProgressDisplay, String> tableState;

	@FXML
	private TableColumn<FileProgressDisplay, Double> tableProgress;

	@FXML
	private TableColumn<FileProgressDisplay, Long> tableTotalSize;

	@FXML
	private TableColumn<FileProgressDisplay, String> tableHash;

	private ScheduledExecutorService executorService;

	private boolean wasRunning;

	public FileDownloadViewController(FileClient fileClient, SettingsClient settingsClient, ResourceBundle bundle)
	{
		this.fileClient = fileClient;
		this.settingsClient = settingsClient;
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		createContextMenu();

		tableName.setCellValueFactory(new PropertyValueFactory<>("name"));
		tableState.setCellValueFactory(new PropertyValueFactory<>("state"));
		tableProgress.setCellFactory(ProgressBarTableCell.forTableColumn());
		tableProgress.setCellValueFactory(new PropertyValueFactory<>("progress"));
		tableTotalSize.setCellFactory(param -> new FileProgressSizeCell());
		tableTotalSize.setCellValueFactory(new PropertyValueFactory<>("totalSize"));
		tableHash.setCellValueFactory(new PropertyValueFactory<>("hash"));
	}

	private void start()
	{
		executorService = ExecutorUtils.createFixedRateExecutor(() -> fileClient.getDownloads().collectMap(FileProgress::hash)
						.doOnSuccess(incomingProgresses -> Platform.runLater(() -> {
							var it = downloadTableView.getItems().iterator();
							while (it.hasNext())
							{
								var currentProgress = it.next();
								var incomingProgress = incomingProgresses.get(currentProgress.getHash());
								if (incomingProgress != null)
								{
									var newProgress = (double) incomingProgress.currentSize() / incomingProgress.totalSize();
									var newState = getState(currentProgress, incomingProgress, newProgress);
									if (currentProgress.getState() != REMOVING)
									{
										currentProgress.setState(newState);
									}
									currentProgress.setProgress(newProgress);
									incomingProgresses.remove(incomingProgress.hash());
								}
								else
								{
									it.remove();
								}
							}
							incomingProgresses.forEach((s, fileProgress) -> downloadTableView.getItems().add(new FileProgressDisplay(fileProgress.id(), fileProgress.name(), fileProgress.currentSize() == fileProgress.totalSize() ? DONE : SEARCHING, 0.0, fileProgress.totalSize(), fileProgress.hash())));
						}))
						.subscribe(),
				1,
				UPDATE_IN_SECONDS);
	}

	private static FileProgressDisplay.State getState(FileProgressDisplay currentProgress, FileProgress incomingProgress, double newProgress)
	{
		if (incomingProgress.currentSize() == incomingProgress.totalSize())
		{
			return DONE;
		}
		if (newProgress != currentProgress.getProgress())
		{
			return TRANSFERRING;
		}
		return SEARCHING;
	}

	public void stop()
	{
		ExecutorUtils.cleanupExecutor(executorService);
	}

	public void resume()
	{
		if (wasRunning)
		{
			start();
		}
	}

	@Override
	public void activate()
	{
		start();
		wasRunning = true;
	}

	@Override
	public void deactivate()
	{
		stop();
		wasRunning = false;
	}

	private void createContextMenu()
	{
		var removeItem = new MenuItem(bundle.getString("button.remove"));
		removeItem.setId(CANCEL_MENU_ID);
		removeItem.setGraphic(new FontIcon(FontAwesomeSolid.TIMES));
		removeItem.setOnAction(event -> {
			if (event.getSource() instanceof FileProgressDisplay fileProgressDisplay)
			{
				log.debug("Removing download of file {}", fileProgressDisplay.getName());
				fileClient.removeDownload(fileProgressDisplay.getId())
						.doOnSuccess(unused -> fileProgressDisplay.setState(REMOVING))
						.subscribe();
			}
		});

		var openItem = new MenuItem(bundle.getString("button.open"));
		openItem.setId(OPEN_MENU_ID);
		openItem.setGraphic(new FontIcon(FontAwesomeSolid.FILE));
		openItem.setOnAction(event -> {
			if (event.getSource() instanceof FileProgressDisplay fileProgressDisplay)
			{
				log.debug("Opening file {}", fileProgressDisplay.getName());
				settingsClient.getSettings()
						.doOnSuccess(settings -> {
							var file = Paths.get(settings.getIncomingDirectory(), fileProgressDisplay.getName()).toFile();
							try
							{
								OsUtils.shellOpen(file);
							}
							catch (IllegalStateException e)
							{
								Platform.runLater(() -> {
									UiUtils.alert(ERROR, bundle.getString("download.view.open-error") + " " + e.getMessage() + ".");
									log.error("Failed to open the file", e);
								});
							}
						})
						.subscribe();
			}
		});

		var showInExplorerItem = new MenuItem(bundle.getString("download.view.show-in-folder"));
		showInExplorerItem.setId(SHOW_IN_FOLDER_MENU_ID);
		showInExplorerItem.setGraphic(new FontIcon(FontAwesomeSolid.FOLDER_OPEN));
		showInExplorerItem.setOnAction(event -> {
			if (event.getSource() instanceof FileProgressDisplay fileProgressDisplay)
			{
				log.debug("Showing file {} in folder", fileProgressDisplay.getName());
				settingsClient.getSettings()
						.doOnSuccess(settings -> {
							var file = Paths.get(settings.getIncomingDirectory(), fileProgressDisplay.getName()).toFile();
							try
							{
								OsUtils.showInFolder(file);
							}
							catch (IllegalStateException e)
							{
								Platform.runLater(() -> {
									UiUtils.alert(ERROR, bundle.getString("download.view.show-error") + " " + e.getMessage() + ".");
									log.error("Failed to show the file in folder", e);
								});
							}
						})
						.subscribe();
			}
		});

		var fileXContextMenu = new XContextMenu<FileProgressDisplay>(downloadTableView, openItem, showInExplorerItem, new SeparatorMenuItem(), removeItem);
		fileXContextMenu.setOnShowing((contextMenu, file) -> file != null);
	}
}
