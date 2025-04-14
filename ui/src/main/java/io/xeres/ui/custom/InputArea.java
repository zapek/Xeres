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

import io.xeres.common.AppName;
import io.xeres.ui.support.util.UiUtils;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import net.harawata.appdirs.AppDirsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

public class InputArea extends TextArea
{
	private static final Logger log = LoggerFactory.getLogger(InputArea.class);

	private static final KeyCodeCombination CTRL_S = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);

	public InputArea()
	{
		this("");
	}

	public InputArea(String text)
	{
		super(text);

		addEventFilter(KeyEvent.KEY_PRESSED, this::handleInputKeys);
	}

	private void handleInputKeys(KeyEvent event)
	{
		if (CTRL_S.match(event))
		{
			if (handleStickers())
			{
				event.consume();
			}
		}
	}

	private boolean handleStickers()
	{
		var bounds = localToScreen(getBoundsInLocal());
		var popup = new Popup();
		var stickerView = new StickerView();
		popup.getContent().add(stickerView);
		popup.setAnchorX(bounds.getMinX());
		popup.setAnchorY(bounds.getMaxY());
		popup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_BOTTOM_LEFT);

		// Proxy the event to the InputArea
		stickerView.addEventHandler(StickerClickedEvent.STICKER_CLICKED, event -> {
			event.consume();
			fireEvent(new StickerClickedEvent(event.getPath()));
			popup.hide();
		});

		popup.show(UiUtils.getWindow(this));
		stickerView.loadStickers(Paths.get(AppDirsFactory.getInstance().getUserDataDir(AppName.NAME, null, null, true), "stickers"));
		popup.setAutoHide(true);
		return true;
	}
}
