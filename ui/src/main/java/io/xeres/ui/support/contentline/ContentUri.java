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

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.custom.DisclosedHyperlink;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.uri.Uri;
import io.xeres.ui.support.util.UiUtils;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ContentUri implements Content
{
	private final DisclosedHyperlink node;
	private final Consumer<Uri> action;
	private static final ContextMenu contextMenu;

	private static final ResourceBundle bundle = I18nUtils.getBundle();

	static
	{
		var copyMenuItem = new MenuItem(bundle.getString("copy"));
		copyMenuItem.setGraphic(new FontIcon(MaterialDesignC.CONTENT_COPY));
		copyMenuItem.setOnAction(ContentUri::copyToClipboard);

		contextMenu = new ContextMenu(copyMenuItem);
	}

	public ContentUri(Uri uri, String description, Consumer<Uri> action)
	{
		this.action = action;
		node = new DisclosedHyperlink(description, uri.toString());
		node.setOnAction(_ -> UiUtils.askBeforeOpeningIfNeeded(node, () -> action.accept(uri)));
		initContextMenu();
	}

	private void initContextMenu()
	{
		node.setOnContextMenuRequested(event -> contextMenu.show(node, event.getScreenX(), event.getScreenY()));
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

	public Consumer<Uri> getAction()
	{
		return action;
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
