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

package io.xeres.ui.support.contentline;

import javafx.scene.Node;
import javafx.scene.text.Text;

public class ContentHeader implements Content
{
	private final Text node;

	public ContentHeader(String text, int size)
	{
		node = new Text(text);
		node.setStyle("-fx-font-size: " + getHeaderFontSize(size) + "px;");
	}

	@Override
	public Node getNode()
	{
		return node;
	}

	private static int getHeaderFontSize(int size)
	{
		return switch (size)
		{
			case 1 -> 32;
			case 2 -> 24;
			case 3 -> 18;
			case 4 -> 16;
			case 5 -> 13;
			case 6 -> 10;
			default -> throw new IllegalStateException("Header size " + size + " is bigger than the maximum of 6");
		};
	}
}
