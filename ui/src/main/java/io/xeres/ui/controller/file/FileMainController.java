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

import io.xeres.ui.controller.Controller;
import io.xeres.ui.controller.TabActivation;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import net.rgielen.fxweaver.core.FxmlView;
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

	@Override
	public void initialize()
	{
		tabPane.getSelectionModel().selectedItemProperty()
				.addListener((observableValue, oldValue, newValue) -> Platform.runLater(() -> {
					idToController(oldValue.getId()).deactivate();
					idToController(newValue.getId()).activate();
				}));
	}

	private TabActivation idToController(String id)
	{
		return switch (id)
		{
			case "search" -> fileSearchViewController;
			case "downloads" -> fileDownloadViewController;
			case "uploads" -> fileUploadViewController;
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
