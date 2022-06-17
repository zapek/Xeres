/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

import io.xeres.ui.JavaFxApplication;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;

import java.util.function.Consumer;

public class ContentUri implements Content
{
	private final Hyperlink node;

	public ContentUri(String uri)
	{
		node = new Hyperlink(uri);
		node.setOnAction(event -> JavaFxApplication.openUrl(node.getText()));
	}

	public ContentUri(String uri, String description, Consumer<String> action)
	{
		node = new Hyperlink(description);
		node.setOnAction(event -> action.accept(uri));
	}

	@Override
	public Node getNode()
	{
		return node;
	}

	public String getUri()
	{
		return node.getText();
	}
}
