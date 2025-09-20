/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.custom;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.client.LocationClient;
import io.xeres.ui.custom.event.FileSelectedEvent;
import io.xeres.ui.custom.event.ImageSelectedEvent;
import io.xeres.ui.support.util.TextInputControlUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static io.xeres.ui.support.util.UiUtils.getWindow;

public class InputAreaGroup extends HBox
{
	@FXML
	private InputArea inputArea;

	@FXML
	private Button addImage;

	@FXML
	private Button addFile;

	@FXML
	private Button addSticker;

	private final ResourceBundle bundle;

	public InputAreaGroup()
	{
		bundle = I18nUtils.getBundle();

		var loader = new FXMLLoader(InputAreaGroup.class.getResource("/view/custom/inputareagroup.fxml"), bundle);
		loader.setRoot(this);
		loader.setController(this);

		try
		{
			loader.load();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@FXML
	private void initialize()
	{
		disabledProperty().addListener((_, _, newValue) -> {
			addImage.setDisable(newValue);
			addFile.setDisable(newValue);
			addSticker.setDisable(newValue);
		});

		addImage.setOnAction(_ -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle(bundle.getString("messaging.file-requester.send-picture"));
			fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(bundle.getString("file-requester.images"), "*.png", "*.jpg", "*.jpeg", "*.jfif"));
			var selectedFile = fileChooser.showOpenDialog(getWindow(this));
			if (selectedFile != null)
			{
				fireEvent(new ImageSelectedEvent(selectedFile));
			}
		});

		addFile.setOnAction(event -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle(bundle.getString("messaging.file-requester.send-file"));
			var selectedFile = fileChooser.showOpenDialog(getWindow(event));
			if (selectedFile != null)
			{
				fireEvent(new FileSelectedEvent(selectedFile));
			}
		});

		addSticker.setOnAction(_ -> inputArea.openStickerSelector());
	}

	public void clear()
	{
		inputArea.clear();
	}

	public void addKeyFilter(EventHandler<? super KeyEvent> eventFilter)
	{
		inputArea.addEventFilter(KeyEvent.KEY_PRESSED, eventFilter);
	}

	public void addEnhancedContextMenu(Consumer<TextInputControl> pasteAction)
	{
		addEnhancedContextMenu(pasteAction, null);
	}

	public void addEnhancedContextMenu(Consumer<TextInputControl> pasteAction, LocationClient locationClient)
	{
		TextInputControlUtils.addEnhancedInputContextMenu(inputArea, locationClient, pasteAction);
	}

	public TextInputControl getTextInputControl()
	{
		return inputArea;
	}

	@Override
	public void requestFocus()
	{
		inputArea.requestFocus();
	}

	/**
	 * Sets the input area to offline mode. Sending images, files and stickers will be disabled, but
	 * text can still be entered.
	 *
	 * @param offline true if offline
	 */
	public void setOffline(boolean offline)
	{
		addImage.setDisable(offline);
		addFile.setDisable(offline);
		addSticker.setDisable(offline);
	}
}
