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

package io.xeres.ui.controller.chat;

import io.xeres.ui.support.chat.ChatLine;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.fxmisc.flowless.VirtualFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

class ChatListDragSelection
{
	private static final Logger log = LoggerFactory.getLogger(ChatListDragSelection.class);

	private TextFlow firstTextFlow;
	private HitInfo firstHitInfo;
	private Path firstPath;

	public void press(MouseEvent e)
	{
		if (e.getEventType() != MouseEvent.MOUSE_PRESSED)
		{
			throw new IllegalArgumentException("Event must be a MOUSE_PRESSED event");
		}

		log.debug("Start {}, X: {}, Y: {}", e.getSource(), e.getX(), e.getY());
		var virtualFlow = (VirtualFlow<ChatLine, ChatListCell>) e.getSource();
		var hitResult = virtualFlow.hit(e.getX(), e.getY());
		if (hitResult.isCellHit()) // XXX: handle the other cases
		{
			var textFlow = hitResult.getCell().getNode();
			firstTextFlow = textFlow;

			var hitInfo = textFlow.hitTest(hitResult.getCellOffset());
			firstHitInfo = hitInfo;

			log.debug("TextFlow: {}, char index: {} (leading: {})", textFlow, hitInfo.getCharIndex(), hitInfo.isLeading());
		}
	}

	public void drag(MouseEvent e)
	{
		if (e.getEventType() != MouseEvent.MOUSE_DRAGGED)
		{
			throw new IllegalArgumentException("Event must be a MOUSE_DRAGGED event");
		}
		log.debug("Drag {}, X: {}, Y: {}", e.getSource(), e.getX(), e.getY());

		var virtualFlow = (VirtualFlow<ChatLine, ChatListCell>) e.getSource();
		var hitResult = virtualFlow.hit(e.getX(), e.getY());
		if (hitResult.isCellHit())
		{
			var textFlow = hitResult.getCell().getNode();
			if (textFlow != firstTextFlow)
			{
				return; // XXX
			}

			var hitInfo = textFlow.hitTest(hitResult.getCellOffset());

			int start = Math.min(firstHitInfo.getCharIndex(), hitInfo.getCharIndex());
			var count = countContent(textFlow.getChildren());
			int end = Math.max(firstHitInfo.getCharIndex(), hitInfo.getCharIndex());
			if (end == count - 1)
			{
				end++;
			}

			// XXX: offset 0 is time, 1 is nickname/action then 2+ is the text, one char per text
			log.debug("*** start: {}, end: {}, leading: {}, count: {}", start, end, hitInfo.isLeading(), count);

			var pathElements = textFlow.rangeShape(start, end);
			var path = new Path(pathElements);
			path.setStroke(Color.TRANSPARENT);
			path.setFill(Color.DODGERBLUE);
			path.setOpacity(0.3);
			path.setManaged(false); // This is needed so they show up above
			path.setTranslateX(8.0); // Margin
			// XXX: I don't know why the last character can't be selected. seems the end index is not incremented somehow (should be one more)
			if (firstPath != null)
			{
				textFlow.getChildren().remove(firstPath);
			}
			textFlow.getChildren().add(path); // XXX: correct? I think so too! but then we need to remove/add it on each update don't we?
			firstPath = path;

			var output = textFlow.getChildren().stream()
					.map(ChatListDragSelection::getContentAsText) // XXX: get the text... also need to get the right offset. Label have 1 and ContentText have their length taken into account
					.collect(Collectors.joining(" "));

			// XXX: need to tweak the output (ie, 0 is nothing, 1 is time, 2 is time + action), etc... and this all depends on start end too!

			// XXX: there's Text.hitTest() too, if easier...

			log.debug("Output: {}", output.substring(0, output.length() - (count - end) - 1));
			ClipboardUtils.copyTextToClipboard(output);
		}
	}

	private static String getContentAsText(Node node)
	{
		return switch (node)
		{
			case Label label -> label.getText();
			case Text text -> text.getText();
			case Hyperlink hyperlink -> hyperlink.getText();
			case ImageView ignored -> "";
			case Path ignored -> ""; // ignore, it's the selection node
			default ->
			{
				log.error("Unhandled node: {}", node);
				yield " ";
			}
		};
	}

	private static int countContent(List<Node> nodes)
	{
		int total = 0;

		for (var node : nodes)
		{
			total += switch (node)
			{
				case Label ignored -> 1;
				case Text text -> text.getText().length();
				case Hyperlink hyperlink -> hyperlink.getText().length();
				case ImageView ignored -> 1;
				default -> 0;
			};
		}
		return total;
	}

	public void release(MouseEvent e)
	{
		if (e.getEventType() != MouseEvent.MOUSE_RELEASED)
		{
			throw new IllegalArgumentException("Event must be a MOUSE_RELEASED event");
		}
		log.debug("Release {}, X: {}, Y: {}", e.getSource(), e.getX(), e.getY());

		if (firstPath != null)
		{
			firstTextFlow.getChildren().remove(firstPath);
		}

		firstPath = null;
		firstHitInfo = null;
		firstTextFlow = null;
	}
}
