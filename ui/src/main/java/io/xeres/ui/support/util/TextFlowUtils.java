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
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public final class TextFlowUtils
{
	private static final Logger log = LoggerFactory.getLogger(TextFlowUtils.class);

	private TextFlowUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Returns a text flow as a string.
	 *
	 * @param textFlow the text flow, not null
	 * @return the string, not null
	 */
	public static String getTextFlowAsText(TextFlow textFlow)
	{
		Objects.requireNonNull(textFlow);
		return getTextFlowAsText(textFlow, 0, getTextFlowCount(textFlow));
	}

	public static String getTextFlowAsText(TextFlow textFlow, int beginIndex)
	{
		Objects.requireNonNull(textFlow);
		return getTextFlowAsText(textFlow, beginIndex, getTextFlowCount(textFlow));
	}

	/**
	 * Returns a text flow as a string.
	 *
	 * @param textFlow   the text flow, not null
	 * @param beginIndex the beginning index, inclusive
	 * @param endIndex   the ending index, exclusive
	 * @return the string, not null
	 */
	public static String getTextFlowAsText(TextFlow textFlow, int beginIndex, int endIndex)
	{
		log.debug("beginIndex: {}, endIndex: {}", beginIndex, endIndex);

		// XXX: this is really a mess!

		Objects.requireNonNull(textFlow);
		var children = textFlow.getChildrenUnmodifiable();
		assert endIndex <= getTextFlowCount(textFlow);
		var s = new StringBuilder();
		var addSpace = false;
		var i = 0;
		var j = 0;

		while (i < endIndex)
		{
			var node = children.get(j);
			if (addSpace)
			{
				s.append(" ");
				addSpace = false;
			}

			var value = (switch (node)
			{
				case Label label ->
				{
					i++;
					yield label.getText();
				}
				case Text text ->
				{
					var t = text.getText();
					t = t.substring((beginIndex > 0 && beginIndex < i + t.length()) ? beginIndex - (i - 2) : 0, Math.min((endIndex - i), t.length()));
					i += text.getText().length();
					beginIndex = Math.max(0, beginIndex - (t.length() - 1));
					yield t;
				}
				case Hyperlink hyperlink ->
				{
					i++;
					yield hyperlink.getText();
				}
				case ImageView ignored ->
				{
					i++;
					yield "";
				}
				case Path ignored ->
				{ // ignore, it's the selection node
					i++;
					yield "";
				}
				default ->
				{
					i++;
					log.error("Unhandled node: {}", node);
					yield "";
				}
			});

			if (!value.isEmpty() && beginIndex == 0)
			{
				s.append(value);
				if (i < 3)
				{
					addSpace = true;
				}
			}
			beginIndex = Math.max(0, beginIndex - 1);
			j++;
		}
		return s.toString();
	}

	public static int getTextFlowCount(TextFlow textFlow)
	{
		Objects.requireNonNull(textFlow);
		var children = textFlow.getChildrenUnmodifiable();

		var total = 0;

		for (var node : children)
		{
			total += switch (node)
			{
				case Label ignored -> 1;
				case Text text -> text.getText().length();
				case Hyperlink ignored -> 1;
				case ImageView ignored -> 1;
				default -> 0;
			};
		}
		return total;
	}

	// XXX
	// The theory of operation is:
	// If node is below beginIndex, ignore it
	// if node is above endIndex, ignore it
	// if node is within beginIndex - endIndex, use it
	//   if it's a text node, we need to handle beginIndex and endIndex

	private static class Context
	{
		private List<Node> nodes;
		private int beginIndex;
		private int endIndex;

		private int currentNode = -1;

		public boolean hasNextNode()
		{
			return currentNode + 1 < nodes.size() && !(nodes.get(currentNode + 1) instanceof Path);
		}

		public Node nextNode()
		{
			currentNode++;
			var node = nodes.get(currentNode);

			beginIndex += switch (node)
			{
				case Label ignored -> 1;
				case Text text -> text.getText().length();
				case Hyperlink ignored -> 1;
				case ImageView ignored -> 1;
				default -> 0;
			};

			return node;
		}

//		public int beginIndexForNode()
//		{
//
//		}
//
//		public int endIndexForNode()
//		{
//
//		}
	}
}
