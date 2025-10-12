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

import io.xeres.ui.OpenUriEvent;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.TabActivation;
import io.xeres.ui.support.uri.SearchUri;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@FxmlView(value = "/view/file/main.fxml")
public class FileMainController implements Controller
{
	@FXML
	private TabPane tabPane;

	@FXML
	private FileSearchViewController fileSearchViewController;

	@FXML
	private FileDownloadViewController fileDownloadViewController;

	@FXML
	private FileUploadViewController fileUploadViewController;

	@FXML
	private FileTrendViewController fileTrendViewController;

	@Override
	public void initialize()
	{
		tabPane.getSelectionModel().selectedItemProperty()
				.addListener((_, oldValue, newValue) -> Platform.runLater(() -> {
					idToController(oldValue.getId()).deactivate();
					idToController(newValue.getId()).activate();
				}));
	}

	@EventListener
	public void handleOpenUriEvents(OpenUriEvent event)
	{
		if (event.uri() instanceof SearchUri _)
		{
			tabPane.getSelectionModel().select(0);
		}
	}

	private TabActivation idToController(String id)
	{
		return switch (id)
		{
			case "search" -> fileSearchViewController;
			case "downloads" -> fileDownloadViewController;
			case "uploads" -> fileUploadViewController;
			case "trends" -> fileTrendViewController;
			default -> throw new IllegalStateException("Unexpected value: " + id);
		};
	}

	public void resume()
	{
		fileDownloadViewController.resume();
		fileUploadViewController.resume();
	}

	public void suspend()
	{
		fileDownloadViewController.stop();
		fileUploadViewController.stop();
	}
}
