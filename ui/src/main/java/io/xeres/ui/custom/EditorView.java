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

package io.xeres.ui.custom;

import io.xeres.ui.support.util.UiUtils;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class EditorView extends VBox
{
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
	private Button heading;

	@FXML
	private TextArea editor;

	public final ReadOnlyIntegerWrapper lengthProperty = new ReadOnlyIntegerWrapper();

	public EditorView()
	{
		var loader = new FXMLLoader(getClass().getResource("/view/custom/editorview.fxml")); // XXX: translation bundle? how?
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

	public void initialize()
	{
		bold.setOnAction(event -> surround("**"));
		italic.setOnAction(event -> surround("_"));
		code.setOnAction(event -> selection(() -> prefixLines("\t"), () -> surround("`")));
		quote.setOnAction(event -> prefixLines(">"));
		list.setOnAction(event -> insertNextLine("-"));
		heading.setOnAction(event -> insertNextLine("##"));
		hyperlink.setOnAction(event -> insertUrl(UiUtils.getWindow(event)));

		lengthProperty.bind(editor.lengthProperty());
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
			var pos = editor.getCaretPosition();
			if (isBeginningOfLine(pos))
			{
				editor.insertText(pos, text + " ");
			}
		}
		else
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
		editor.requestFocus();
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
		dialog.setTitle("Insert Hyperlink");
		dialog.setGraphic(null);
		dialog.setHeaderText("Enter URL");
		dialog.initOwner(parent);

		dialog.showAndWait().ifPresent(link -> {
			if (isNotBlank(link))
			{
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
}
