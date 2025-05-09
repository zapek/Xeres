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

package io.xeres.ui.custom;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.client.LocationClient;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.contentline.Content;
import io.xeres.ui.support.contentline.ContentText;
import io.xeres.ui.support.markdown.MarkdownService;
import io.xeres.ui.support.markdown.UriAction;
import io.xeres.ui.support.util.TextInputControlUtils;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.util.image.ImageUtils;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class EditorView extends VBox
{
	private static final Logger log = LoggerFactory.getLogger(EditorView.class);

	private static final KeyCodeCombination PASTE_KEY = new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);
	private static final KeyCodeCombination ENTER_INSERT_KEY = new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);

	private static final int IMAGE_WIDTH_MAX = 640;
	private static final int IMAGE_HEIGHT_MAX = 480;
	private static final int IMAGE_MAXIMUM_SIZE = 31000; // Same as the one in chat

	private static final Pattern URL_DETECTOR = Pattern.compile("(^mailto:.*$|^\\p{Ll}.{1,30}://.*$)");

	@FXML
	private ToolBar toolBar;

	@FXML
	private Button bold;

	@FXML
	private Button italic;

	@FXML
	private Button hyperlink;

	@FXML
	private Button quote;

	@FXML
	private Button code;

	@FXML
	private Button list;

	@FXML
	private MenuButton heading;

	@FXML
	private MenuItem header1;

	@FXML
	private MenuItem header2;

	@FXML
	private MenuItem header3;

	@FXML
	private MenuItem header4;

	@FXML
	private MenuItem header5;

	@FXML
	private MenuItem header6;

	@FXML
	private ToggleButton preview;

	@FXML
	private TextArea editor;

	@FXML
	private ScrollPane previewPane;

	@FXML
	private TextFlow previewContent;

	private int typingCount;

	private MarkdownService markdownService;

	private final ResourceBundle bundle;

	public final ReadOnlyIntegerWrapper lengthProperty = new ReadOnlyIntegerWrapper();

	private final BooleanProperty previewOnly = new SimpleBooleanProperty(false);

	public EditorView()
	{
		bundle = I18nUtils.getBundle();

		var loader = new FXMLLoader(getClass().getResource("/view/custom/editorview.fxml"), bundle);
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
		bold.setOnAction(event -> surround("**"));
		italic.setOnAction(event -> surround("_"));
		code.setOnAction(event -> selection(() -> prefixLines("\t"), () -> surround("`")));
		quote.setOnAction(event -> prefixLines(">"));
		list.setOnAction(event -> insertNextLine("-"));
		header1.setOnAction(event -> insertNextLine("#"));
		header2.setOnAction(event -> insertNextLine("##"));
		header3.setOnAction(event -> insertNextLine("###"));
		header4.setOnAction(event -> insertNextLine("####"));
		header5.setOnAction(event -> insertNextLine("#####"));
		header6.setOnAction(event -> insertNextLine("######"));
		hyperlink.setOnAction(event -> insertUrl(UiUtils.getWindow(event)));

		editor.addEventFilter(KeyEvent.KEY_PRESSED, this::handleInputKeys);

		preview.setOnAction(event -> {
			if (preview.isSelected())
			{
				editor.setVisible(false);
				var contents = markdownService.parse(editor.getText(), EnumSet.noneOf(MarkdownService.ParsingMode.class), null);
				previewContent.getChildren().addAll(contents.stream()
						.map(Content::getNode).toList());
				previewPane.setVisible(true);
			}
			else
			{
				editor.setVisible(true);
				previewPane.setVisible(false);
				previewContent.getChildren().clear();
			}
		});

		lengthProperty.bind(editor.lengthProperty());

		previewOnly.addListener((observable, oldValue, newValue) -> {
			if (Boolean.TRUE.equals(newValue))
			{
				UiUtils.setAbsent(toolBar);
				editor.setVisible(false);
				previewPane.setVisible(true);
			}
			else
			{
				UiUtils.setPresent(toolBar);
				editor.setVisible(true);
				previewPane.setVisible(false);
			}
		});
	}

	public void setMarkdown(InputStream input, UriAction uriAction)
	{
		if (!previewOnly.get())
		{
			throw new IllegalStateException("Markdown file can only be set to an EditorView in previewOnly mode");
		}

		List<Content> contents;

		try
		{
			contents = markdownService.parse(new String(input.readAllBytes(), StandardCharsets.UTF_8), EnumSet.noneOf(MarkdownService.ParsingMode.class), uriAction);
		}
		catch (IOException e)
		{
			contents = List.of(new ContentText("Couldn't open markdown file " + input + " (" + e.getMessage() + ")"));
		}
		previewContent.getChildren().clear();
		previewContent.getChildren().addAll(contents.stream()
				.map(Content::getNode).toList());
	}

	/**
	 * Sets the markdown service. If it is set, then the EditorView automatically gets a preview button.
	 *
	 * @param markdownService the markdown service
	 */
	public void setMarkdownService(MarkdownService markdownService)
	{
		this.markdownService = markdownService;

		UiUtils.setPresent(preview);
	}

	public String getText()
	{
		return editor.getText();
	}

	public void setReply(String reply)
	{
		if (!reply.isBlank())
		{
			reply = "\n\n> " + reply.replace("\n", "\n> ");
			if (reply.endsWith("\n> "))
			{
				reply = reply.substring(0, reply.length() - 3);
			}
		}
		editor.setText(reply);
		editor.positionCaret(0);
		editor.requestFocus();
	}

	public void setInputContextMenu(LocationClient locationClient)
	{
		TextInputControlUtils.addEnhancedInputContextMenu(editor, locationClient, this::handlePaste);
	}

	public boolean isModified()
	{
		return typingCount >= 2;
	}

	public boolean isPreviewOnly()
	{
		return previewOnly.get();
	}

	public void setPreviewOnly(boolean previewOnly)
	{
		this.previewOnly.set(previewOnly);
	}

	private void surround(String text)
	{
		var selection = editor.getSelection();

		if (selection.getLength() <= 0)
		{
			var pos = editor.getCaretPosition();
			editor.insertText(pos, text + text);
			editor.positionCaret(pos + text.length());
		}
		else
		{
			var trailingSpace = editor.getText(selection.getEnd() - 1, selection.getEnd()).equals(" ");
			editor.insertText(selection.getStart(), text);
			var end = selection.getEnd() + text.length();
			if (trailingSpace)
			{
				end--;
			}
			editor.insertText(end, text);
			editor.positionCaret(end + text.length() + 1);
		}
		editor.requestFocus();
	}

	private void prefixLines(String text)
	{
		var selection = editor.getSelection();

		if (selection.getLength() <= 0)
		{
			prefixSingleLine(text);
		}
		else
		{
			prefixParagraph(text, selection);
		}
		editor.requestFocus();
	}

	private void prefixSingleLine(String text)
	{
		var pos = editor.getCaretPosition();
		if (isBeginningOfLine(pos))
		{
			editor.insertText(pos, text + " ");
		}
	}

	private void prefixParagraph(String text, IndexRange selection)
	{
		if (isParagraphBoundaries())
		{
			var start = selection.getStart();
			int end;

			while (start <= selection.getEnd())
			{
				end = editor.getText(start, selection.getEnd()).indexOf("\n");
				if (end == -1)
				{
					end = selection.getEnd();
				}
				else
				{
					end += start;
				}

				editor.insertText(start, text + (text.isBlank() ? "" : " ")); // spacing not needed for indentation or so
				editor.positionCaret(end + 2 + text.length());

				start = end + 2 + text.length();
			}
		}
	}

	private void insertNextLine(String text)
	{
		var selection = editor.getSelection();

		if (selection.getLength() <= 0)
		{
			var pos = editor.getCaretPosition();
			if (isBeginningOfLine(pos))
			{
				editor.insertText(pos, text + " ");
			}
			else
			{
				editor.insertText(pos, "\n" + text + " ");
			}
		}
		else
		{
			var selectedText = editor.getText(selection.getStart(), selection.getEnd());
			if (!selectedText.contains("\n") && selection.getEnd() == editor.getLength())
			{
				editor.insertText(selection.getStart(), "\n" + text + " ");
			}
		}
		editor.requestFocus();
	}

	private boolean isBeginningOfLine(int pos)
	{
		return pos == 0 || editor.getText(pos - 1, pos).equals("\n");
	}

	private void insertUrl(Window parent)
	{
		var selection = editor.getSelection();

		var dialog = new TextInputDialog();
		dialog.setTitle(bundle.getString("editor.hyperlink.insert"));
		dialog.setGraphic(new FontIcon(MaterialDesignL.LINK_VARIANT));
		dialog.setHeaderText(bundle.getString("editor.hyperlink.enter"));
		dialog.initOwner(parent);

		dialog.showAndWait().ifPresent(link -> {
			if (isNotBlank(link))
			{
				if (!URL_DETECTOR.matcher(link).matches())
				{
					link = "https://" + link;
				}
				if (selection.getLength() <= 0)
				{
					var pos = editor.getCaretPosition();

					editor.insertText(pos, "[](" + link + ")");
					editor.positionCaret(pos + 1);
				}
				else
				{
					editor.insertText(selection.getStart(), "[");
					editor.insertText(editor.getText(selection.getEnd(), selection.getEnd() + 1).equals(" ") ? selection.getEnd() : (selection.getEnd() + 1), "](" + link + ")");
				}
			}
			editor.requestFocus();
		});
	}

	private void selection(Runnable paragraph, Runnable subline)
	{
		var selection = editor.getSelection();

		if (selection.getLength() <= 0)
		{
			subline.run();
		}
		else
		{
			if (isParagraphBoundaries())
			{
				paragraph.run();
			}
			else
			{
				subline.run();
			}
		}
		editor.requestFocus();
	}

	private boolean isParagraphBoundaries()
	{
		var selection = editor.getSelection();

		var start = selection.getStart();
		var end = selection.getEnd();

		return (start == 0 || editor.getText(start - 1, start).equals("\n")) && (editor.getText(end - 1, end).equals("\n") || end == editor.getLength() || editor.getText(end, end + 1).equals("\n"));
	}

	private void handleInputKeys(KeyEvent event)
	{
		typingCount++;

		if (PASTE_KEY.match(event))
		{
			if (handlePaste(editor))
			{
				event.consume();
			}
		}
		else if (ENTER_INSERT_KEY.match(event))
		{
			completeStatement();
		}
	}

	private boolean handlePaste(TextInputControl textInputControl)
	{
		var object = ClipboardUtils.getSupportedObjectFromClipboard();
		return switch (object)
		{
			case Image image ->
			{
				var imageView = new ImageView(image);
				ImageUtils.limitMaximumImageSize(imageView, IMAGE_WIDTH_MAX * IMAGE_HEIGHT_MAX);

				var imgData = ImageUtils.writeImageAsJpegData(imageView.getImage(), IMAGE_MAXIMUM_SIZE);
				textInputControl.insertText(textInputControl.getCaretPosition(), "![](" + imgData + ")");

				yield true;
			}
			case String string ->
			{
				textInputControl.insertText(textInputControl.getCaretPosition(), string);
				yield true;
			}
			default -> false;
		};
	}

	/**
	 * Inserts a new line without cutting the current line.
	 */
	private void completeStatement()
	{
		var s = editor.getText(editor.getCaretPosition(), editor.getLength());
		var eol = s.indexOf('\n');
		if (eol == -1)
		{
			eol = s.length();
		}
		editor.insertText(editor.getCaretPosition() + eol, "\n");
	}
}
