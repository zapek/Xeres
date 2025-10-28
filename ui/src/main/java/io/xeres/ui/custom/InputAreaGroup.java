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
import io.xeres.ui.support.util.UiUtils;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static io.xeres.ui.support.util.UiUtils.getWindow;

public class InputAreaGroup extends HBox
{
	@FXML
	private InputArea inputArea;

	@FXML
	private Button addMedia;

	@FXML
	private Button addSticker;

	@FXML
	private Button callButton;

	private final ResourceBundle bundle;

	public InputAreaGroup()
	{
		bundle = I18nUtils.getBundle();

		var loader = new FXMLLoader(InputAreaGroup.class.getResource("/view/custom/input_area_group.fxml"), bundle);
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

	public ReadOnlyBooleanProperty callPressedProperty()
	{
		return callButton.pressedProperty();
	}

	@FXML
	private void initialize()
	{
		disabledProperty().addListener((_, _, newValue) -> {
			addMedia.setDisable(newValue);
			addSticker.setDisable(newValue);
		});

		addSticker.setOnAction(_ -> inputArea.openStickerSelector());

		createAddMediaContextMenu();
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
		addMedia.setDisable(offline);
		addSticker.setDisable(offline);
	}

	public void setVoipCapable(boolean voipCapable)
	{
		UiUtils.setPresent(callButton, voipCapable);
	}

	private void createAddMediaContextMenu()
	{
		var addImageItem = new MenuItem(bundle.getString("messaging.action.send-inline"));
		addImageItem.setGraphic(new FontIcon(MaterialDesignF.FILE_IMAGE_OUTLINE));
		addImageItem.setOnAction(event -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle(bundle.getString("messaging.file-requester.send-picture"));
			fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(bundle.getString("file-requester.images"), "*.png", "*.jpg", "*.jpeg", "*.jfif"));
			var selectedFile = fileChooser.showOpenDialog(getWindow(event));
			if (selectedFile != null)
			{
				fireEvent(new ImageSelectedEvent(selectedFile));
			}
		});

		var addFileItem = new MenuItem(bundle.getString("messaging.action.send-file"));
		addFileItem.setGraphic(new FontIcon(MaterialDesignA.ATTACHMENT));
		addFileItem.setOnAction(event -> {
			var fileChooser = new FileChooser();
			fileChooser.setTitle(bundle.getString("messaging.file-requester.send-file"));
			var selectedFile = fileChooser.showOpenDialog(getWindow(event));
			if (selectedFile != null)
			{
				fireEvent(new FileSelectedEvent(selectedFile));
			}
		});

		var contextMenu = new ContextMenu(addImageItem, addFileItem);
		addMedia.setOnContextMenuRequested(event -> contextMenu.show(addMedia, event.getScreenX(), event.getScreenY()));
		UiUtils.setOnPrimaryMouseClicked(addMedia, event -> contextMenu.show(addMedia, event.getScreenX(), event.getScreenY()));
	}
}
