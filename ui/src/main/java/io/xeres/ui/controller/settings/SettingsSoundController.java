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

package io.xeres.ui.controller.settings;

import io.xeres.ui.model.settings.Settings;
import io.xeres.ui.support.sound.SoundService;
import io.xeres.ui.support.sound.SoundSettings;
import io.xeres.ui.support.util.UiUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;

@Component
@FxmlView(value = "/view/settings/settings_sound.fxml")
public class SettingsSoundController implements SettingsController
{
	@FXML
	private CheckBox messageEnabled;

	@FXML
	private CheckBox highlightEnabled;

	@FXML
	private CheckBox friendEnabled;

	@FXML
	private CheckBox downloadEnabled;

	@FXML
	private TextField messageFile;

	@FXML
	private TextField highlightFile;

	@FXML
	private TextField friendFile;

	@FXML
	private TextField downloadFile;

	@FXML
	private Button messageFileSelector;

	@FXML
	private Button highlightFileSelector;

	@FXML
	private Button friendFileSelector;

	@FXML
	private Button downloadFileSelector;

	@FXML
	private Button messagePlay;

	@FXML
	private Button highlightPlay;

	@FXML
	private Button friendPlay;

	@FXML
	private Button downloadPlay;

	private final ResourceBundle bundle;
	private final SoundSettings soundSettings;
	private final SoundService soundService;

	public SettingsSoundController(ResourceBundle bundle, SoundSettings soundSettings, SoundService soundService)
	{
		this.bundle = bundle;
		this.soundSettings = soundSettings;
		this.soundService = soundService;
	}

	@Override
	public void initialize() throws IOException
	{
		initializeSoundPath(messageEnabled, messageFile, messageFileSelector, messagePlay);
		initializeSoundPath(highlightEnabled, highlightFile, highlightFileSelector, highlightPlay);
		initializeSoundPath(friendEnabled, friendFile, friendFileSelector, friendPlay);
		initializeSoundPath(downloadEnabled, downloadFile, downloadFileSelector, downloadPlay);
	}

	private void initializeSoundPath(CheckBox checkbox, TextField path, Button pathSelector, Button playButton)
	{
		checkbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
			path.setDisable(!newValue);
			pathSelector.setDisable(!newValue);
			playButton.setDisable(!newValue);
		});
		pathSelector.setOnAction(event -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle(bundle.getString("file-requester.select-sound-title"));
			fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(bundle.getString("file-requester.sounds"), "*.aif", "*.aiff", "*.mp3", "*.mp4", "*.wav"));
			if (!path.getText().isEmpty())
			{
				fileChooser.setInitialFileName(path.getText());
				setInitialDirectoryIfExists(fileChooser, path.getText());
			}
			var selectedFile = fileChooser.showOpenDialog(UiUtils.getWindow(event));
			if (selectedFile != null && selectedFile.isFile())
			{
				path.setText(selectedFile.getAbsolutePath());
			}
		});
		playButton.setOnAction(actionEvent -> soundService.play(path.getText()));
	}

	private static void setInitialDirectoryIfExists(FileChooser fileChooser, String path)
	{
		var parent = Path.of(path).getParent();
		if (parent != null)
		{
			var file = parent.toFile();
			if (file.exists() && file.isDirectory())
			{
				fileChooser.setInitialDirectory(file);
			}
		}
	}

	@Override
	public void onLoad(Settings settings)
	{
		messageEnabled.setSelected(soundSettings.isMessageEnabled());
		highlightEnabled.setSelected(soundSettings.isHighlightEnabled());
		friendEnabled.setSelected(soundSettings.isFriendEnabled());
		downloadEnabled.setSelected(soundSettings.isDownloadEnabled());

		messageFile.setText(soundSettings.getMessageFile());
		highlightFile.setText(soundSettings.getHighlightFile());
		friendFile.setText(soundSettings.getFriendFile());
		downloadFile.setText(soundSettings.getDownloadFile());
	}

	@Override
	public Settings onSave()
	{
		soundSettings.setMessageEnabled(messageEnabled.isSelected());
		soundSettings.setHighlightEnabled(highlightEnabled.isSelected());
		soundSettings.setFriendEnabled(friendEnabled.isSelected());
		soundSettings.setDownloadEnabled(downloadEnabled.isSelected());

		soundSettings.setMessageFile(messageFile.getText());
		soundSettings.setHighlightFile(highlightFile.getText());
		soundSettings.setFriendFile(friendFile.getText());
		soundSettings.setDownloadFile(downloadFile.getText());

		soundSettings.save();
		return null;
	}
}
