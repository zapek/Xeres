/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.about;

import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@FxmlView(value = "/view/about/about.fxml")
public class AboutWindowController implements WindowController
{
	@FXML
	private Button closeWindow;

	@FXML
	private Hyperlink website;

	@FXML
	private TextArea licenseTextArea;

	@FXML
	private Label version;

	private final BuildProperties buildProperties;

	public AboutWindowController(BuildProperties buildProperties)
	{
		this.buildProperties = buildProperties;
	}

	public void initialize() throws IOException
	{
		version.setText(buildProperties.getVersion());
		licenseTextArea.setText(UiUtils.getResourceFileAsString(getClass().getResourceAsStream("/LICENSE")));

		closeWindow.setOnAction(UiUtils::closeWindow);
		website.setOnAction(event -> JavaFxApplication.openUrl("https://xeres.io/"));
		Platform.runLater(() -> closeWindow.requestFocus());
	}
}
