/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.settings;

import io.xeres.ui.JavaFxApplication;
import io.xeres.ui.model.settings.Settings;
import io.xeres.ui.support.util.TextFieldUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ResourceBundle;

import static javafx.scene.control.Alert.AlertType.INFORMATION;

@Component
@FxmlView(value = "/view/settings/settings_transfer.fxml")
public class SettingsTransferController implements SettingsController
{
	@FXML
	private TextField incomingDirectory;

	@FXML
	private Button incomingDirectorySelector;

	private Settings settings;

	private final ResourceBundle bundle;

	public SettingsTransferController(ResourceBundle bundle)
	{
		this.bundle = bundle;
	}

	@Override
	public void initialize()
	{
		incomingDirectorySelector.setOnAction(event -> {
			if (JavaFxApplication.isRemoteUiClient())
			{
				UiUtils.alert(INFORMATION, bundle.getString("settings.directory.no-remote"));
				return;
			}
			var directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle(bundle.getString("settings.transfer.select-incoming"));
			if (settings.hasIncomingDirectory())
			{
				directoryChooser.setInitialDirectory(Path.of(settings.getIncomingDirectory()).toFile());
			}
			var selectedDirectory = directoryChooser.showDialog(UiUtils.getWindow(event));
			if (selectedDirectory != null && selectedDirectory.isDirectory())
			{
				incomingDirectory.setText(selectedDirectory.getAbsolutePath());
			}
		});
	}

	@Override
	public void onLoad(Settings settings)
	{
		this.settings = settings;

		incomingDirectory.setText(settings.getIncomingDirectory());
	}

	@Override
	public Settings onSave()
	{
		settings.setIncomingDirectory(TextFieldUtils.getString(incomingDirectory));

		return settings;
	}
}
