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

import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;
import java.util.Objects;

public final class TextFlowUtils
{
	private TextFlowUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public enum Options
	{
		NONE,
		SPACED_PREFIXES // This lacks flexibility but will do for now
	}

	/**
	 * Returns a text flow as a string.
	 *
	 * @param textFlow   the text flow, not null
	 * @param beginIndex the beginning index, inclusive
	 * @param options    the options
	 * @return the string, not null
	 */
	public static String getTextFlowAsText(TextFlow textFlow, int beginIndex, Options options)
	{
		Objects.requireNonNull(textFlow);
		return getTextFlowAsText(textFlow, beginIndex, getTextFlowCount(textFlow), options);
	}

	/**
	 * Returns a text flow as a string.
	 *
	 * @param textFlow   the text flow, not null
	 * @param beginIndex the beginning index, inclusive
	 * @param endIndex   the ending index, exclusive
	 * @param options    the options
	 * @return the string, not null
	 */
	public static String getTextFlowAsText(TextFlow textFlow, int beginIndex, int endIndex, Options options)
	{
		var context = new Context(textFlow.getChildrenUnmodifiable(), beginIndex, endIndex, options == Options.SPACED_PREFIXES ? 2 : 0);
		return context.getText();
	}

	/**
	 * Calculates the length of a textflow.
	 * <p>Note: only {@link Text} has a length equal to the characters it contains, the other nodes return 1.
	 *
	 * @param textFlow the textflow
	 * @return the length of the textflow
	 */
	public static int getTextFlowCount(TextFlow textFlow)
	{
		Objects.requireNonNull(textFlow);
		var children = textFlow.getChildrenUnmodifiable();

		var total = 0;

		for (var node : children)
		{
			total += getTotalSize(node);
		}
		return total;
	}

	/**
	 * Shows the selected text visually.
	 *
	 * @param textFlow     the text flow
	 * @param pathElements the path elements, retrieved with {@link TextFlow#getRangeShape(int, int, boolean)}.
	 */
	public static void showSelection(TextFlow textFlow, PathElement[] pathElements)
	{
		var path = new Path(pathElements);
		path.setStroke(Color.TRANSPARENT);
		path.setFill(Color.DODGERBLUE);
		path.setOpacity(0.3);
		path.setManaged(false); // This is needed so they show up above
		hideSelection(textFlow);
		textFlow.getChildren().add(path);
	}

	/**
	 * Visually hides all the selected text.
	 *
	 * @param textFlow the text flow
	 */
	public static void hideSelection(TextFlow textFlow)
	{
		if (textFlow.getChildren().getLast() instanceof Path)
		{
			textFlow.getChildren().removeLast();
		}
	}

	private static int getTotalSize(Node node)
	{
		return switch (node)
		{
			case Label ignored -> 1;
			case Text text -> text.getText().length();
			case Hyperlink ignored -> 1;
			case ImageView ignored -> 1;
			case Path ignored -> 0; // We don't account for that one because it's for marking selected text, and it's always at the end
			default -> throw new IllegalStateException("Unhandled node: " + node);
		};
	}

	/**
	 * Little helper class to keep track of the context when walking the flow.
	 */
	private static class Context
	{
		private final List<Node> nodes;
		private final int beginIndex;
		private final int endIndex;
		private final int prefixNeedingSpace;
		private int currentIndex;

		private int currentNode = -1;

		public Context(List<Node> nodes, int beginIndex, int endIndex, int prefixNeedingSpace)
		{
			this.nodes = nodes;
			this.beginIndex = beginIndex;
			this.endIndex = endIndex;
			this.prefixNeedingSpace = prefixNeedingSpace;
		}

		public String getText()
		{
			var sb = new StringBuilder();
			while (hasNextNode())
			{
				if (needsSpace() && !sb.isEmpty())
				{
					sb.append(" ");
				}
				sb.append(processNextNode());
			}
			return sb.toString();
		}

		private boolean hasNextNode()
		{
			return currentNode + 1 < nodes.size() && !(nodes.get(currentNode + 1) instanceof Path);
		}

		private boolean needsSpace()
		{
			return currentNode < prefixNeedingSpace;
		}

		private String processNextNode()
		{
			currentNode++;
			var node = nodes.get(currentNode);

			var size = getTotalSize(node);

			if (currentIndex + size <= beginIndex)
			{
				currentIndex += size;
				return "";
			}
			if (currentIndex >= endIndex)
			{
				currentIndex += size;
				return "";
			}

			switch (node)
			{
				case Label label ->
				{
					currentIndex += size;
					return label.getText();
				}
				case Hyperlink hyperlink ->
				{
					currentIndex += size;
					return hyperlink.getText();
				}
				case ImageView image ->
				{
					currentIndex += size;
					var imageUserData = image.getUserData();
					return imageUserData != null ? (String) imageUserData : "";
				}
				case Path _ ->
				{
					return "";
				}
				case Text text ->
				{
					var start = 0;
					var end = text.getText().length();
					if (beginIndex >= currentIndex && beginIndex < currentIndex + size)
					{
						start = beginIndex - currentIndex;
					}
					if (endIndex <= currentIndex + size) // endIndex is always past currentIndex, see above
					{
						end = endIndex - currentIndex;
					}
					currentIndex += text.getText().length(); // We don't use end because that way we'll break out of the next run
					return text.getText().substring(start, end);
				}
				default -> throw new IllegalStateException("Unhandled node: " + node);
			}
		}
	}
}
