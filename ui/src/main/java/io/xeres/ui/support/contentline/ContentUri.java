/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.uri.UriService;
import io.xeres.ui.support.util.UiUtils;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ContentUri implements Content
{
	private final Hyperlink node;
	private static final ContextMenu contextMenu;

	private static final ResourceBundle bundle = I18nUtils.getBundle();

	static
	{
		var copyMenuItem = new MenuItem(bundle.getString("copy"));
		copyMenuItem.setGraphic(new FontIcon(MaterialDesignC.CONTENT_COPY));
		copyMenuItem.setOnAction(ContentUri::copyToClipboard);

		contextMenu = new ContextMenu(copyMenuItem);
	}

	public ContentUri(String uri)
	{
		node = new Hyperlink(uri);
		node.setOnAction(event -> UriService.openUri(appendMailToIfNeeded(node.getText())));
		initContextMenu();
	}

	public ContentUri(String uri, String description)
	{
		node = new DisclosedHyperlink(description, uri);
		node.setOnAction(event -> askBeforeOpeningIfNeeded(() -> UriService.openUri(appendMailToIfNeeded(uri))));
		initContextMenu();
	}

	public ContentUri(String uri, String description, Consumer<String> action)
	{
		node = new DisclosedHyperlink(description, uri);
		node.setOnAction(event -> askBeforeOpeningIfNeeded(() -> action.accept(uri)));
		initContextMenu();
	}

	private void initContextMenu()
	{
		node.setOnContextMenuRequested(event -> contextMenu.show(node, event.getScreenX(), event.getScreenY()));
	}

	private void askBeforeOpeningIfNeeded(Runnable action)
	{
		if (((DisclosedHyperlink) node).isMalicious())
		{
			UiUtils.alertConfirm(MessageFormat.format(bundle.getString("uri.malicious-link.confirm"), ((DisclosedHyperlink) node).getUri()), action);
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
		return getUri(node);
	}

	@Override
	public String asText()
	{
		return node.getText();
	}

	private static String getUri(Node node)
	{
		return switch (node)
		{
			case DisclosedHyperlink disclosedHyperlink -> disclosedHyperlink.getUri();
			case Hyperlink hyperlink -> hyperlink.getText();
			default -> "";
		};
	}

	private static void copyToClipboard(ActionEvent event)
	{
		var selectedMenuItem = (MenuItem) event.getTarget();

		var popup = Objects.requireNonNull(selectedMenuItem.getParentPopup());
		ClipboardUtils.copyTextToClipboard(getUri(popup.getOwnerNode()));
	}
}
