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
import io.xeres.ui.support.util.TextFlowUtils;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.HitInfo;
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

	private final Node focusNode;

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

	private HitInfo firstHitInfo;
	private int startCellIndex;
	private int lastCellIndex;

	private SelectionMode selectionMode;

	private ChatListSelectRange selectRange;

	private Direction direction = Direction.SAME;

	private final List<TextFlow> textFlows = new LinkedList<>();

	public ChatListDragSelection(Node focusNode)
	{
		this.focusNode = focusNode;
	}

	public void press(MouseEvent e)
	{
		if (e.getEventType() != MouseEvent.MOUSE_PRESSED)
		{
			throw new IllegalArgumentException("Event must be a MOUSE_PRESSED event");
		}

		clearSelection();

		var virtualFlow = getVirtualFlow(e);
		virtualFlow.setCursor(Cursor.TEXT);
		var hitResult = virtualFlow.hit(e.getX(), e.getY());
		if (hitResult.isCellHit())
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
		}
	}

	public void drag(MouseEvent e)
	{
		if (e.getEventType() != MouseEvent.MOUSE_DRAGGED)
		{
			throw new IllegalArgumentException("Event must be a MOUSE_DRAGGED event");
		}

		var virtualFlow = getVirtualFlow(e);
		var hitResult = virtualFlow.hit(e.getX(), e.getY());
		if (hitResult.isCellHit())
		{
			var cellIndex = hitResult.getCellIndex();
			if (cellIndex < virtualFlow.getFirstVisibleIndex() || cellIndex > virtualFlow.getLastVisibleIndex())
			{
				return;
			}

			// XXX: this is not currently working (remove the above to have this be reachable)
			if (cellIndex <= virtualFlow.getFirstVisibleIndex())
			{
				virtualFlow.showAsFirst(cellIndex);
			}
			else if (cellIndex >= virtualFlow.getLastVisibleIndex())
			{
				virtualFlow.showAsLast(cellIndex);
			}

			if (!handleMultilineSelect(virtualFlow, hitResult))
			{
				handleSingleLineSelect(hitResult);
			}
		}
	}

	private boolean handleMultilineSelect(VirtualFlow<ChatLine, ChatListCell> virtualFlow, VirtualFlowHit<ChatListCell> hitResult)
	{
		var cellIndex = hitResult.getCellIndex();
		var textFlow = hitResult.getCell().getNode();

		if (cellIndex != startCellIndex)
		{
			if (direction == Direction.SAME)
			{
				// We're switching to multiline mode.
				var pathElements = textFlows.getFirst().rangeShape(getOffsetFromSelectionMode(), TextFlowUtils.getTextFlowCount(textFlows.getFirst()));
				showVisibleSelection(textFlows.getFirst(), pathElements);

				direction = cellIndex > startCellIndex ? Direction.DOWN : Direction.UP;
				markSelection(virtualFlow, startCellIndex, cellIndex);
			}
			else
			{
				markSelection(virtualFlow, lastCellIndex, cellIndex);
			}
			lastCellIndex = cellIndex;
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

	private void markSelection(VirtualFlow<ChatLine, ChatListCell> virtualFlow, int fromCell, int toCell)
	{
		switch (direction)
		{
			case UP ->
			{
				if (fromCell > toCell) // Going up (mark more)
				{
					for (int i = fromCell; i >= toCell; i--)
					{
						addVisibleSelection(virtualFlow.getCell(i).getNode());
					}
				}
				else if (fromCell < toCell) // Going down (unwind)
				{
					for (int i = fromCell; i < toCell; i++)
					{
						removeVisibleSelection(virtualFlow.getCell(i).getNode());
					}
				}
			}
			case DOWN ->
			{
				if (fromCell < toCell) // Going down (mark more)
				{
					for (int i = fromCell; i <= toCell; i++)
					{
						addVisibleSelection(virtualFlow.getCell(i).getNode());
					}
				}
				else if (fromCell > toCell) // Going up (unwind)
				{
					for (int i = fromCell; i > toCell; i--)
					{
						removeVisibleSelection(virtualFlow.getCell(i).getNode());
					}
				}
			}
			case null, default -> throw new IllegalArgumentException("Wrong direction: " + direction);
		}
	}

	private int getOffsetFromSelectionMode()
	{
		return switch (selectionMode)
		{
			case TIME_ACTION_AND_TEXT -> 0;
			case ACTION_AND_TEXT -> 1;
			case TEXT -> 2;
		};
	}

	private void handleSingleLineSelect(VirtualFlowHit<ChatListCell> hitResult)
	{
		var textFlow = hitResult.getCell().getNode();

		selectRange = new ChatListSelectRange(firstHitInfo, textFlow.hitTest(hitResult.getCellOffset()));

		if (selectRange.isSelected())
		{
			var pathElements = textFlow.rangeShape(selectRange.getStart(), selectRange.getEnd() + 1);
			showVisibleSelection(textFlow, pathElements);
		}
		else
		{
			hideVisibleSelection(textFlow);
		}
	}

	private void addVisibleSelection(TextFlow textFlow)
	{
		showVisibleSelection(textFlow, textFlow.rangeShape(getOffsetFromSelectionMode(), TextFlowUtils.getTextFlowCount(textFlow)));
		if (textFlows.getLast() != textFlow)
		{
			textFlows.add(textFlow);
		}
	}

	private static void showVisibleSelection(TextFlow textFlow, PathElement[] pathElements)
	{
		var path = new Path(pathElements);
		path.setStroke(Color.TRANSPARENT);
		path.setFill(Color.DODGERBLUE);
		path.setOpacity(0.3);
		path.setManaged(false); // This is needed so they show up above
		path.setTranslateX(8.0); // Margin
		hideVisibleSelection(textFlow);
		textFlow.getChildren().add(path);
	}

	private static void hideVisibleSelection(TextFlow textFlow)
	{
		if (textFlow.getChildren().getLast() instanceof Path)
		{
			textFlow.getChildren().removeLast();
		}
	}

	private void removeVisibleSelection(TextFlow textFlow)
	{
		hideVisibleSelection(textFlow);
		textFlows.remove(textFlow);
	}


	public void release(MouseEvent e)
	{
		if (e.getEventType() != MouseEvent.MOUSE_RELEASED)
		{
			throw new IllegalArgumentException("Event must be a MOUSE_RELEASED event");
		}

		var virtualFlow = getVirtualFlow(e);
		virtualFlow.setCursor(Cursor.DEFAULT);

		if (selectRange == null || !selectRange.isSelected())
		{
			clearSelection();
			selectRange = null;
		}

		if (focusNode != null)
		{
			focusNode.requestFocus();
		}
	}

	public void copy()
	{
		var text = getSelectionAsText();
		if (StringUtils.isNotBlank(text))
		{
			ClipboardUtils.copyTextToClipboard(text);
		}
	}

	public boolean isSelected()
	{
		return !textFlows.isEmpty();
	}

	private void clearSelection()
	{
		while (!textFlows.isEmpty())
		{
			var textFlow = textFlows.getLast();
			removeVisibleSelection(textFlow);
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
			// Single line selection
			var textFlow = textFlows.getFirst();

			assert textFlow.getChildren().size() >= 3;

			return TextFlowUtils.getTextFlowAsText(textFlow, selectRange.getStart(), selectRange.getEnd() + 1);
		}
		else
		{
			if (direction == Direction.UP)
			{
				return textFlows.reversed().stream()
						.map(textFlow -> TextFlowUtils.getTextFlowAsText(textFlow, getOffsetFromSelectionMode()))
						.collect(Collectors.joining("\n"));

			}
			else
			{
				return textFlows.stream()
						.map(textFlow -> TextFlowUtils.getTextFlowAsText(textFlow, getOffsetFromSelectionMode()))
						.collect(Collectors.joining("\n"));
			}
		}
	}

	private VirtualFlow<ChatLine, ChatListCell> getVirtualFlow(MouseEvent e)
	{
		//noinspection unchecked
		return (VirtualFlow<ChatLine, ChatListCell>) e.getSource();
	}
}
