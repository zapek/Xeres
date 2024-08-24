/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.custom.DisclosedHyperlink;
import io.xeres.ui.support.uri.UriService;
import io.xeres.ui.support.util.UiUtils;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;

import java.text.MessageFormat;
import java.util.function.Consumer;

public class ContentUri implements Content
{
	private final Hyperlink node;

	public ContentUri(String uri)
	{
		node = new Hyperlink(uri);
		node.setOnAction(event -> UriService.openUri(appendMailToIfNeeded(node.getText())));
	}

	public ContentUri(String uri, String description)
	{
		node = new DisclosedHyperlink(description, uri);
		node.setOnAction(event -> askBeforeOpeningIfNeeded(() -> UriService.openUri(appendMailToIfNeeded(uri))));
	}

	public ContentUri(String uri, String description, Consumer<String> action)
	{
		node = new DisclosedHyperlink(description, uri);
		node.setOnAction(event -> askBeforeOpeningIfNeeded(() -> action.accept(uri)));
	}

	private void askBeforeOpeningIfNeeded(Runnable action)
	{
		if (((DisclosedHyperlink) node).isMalicious())
		{
			UiUtils.alertConfirm(MessageFormat.format(I18nUtils.getString("uri.malicious-link.confirm"), ((DisclosedHyperlink) node).getUri()), action);
		}
		else
		{
			action.run();
		}
	}

	private static String appendMailToIfNeeded(String uri)
	{
		if (uri.contains("@") && !uri.contains("://"))
		{
			return "mailto:" + uri;
		}
		return uri;
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
