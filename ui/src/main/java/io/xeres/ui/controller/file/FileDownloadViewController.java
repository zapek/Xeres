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
import io.xeres.ui.client.FileClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.TabActivation;
import io.xeres.ui.support.contextmenu.XContextMenu;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;

import static io.xeres.ui.controller.file.FileProgressDisplay.State.*;

@Component
@FxmlView(value = "/view/file/download.fxml")
public class FileDownloadViewController implements Controller, TabActivation
{
	private static final Logger log = LoggerFactory.getLogger(FileDownloadViewController.class);

	private static final int UPDATE_IN_SECONDS = 2;

	private static final String CANCEL_MENU_ID = "cancel";

	private final FileClient fileClient;

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

	public FileDownloadViewController(FileClient fileClient)
	{
		this.fileClient = fileClient;
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
									currentProgress.setState(getState(currentProgress, incomingProgress, newProgress));
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
		if (newProgress != currentProgress.getProgress())
		{
			return TRANSFERRING;
		}
		if (incomingProgress.currentSize() == incomingProgress.totalSize())
		{
			return DONE;
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
		var removeItem = new MenuItem("Remove");
		removeItem.setId(CANCEL_MENU_ID);
		removeItem.setOnAction(event -> {
			if (event.getSource() instanceof FileProgressDisplay file)
			{
				log.debug("Removing download of file {}", file.getName());
				fileClient.removeDownload(file.getId())
						.subscribe();
			}
		});

		var fileXContextMenu = new XContextMenu<FileProgressDisplay>(downloadTableView, removeItem);
		fileXContextMenu.setOnShowing((contextMenu, file) -> file != null);
	}
}
