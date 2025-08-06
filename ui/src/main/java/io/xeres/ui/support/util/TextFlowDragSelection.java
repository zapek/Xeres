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

package io.xeres.ui.support.util;

import io.micrometer.common.util.StringUtils;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.util.TextFlowUtils.Options;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.HitInfo;
import javafx.scene.text.TextFlow;

public class TextFlowDragSelection
{
	private final TextFlow textFlow;

	private HitInfo firstHitInfo;

	private TextSelectRange textSelectRange;

	public TextFlowDragSelection(TextFlow textFlow)
	{
		this.textFlow = textFlow;
	}

	public void press(MouseEvent e)
	{
		if (e.getEventType() != MouseEvent.MOUSE_PRESSED)
		{
			throw new IllegalArgumentException("Event must be a MOUSE_PRESSED event");
		}

		TextFlowUtils.hideSelection(textFlow);
		textFlow.setCursor(Cursor.TEXT);

		firstHitInfo = textFlow.hitTest(new Point2D(e.getX(), e.getY()));
	}

	public void drag(MouseEvent e)
	{
		if (e.getEventType() != MouseEvent.MOUSE_DRAGGED)
		{
			throw new IllegalArgumentException("Event must be a MOUSE_DRAGGED event");
		}

		if (firstHitInfo == null)
		{
			throw new IllegalStateException("press() wasn't called prior to drag()");
		}

		textSelectRange = new TextSelectRange(firstHitInfo, textFlow.hitTest(new Point2D(e.getX(), e.getY())));
		if (textSelectRange.isSelected())
		{
			var pathElements = textFlow.rangeShape(textSelectRange.getStart(), textSelectRange.getEnd() + 1);
			TextFlowUtils.showSelection(textFlow, pathElements, 0.0);
		}
		else
		{
			TextFlowUtils.hideSelection(textFlow);
		}
	}

	public void release(MouseEvent e)
	{
		if (e.getEventType() != MouseEvent.MOUSE_RELEASED)
		{
			throw new IllegalArgumentException("Event must be a MOUSE_RELEASED event");
		}

		textFlow.setCursor(Cursor.DEFAULT);

		if (textSelectRange == null || !textSelectRange.isSelected())
		{
			TextFlowUtils.hideSelection(textFlow);
			textSelectRange = null;
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
