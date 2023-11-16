/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.text.Text;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ResourceBundle;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

@Component
@FxmlView(value = "/view/about/about.fxml")
public class AboutWindowController implements WindowController
{
	@FXML
	private Button closeWindow;

	@FXML
	private TabPane infoPane;

	@FXML
	private Text license;

	@FXML
	private Label version;

	@FXML
	private Label profile;

	private final BuildProperties buildProperties;
	private final Environment environment;
	private final ResourceBundle bundle;

	public AboutWindowController(BuildProperties buildProperties, Environment environment, ResourceBundle bundle)
	{
		this.buildProperties = buildProperties;
		this.environment = environment;
		this.bundle = bundle;
	}

	@Override
	public void initialize() throws IOException
	{
		version.setText(buildProperties.getVersion());
		if (isEmpty(environment.getActiveProfiles()))
		{
			profile.setText(bundle.getString("about.release"));
		}
		else
		{
			profile.setText("Profiles: " + String.join(", ", environment.getActiveProfiles()));
		}
		license.setText(UiUtils.getResourceFileAsString(getClass().getResourceAsStream("/LICENSE")));

		closeWindow.setOnAction(UiUtils::closeWindow);
		UiUtils.linkify(infoPane);

		Platform.runLater(() -> closeWindow.requestFocus());
	}
}
