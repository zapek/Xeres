/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class ContentText implements Content
{
	private final Text node;

	public ContentText(String text)
	{
		node = new Text(text);
	}

	public ContentText(String text, int quoteLevel)
	{
		this(text);

		switch (quoteLevel)
		{
			case 1 -> node.setFill(Color.GREEN);
			case 2 -> node.setFill(Color.BLUE);
			case 3 -> node.setFill(Color.RED);
			case 4 -> node.setFill(Color.MAGENTA);
			case 5 -> node.setFill(Color.YELLOW);
			case 6 -> node.setFill(Color.CYAN);
		}
		if (quoteLevel > 6)
		{
			node.setFill(Color.GRAY);
		}

	}

	@Override
	public Node getNode()
	{
		return node;
	}

	@Override
	public String asText()
	{
		return node.getText();
	}
}
