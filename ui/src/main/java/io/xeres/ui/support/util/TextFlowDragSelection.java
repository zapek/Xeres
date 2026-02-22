/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

import io.micrometer.common.util.StringUtils;
import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.util.TextFlowUtils.Options;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.*;
import javafx.scene.text.HitInfo;
import javafx.scene.text.TextFlow;

public class TextFlowDragSelection
{
	private static final KeyCodeCombination COPY_KEY = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);

	private final TextFlow textFlow;

	private HitInfo firstHitInfo;

	private TextSelectRange textSelectRange;

	private ContextMenu contextMenu;

	/**
	 * Enables the selection.
	 *
	 * @param textFlow     the textflow to enable the selection for, a context menu with "Copy" is automatically added
	 * @param keyContainer the optional container (usually a pane) that can handle the key presses to enable CTRL-C, can be null
	 */
	public static void enableSelection(TextFlow textFlow, Node keyContainer)
	{
		var selection = new TextFlowDragSelection(textFlow);
		textFlow.addEventFilter(MouseEvent.MOUSE_PRESSED, selection::press);
		textFlow.addEventFilter(MouseEvent.MOUSE_DRAGGED, selection::drag);
		textFlow.addEventFilter(MouseEvent.MOUSE_RELEASED, selection::release);
		if (keyContainer != null)
		{
			keyContainer.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
				if (COPY_KEY.match(event))
				{
					selection.copy();
					event.consume();
				}
			});
		}
		var copyItem = new MenuItem(I18nUtils.getBundle().getString("copy"));
		copyItem.setOnAction(_ -> selection.copy());
		var contextMenu = new ContextMenu(copyItem);
		textFlow.setOnContextMenuRequested(event -> {
			if (selection.textSelectRange != null && selection.textSelectRange.isSelected())
			{
				contextMenu.show(textFlow, event.getScreenX(), event.getScreenY());
				event.consume();
			}
		});
		selection.setContextMenu(contextMenu);
	}

	public TextFlowDragSelection(TextFlow textFlow)
	{
		this.textFlow = textFlow;
	}

	private void setContextMenu(ContextMenu contextMenu)
	{
		this.contextMenu = contextMenu;
	}

	public void press(MouseEvent e)
	{
		if (e.getEventType() != MouseEvent.MOUSE_PRESSED)
		{
			throw new IllegalArgumentException("Event must be a MOUSE_PRESSED event");
		}

		if (e.getButton() == MouseButton.PRIMARY)
		{
			if (contextMenu != null)
			{
				contextMenu.hide();
			}
			TextFlowUtils.hideSelection(textFlow);
			textSelectRange = null;
			textFlow.setCursor(Cursor.TEXT);

			firstHitInfo = textFlow.getHitInfo(new Point2D(e.getX(), e.getY()));
		}
	}

	public void drag(MouseEvent e)
	{
		if (e.getEventType() != MouseEvent.MOUSE_DRAGGED)
		{
			throw new IllegalArgumentException("Event must be a MOUSE_DRAGGED event");
		}

		if (e.getButton() == MouseButton.PRIMARY)
		{
			if (firstHitInfo == null)
			{
				throw new IllegalStateException("press() wasn't called prior to drag()");
			}

			textSelectRange = new TextSelectRange(firstHitInfo, textFlow.getHitInfo(new Point2D(e.getX(), e.getY())));
			if (textSelectRange.isSelected())
			{
				var pathElements = textFlow.getRangeShape(textSelectRange.getStart(), textSelectRange.getEnd() + 1, false);
				TextFlowUtils.showSelection(textFlow, pathElements);
			}
			else
			{
				TextFlowUtils.hideSelection(textFlow);
			}
		}

	}

	public void release(MouseEvent e)
	{
		if (e.getEventType() != MouseEvent.MOUSE_RELEASED)
		{
			throw new IllegalArgumentException("Event must be a MOUSE_RELEASED event");
		}

		if (e.getButton() == MouseButton.PRIMARY)
		{
			textFlow.setCursor(Cursor.DEFAULT);

			if (textSelectRange == null || !textSelectRange.isSelected())
			{
				TextFlowUtils.hideSelection(textFlow);
				textSelectRange = null;
			}
		}
	}

	public void copy()
	{
		if (textSelectRange == null || !textSelectRange.isSelected())
		{
			return;
		}

		var text = TextFlowUtils.getTextFlowAsText(textFlow, textSelectRange.getStart(), textSelectRange.getEnd() + 1, Options.NONE);
		if (StringUtils.isNotBlank(text))
		{
			ClipboardUtils.copyTextToClipboard(text);
		}
	}
}
