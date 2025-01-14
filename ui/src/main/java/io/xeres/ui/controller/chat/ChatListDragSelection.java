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

import io.micrometer.common.util.StringUtils;
import io.xeres.ui.support.chat.ChatLine;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualFlowHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

class ChatListDragSelection
{
	private static final Logger log = LoggerFactory.getLogger(ChatListDragSelection.class);

	private enum SelectionMode
	{
		TEXT,
		ACTION_AND_TEXT,
		TIME_ACTION_AND_TEXT
	}

	private enum Direction
	{
		SAME,
		DOWN,
		UP
	}

	// XXX: remember, this is only needed if we only have ONE textflow... for the other cases, it's SelectionMode full lines!
	private HitInfo firstHitInfo;
	private HitInfo lastHitInfo;
	private int startCellIndex;

	private SelectionMode selectionMode;

	private ChatListSelectRange selectRange;

	private Direction direction = Direction.SAME;

	private final List<TextFlow> textFlows = new LinkedList<>();

	public void press(MouseEvent e)
	{
		if (e.getEventType() != MouseEvent.MOUSE_PRESSED)
		{
			throw new IllegalArgumentException("Event must be a MOUSE_PRESSED event");
		}

		log.debug("Start {}, X: {}, Y: {}", e.getSource(), e.getX(), e.getY());
		var virtualFlow = getVirtualFlow(e);
		virtualFlow.setCursor(Cursor.TEXT);
		var hitResult = virtualFlow.hit(e.getX(), e.getY());
		if (hitResult.isCellHit()) // XXX: handle the other cases
		{
			var textFlow = hitResult.getCell().getNode();
			startCellIndex = hitResult.getCellIndex();
			textFlows.add(textFlow);

			var hitInfo = textFlow.hitTest(hitResult.getCellOffset());
			firstHitInfo = hitInfo;

			switch (hitInfo.getCharIndex())
			{
				case 0 -> selectionMode = SelectionMode.TIME_ACTION_AND_TEXT;
				case 1 -> selectionMode = SelectionMode.ACTION_AND_TEXT;
				default -> selectionMode = SelectionMode.TEXT;
			}

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

		var virtualFlow = getVirtualFlow(e);
		var hitResult = virtualFlow.hit(e.getX(), e.getY());
		if (hitResult.isCellHit())
		{
			if (!handleMultilineSelect(hitResult))
			{
				handleSingleLineSelect(hitResult);
			}
		}
	}

	private boolean handleMultilineSelect(VirtualFlowHit<ChatListCell> hitResult)
	{
		var cellIndex = hitResult.getCellIndex();
		var textFlow = hitResult.getCell().getNode();

		if (cellIndex != startCellIndex)
		{
			if (direction == Direction.SAME)
			{
				// We're switching to multiline mode.
				var pathElements = textFlows.getFirst().rangeShape(0, countContent(textFlows.getFirst().getChildren())); // XXX: use the mode
				showVisibleSelection(textFlows.getFirst(), pathElements);

				// XXX: check if it's possible to "skip" selection when selecting quickly... yes, it is when moving very quickly :( need to write loops

				direction = cellIndex > startCellIndex ? Direction.DOWN : Direction.UP;
				showVisibleSelection(textFlow, textFlow.rangeShape(0, countContent(textFlow.getChildren())));
				textFlows.add(textFlow);
			}
			else if (direction == Direction.DOWN)
			{
				if (cellIndex > startCellIndex)
				{
					showVisibleSelection(textFlow, textFlow.rangeShape(0, countContent(textFlow.getChildren())));
					textFlows.add(textFlow);
				}
				else
				{
					// XXX: we don't unwind immediately because we use startCellIndex. we should store an intermediate one...
					clearVisibleSelection(textFlow);
					textFlows.remove(textFlow);
				}
			}
			else if (direction == Direction.UP)
			{
				if (cellIndex < startCellIndex)
				{
					showVisibleSelection(textFlow, textFlow.rangeShape(0, countContent(textFlow.getChildren())));
					textFlows.add(textFlow);
				}
				else
				{
					clearVisibleSelection(textFlow);
					textFlows.remove(textFlow);
				}
			}
			return true;
		}
		else
		{
			if (direction != Direction.SAME)
			{
				// We're coming back to single line mode.
				clearSelection();
				direction = Direction.SAME;
				textFlows.add(textFlow);
			}
			return false;
		}
	}

	private void handleSingleLineSelect(VirtualFlowHit<ChatListCell> hitResult)
	{
		var textFlow = hitResult.getCell().getNode();

		lastHitInfo = textFlow.hitTest(hitResult.getCellOffset());

		selectRange = new ChatListSelectRange(firstHitInfo, lastHitInfo);

		// XXX: offset 0 is time, 1 is nickname/action then 2+ is the text, one char per text. hyperlink is 1, image is 1... sigh

		var pathElements = textFlow.rangeShape(selectRange.getStart(), selectRange.getEnd());
		showVisibleSelection(textFlow, pathElements);
	}

	private static void showVisibleSelection(TextFlow textFlow, PathElement[] pathElements)
	{
		var path = new Path(pathElements);
		path.setStroke(Color.TRANSPARENT);
		path.setFill(Color.DODGERBLUE);
		path.setOpacity(0.3);
		path.setManaged(false); // This is needed so they show up above
		path.setTranslateX(8.0); // Margin
		if (textFlow.getChildren().getLast() instanceof Path)
		{
			textFlow.getChildren().removeLast();
		}
		textFlow.getChildren().add(path);
	}

	private static void clearVisibleSelection(TextFlow textFlow)
	{
		if (textFlow.getChildren().getLast() instanceof Path)
		{
			textFlow.getChildren().removeLast();
		}
	}


	public void release(MouseEvent e)
	{
		if (e.getEventType() != MouseEvent.MOUSE_RELEASED)
		{
			throw new IllegalArgumentException("Event must be a MOUSE_RELEASED event");
		}
		log.debug("Release {}, X: {}, Y: {}", e.getSource(), e.getX(), e.getY());

		var virtualFlow = getVirtualFlow(e);
		virtualFlow.setCursor(Cursor.DEFAULT);

		// XXX: don't do that here! it has to be when either we stop or hit ctrl - c
		var text = getSelectionAsText();
		if (StringUtils.isNotBlank(text))
		{
			log.debug("Copying to clipboard: [{}]", text);
			ClipboardUtils.copyTextToClipboard(text);
		}

		clearSelection();

		firstHitInfo = null;
	}

	private void clearSelection()
	{
		while (!textFlows.isEmpty())
		{
			var textFlow = textFlows.removeLast();
			clearVisibleSelection(textFlow);
		}
	}

	private String getSelectionAsText()
	{
		if (textFlows.isEmpty())
		{
			return "";
		}

		if (textFlows.size() == 1)
		{
			var textFlow = textFlows.getFirst();

			assert textFlow.getChildren().size() >= 3;

			String time = getContentAsText(textFlow.getChildren().getFirst());
			String action = getContentAsText(textFlow.getChildren().get(1));
			String text = textFlow.getChildren().stream()
					.skip(2)
					.map(ChatListDragSelection::getContentAsText) // This is wrong. a hyper link has size 1 (like a label or an image) but its text is usually bigger
					.collect(Collectors.joining(" "));

			var sb = new StringBuilder();

			if (selectRange.getStart() == 0 && selectRange.getEnd() > 0)
			{
				sb.append(time);
			}
			if (selectRange.getStart() <= 1 && selectRange.getEnd() > 1)
			{
				if (!sb.isEmpty())
				{
					sb.append(" ");
				}
				sb.append(action);
			}
			if (selectRange.getStart() <= 2 && selectRange.getEnd() > 2)
			{
				if (!sb.isEmpty())
				{
					sb.append(" ");
				}

				var start = Math.max(0, selectRange.getStart() - 2);
				var end = selectRange.getEnd() - 2;

				if (hasHyperlinks(textFlow.getChildren())) // This is a bit fo a hack because hyperlinks count as 1 and not their number of chars
				{
					sb.append(text);
				}
				else
				{
					sb.append(text, start, end);
				}
			}
			return sb.toString();
		}
		else
		{
			// XXX: handle multiple text flows!
			return "";
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

	private static boolean hasHyperlinks(List<Node> nodes)
	{
		for (var node : nodes)
		{
			if (node instanceof Hyperlink)
			{
				return true;
			}
		}
		return false;
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
				case Hyperlink hyperlink -> 1;//hyperlink.getText().length();
				case ImageView ignored -> 1;
				default -> 0;
			};
		}
		return total;
	}

	private VirtualFlow<ChatLine, ChatListCell> getVirtualFlow(MouseEvent e)
	{
		//noinspection unchecked
		return (VirtualFlow<ChatLine, ChatListCell>) e.getSource();
	}
}
