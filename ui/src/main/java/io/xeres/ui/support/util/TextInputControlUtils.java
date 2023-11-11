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

package io.xeres.ui.support.util;

import io.xeres.common.AppName;
import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.rest.location.RSIdResponse;
import io.xeres.common.rsid.Type;
import io.xeres.ui.client.LocationClient;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputControl;

import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

import static io.xeres.common.dto.location.LocationConstants.OWN_LOCATION_ID;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class TextInputControlUtils
{
	private TextInputControlUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ContextMenu createInputContextMenu(TextInputControl textInputControl, LocationClient locationClient)
	{
		var contextMenu = new ContextMenu();

		contextMenu.getItems().addAll(createDefaultChatInputMenuItems(textInputControl));
		var pasteId = new MenuItem(I18nUtils.getString("chat.room.input.paste-id"));
		pasteId.setOnAction(event -> appendOwnId(textInputControl, locationClient));
		contextMenu.getItems().addAll(new SeparatorMenuItem(), pasteId);
		return contextMenu;
	}

	private static void appendOwnId(TextInputControl textInputControl, LocationClient locationClient)
	{
		var rsIdResponse = locationClient.getRSId(OWN_LOCATION_ID, Type.CERTIFICATE);
		rsIdResponse.subscribe(reply -> Platform.runLater(() -> textInputControl.appendText(buildRetroshareUrl(reply))));
	}

	private static String buildRetroshareUrl(RSIdResponse rsIdResponse)
	{
		var uri = URI.create("retroshare://certificate?" +
				"radix=" + URLEncoder.encode(rsIdResponse.rsId().replace("\n", ""), UTF_8) + // Removing the '\n' is in case this is a certificate which is sliced for presentation
				"&amp;name=" + URLEncoder.encode(rsIdResponse.name(), UTF_8) +
				"&amp;location=" + URLEncoder.encode(rsIdResponse.location(), UTF_8));
		return "<a href=\"" + uri + "\">" + AppName.NAME + " Certificate (" + rsIdResponse.name() + ", @" + rsIdResponse.location() + ")</a>";
	}

	private static List<MenuItem> createDefaultChatInputMenuItems(TextInputControl textInputControl)
	{
		var undo = new MenuItem(I18nUtils.getString("chat.room.input.undo"));
		undo.setOnAction(event -> textInputControl.undo());

		var redo = new MenuItem(I18nUtils.getString("chat.room.input.redo"));
		redo.setOnAction(event -> textInputControl.redo());

		var cut = new MenuItem(I18nUtils.getString("chat.room.input.cut"));
		cut.setOnAction(event -> textInputControl.cut());

		var copy = new MenuItem(I18nUtils.getString("chat.room.input.copy"));
		copy.setOnAction(event -> textInputControl.copy());

		var paste = new MenuItem(I18nUtils.getString("chat.room.input.paste"));
		paste.setOnAction(event -> textInputControl.paste());

		var delete = new MenuItem(I18nUtils.getString("chat.room.input.delete"));
		delete.setOnAction(event -> textInputControl.deleteText(textInputControl.getSelection()));

		var selectAll = new MenuItem(I18nUtils.getString("chat.room.input.select-all"));
		selectAll.setOnAction(event -> textInputControl.selectAll());

		var emptySelection = Bindings.createBooleanBinding(() -> textInputControl.getSelection().getLength() == 0, textInputControl.selectionProperty());

		cut.disableProperty().bind(emptySelection);
		copy.disableProperty().bind(emptySelection);
		delete.disableProperty().bind(emptySelection);

		var canUndo = Bindings.createBooleanBinding(() -> !textInputControl.isUndoable(), textInputControl.undoableProperty());
		var canRedo = Bindings.createBooleanBinding(() -> !textInputControl.isRedoable(), textInputControl.redoableProperty());

		undo.disableProperty().bind(canUndo);
		redo.disableProperty().bind(canRedo);

		return List.of(undo, redo, cut, copy, paste, delete, new SeparatorMenuItem(), selectAll);
	}
}
