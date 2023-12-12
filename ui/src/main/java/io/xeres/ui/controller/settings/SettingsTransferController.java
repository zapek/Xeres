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

import io.xeres.ui.model.settings.Settings;
import io.xeres.ui.support.util.UiUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@FxmlView(value = "/view/settings/settings_transfer.fxml")
public class SettingsTransferController implements SettingsController
{
	@FXML
	private TextField incomingDirectory;

	@FXML
	private Button incomingDirectorySelector;

	@Override
	public void initialize() throws IOException
	{
		incomingDirectorySelector.setOnAction(event -> {
			// XXX: this is all wrong. I cannot use that because the file system is actually REMOTE. have to write everything by hand (browsing, selection, etc...)
			var directoryChooser = new DirectoryChooser();
			directoryChooser.setTitle("Select Incoming Directory");
			//directoryChooser.setInitialDirectory(Path.of()); ... how to get dataDir? have to ask the server... actually... the selection would be remote :) this is hard...
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

	}

	@Override
	public Settings onSave()
	{
		return null;
	}
}
