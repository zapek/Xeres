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

package io.xeres.ui.support.contentline;

import javafx.scene.Node;
import javafx.scene.text.Text;

import java.util.Set;

public class ContentEmphasis implements Content
{
	public enum Style
	{
		BOLD,
		ITALIC
	}

	private final Text node;

	public ContentEmphasis(String text, Set<Style> style)
	{
		node = new Text(text);
		var css = "";
		if (style.contains(Style.BOLD))
		{
			css += "-fx-font-weight: bold;";
		}
		if (style.contains(Style.ITALIC))
		{
			css += "-fx-font-style: italic;";
		}
		if (!css.isEmpty())
		{
			node.setStyle(css);
		}
	}

	@Override
	public Node getNode()
	{
		return node;
	}
}
