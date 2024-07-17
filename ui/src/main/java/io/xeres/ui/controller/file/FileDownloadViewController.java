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
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ProgressBarTableCell;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;

@Component
@FxmlView(value = "/view/file/download.fxml")
public class FileDownloadViewController implements Controller, TabActivation
{
	private static final int UPDATE_IN_SECONDS = 2;

	private final FileClient fileClient;

	@FXML
	private TableView<FileProgress> downloadTableView;

	@FXML
	private TableColumn<FileProgress, String> tableName;

	@FXML
	private TableColumn<FileProgress, Double> tableProgress;

	@FXML
	private TableColumn<FileProgress, Long> tableTotalSize;

	@FXML
	private TableColumn<FileProgress, String> tableHash;

	private ScheduledExecutorService executorService;

	private boolean wasRunning;

	public FileDownloadViewController(FileClient fileClient)
	{
		this.fileClient = fileClient;
	}

	@Override
	public void initialize()
	{
		tableName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().name()));
		tableProgress.setCellFactory(ProgressBarTableCell.forTableColumn());
		tableProgress.setCellValueFactory(param -> new SimpleObjectProperty<>((double) param.getValue().currentSize() / param.getValue().totalSize()));
		tableTotalSize.setCellFactory(param -> new FileProgressSizeCell());
		tableTotalSize.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().totalSize()));
		tableHash.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().hash()));
	}

	private void start()
	{
		executorService = ExecutorUtils.createFixedRateExecutor(() -> fileClient.getDownloads().collectList()
						.doOnSuccess(fileProgresses -> Platform.runLater(() -> {
							downloadTableView.getItems().clear(); // XXX: not optimal... this prevents selection, etc...
							downloadTableView.getItems().addAll(fileProgresses);
						}))
						.subscribe(),
				0,
				UPDATE_IN_SECONDS);
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
}
