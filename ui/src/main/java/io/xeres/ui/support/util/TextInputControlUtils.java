/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.rest.location.RSIdResponse;
import io.xeres.common.rsid.Type;
import io.xeres.ui.client.LocationClient;
import io.xeres.ui.support.uri.CertificateUriFactory;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputControl;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.*;

import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static io.xeres.common.dto.location.LocationConstants.OWN_LOCATION_ID;

public final class TextInputControlUtils
{
	private static final ResourceBundle bundle = I18nUtils.getBundle();

	private static final Pattern CODE_PATTERN = Pattern.compile(
			"(?mi)\\b(class|interface|enum|package|import|public|private|protected|static|final|synchronized|volatile|abstract|def|function|var|let|const|using|namespace)\\b"
					+ "|\\b(if|for|while|switch|case|return)\\b"
					+ "|\\w+\\s*\\([^)]*\\)\\s*\\{"
					+ "|=>|->|#include\\s*<|System\\.out\\.println|console\\.log\\(|printf\\(|std::",
			Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

	private static final Pattern SQL_PATTERN = Pattern.compile(
			"\\b(SELECT\\s+.+?FROM|INSERT\\s+INTO|UPDATE\\s+\\w+\\s+SET|DELETE\\s+FROM|CREATE\\s+TABLE|ALTER\\s+TABLE|DROP\\s+TABLE|WHERE\\s+[^=]++=)\\b",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL
	);


	private TextInputControlUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Adds an enhanced input context menu to TextField or TextArea. It features icons and an optional
	 * "Paste own ID" menu item.
	 *
	 * @param textInputControl the text input control
	 * @param locationClient   the location client, if null, then there will be no "Paste own ID" menu item
	 * @param pasteAction      the action on paste, if null, there will be no action performed
	 */
	public static void addEnhancedInputContextMenu(TextInputControl textInputControl, LocationClient locationClient, Consumer<TextInputControl> pasteAction)
	{
		var contextMenu = new ContextMenu();

		contextMenu.getItems().addAll(createDefaultChatInputMenuItems(textInputControl, pasteAction));
		if (locationClient != null)
		{
			var pasteId = new MenuItem(bundle.getString("paste-id"));
			pasteId.setGraphic(new FontIcon(MaterialDesignC.CARD_ACCOUNT_DETAILS));
			pasteId.setOnAction(_ -> appendOwnId(textInputControl, locationClient));
			contextMenu.getItems().addAll(new SeparatorMenuItem(), pasteId);
		}
		textInputControl.setContextMenu(contextMenu);
	}

	public static void pasteGuessedContent(TextInputControl textInputControl, String content)
	{
		if (textInputControl.getText().isBlank())
		{
			//noinspection StatementWithEmptyBody
			if (isUri(content))
			{
				// Don't do anything
			}
			else if (isSourceCode(content))
			{
				content = "```\n" + content + "\n```";
			}
			else if (isCitation(content))
			{
				content = "> " + content;
			}
		}
		textInputControl.insertText(textInputControl.getCaretPosition(), content);
	}

	private static void appendOwnId(TextInputControl textInputControl, LocationClient locationClient)
	{
		var rsIdResponse = locationClient.getRSId(OWN_LOCATION_ID, Type.CERTIFICATE);
		rsIdResponse.subscribe(reply -> Platform.runLater(() -> textInputControl.appendText(buildRetroshareUrl(reply))));
	}

	private static String buildRetroshareUrl(RSIdResponse rsIdResponse)
	{
		var cleanCert = rsIdResponse.rsId().replace("\n", ""); // Removing the '\n' is in case this is a certificate which is sliced for presentation
		return CertificateUriFactory.generate(cleanCert, rsIdResponse.name(), rsIdResponse.location());
	}

	private static List<MenuItem> createDefaultChatInputMenuItems(TextInputControl textInputControl, Consumer<TextInputControl> pasteAction)
	{
		var undo = new MenuItem(bundle.getString("undo"));
		undo.setGraphic(new FontIcon(MaterialDesignU.UNDO_VARIANT));
		undo.setOnAction(_ -> textInputControl.undo());

		var redo = new MenuItem(bundle.getString("redo"));
		redo.setGraphic(new FontIcon(MaterialDesignR.REDO_VARIANT));
		redo.setOnAction(_ -> textInputControl.redo());

		var cut = new MenuItem(bundle.getString("cut"));
		cut.setGraphic(new FontIcon(MaterialDesignC.CONTENT_CUT));
		cut.setOnAction(_ -> textInputControl.cut());

		var copy = new MenuItem(bundle.getString("copy"));
		copy.setGraphic(new FontIcon(MaterialDesignC.CONTENT_COPY));
		copy.setOnAction(_ -> textInputControl.copy());

		var paste = new MenuItem(bundle.getString("paste"));
		paste.setGraphic(new FontIcon(MaterialDesignC.CONTENT_PASTE));
		paste.setOnAction(_ -> {
			if (pasteAction != null)
			{
				pasteAction.accept(textInputControl);
			}
			else
			{
				textInputControl.paste();
			}
		});

		var delete = new MenuItem(bundle.getString("delete"));
		delete.setGraphic(new FontIcon(MaterialDesignT.TRASH_CAN));
		delete.setOnAction(_ -> textInputControl.deleteText(textInputControl.getSelection()));

		var selectAll = new MenuItem(bundle.getString("select-all"));
		selectAll.setGraphic(new FontIcon(MaterialDesignS.SELECT_ALL));
		selectAll.setOnAction(_ -> textInputControl.selectAll());

		var emptySelection = Bindings.createBooleanBinding(() -> textInputControl.getSelection().getLength() == 0, textInputControl.selectionProperty());

		cut.disableProperty().bind(emptySelection);
		copy.disableProperty().bind(emptySelection);
		delete.disableProperty().bind(emptySelection);

		var emptyText = Bindings.createBooleanBinding(() -> textInputControl.getLength() == 0, textInputControl.textProperty());

		selectAll.disableProperty().bind(emptyText);

		var canUndo = Bindings.createBooleanBinding(() -> !textInputControl.isUndoable(), textInputControl.undoableProperty());
		var canRedo = Bindings.createBooleanBinding(() -> !textInputControl.isRedoable(), textInputControl.redoableProperty());

		undo.disableProperty().bind(canUndo);
		redo.disableProperty().bind(canRedo);

		return List.of(undo, redo, cut, copy, paste, delete, new SeparatorMenuItem(), selectAll);
	}

	// Visible for testing
	static boolean isSourceCode(String text)
	{
		String trimmed = text.trim();

		// Fenced code block (Markdown), but not if it's at the start
		if (trimmed.contains("```"))
		{
			return !trimmed.startsWith("```");
		}

		// Multi-line indented block (typical pasted code)
		String[] lines = trimmed.split("\\R");
		var indentedLines = 0;
		for (String line : lines)
		{
			if (line.startsWith("    ") || line.startsWith("\t"))
			{
				indentedLines++;
			}
		}
		if (indentedLines >= 2)
		{
			return true;
		}

		// Common language keywords, function/method signatures and includes/prints
		if (CODE_PATTERN.matcher(trimmed).find())
		{
			return true;
		}

		if (SQL_PATTERN.matcher(trimmed).find())
		{
			return true;
		}

		// Semicolon frequency (common in C/Java/JS) and presence of multiple lines
		long semicolons = trimmed.chars().filter(c -> c == ';').count();
		if (semicolons >= 2 && trimmed.contains("\n"))
		{
			return true;
		}

		// Symbol density heuristic (braces, parentheses, angle brackets, equals, hashes, etc.)
		var symbols = ";{}()[]<>#=\\*%+-|";
		long specialCount = trimmed.chars().filter(c -> symbols.indexOf(c) >= 0).count();
		double density = (double) specialCount / Math.max(1, trimmed.length());
		if (density > 0.03 && trimmed.contains("\n"))
		{
			return true;
		}
		return false;
	}

	// Visible for testing
	static boolean isCitation(String text)
	{
		return text.trim().length() >= 30;
	}

	static boolean isUri(String text)
	{
		var trimmed = text.trim();
		return trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("retroshare://");
	}
}
