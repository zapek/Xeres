/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

import atlantafx.base.theme.Styles;
import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.custom.DisclosedHyperlink;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.support.clipboard.ClipboardUtils;
import io.xeres.ui.support.uri.Uri;
import io.xeres.ui.support.util.ImageViewUtils;
import io.xeres.ui.support.util.UiUtils;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

public class ContentUriPreview implements Content
{
	private static final int MAXIMUM_THUMBNAIL_WIDTH = 240;
	private static final int MAXIMUM_THUMBNAIL_HEIGHT = 180;

	private final Pane node;
	private final DisclosedHyperlink hyperlink;
	private static final ContextMenu contextMenu;

	private static final ResourceBundle bundle = I18nUtils.getBundle();

	static
	{
		var copyMenuItem = new MenuItem(bundle.getString("copy"));
		copyMenuItem.setGraphic(new FontIcon(MaterialDesignC.CONTENT_COPY));
		copyMenuItem.setOnAction(ContentUriPreview::copyToClipboard);

		contextMenu = new ContextMenu(copyMenuItem);
	}

	public ContentUriPreview(Uri uri, String title, String description, String site, String thumbnailUrl, int thumbnailWidth, int thumbnailHeight, Function<String, byte[]> loader, Consumer<Uri> action, Runnable renderedAction)
	{
		var asyncImageView = new AsyncImageView(loader);
		if (thumbnailWidth > 0 && thumbnailHeight > 0)
		{
			var dimensions = ImageViewUtils.limitMaximumImageSize(thumbnailWidth, thumbnailHeight, MAXIMUM_THUMBNAIL_WIDTH, MAXIMUM_THUMBNAIL_HEIGHT);
			asyncImageView.setFitWidth(dimensions.getWidth());
			asyncImageView.setFitHeight(dimensions.getHeight());
		}
		else
		{
			asyncImageView.setOnSuccess(() -> {
				ImageViewUtils.limitMaximumImageSize(asyncImageView, MAXIMUM_THUMBNAIL_WIDTH, MAXIMUM_THUMBNAIL_HEIGHT);
				renderedAction.run();
			});
		}
		asyncImageView.setUrl(thumbnailUrl);

		node = new VBox(asyncImageView); // XXX: VBox computes its baseline from the first managed item (usually the image), but we would like the bottom of the VBox to be the baseline... how to do it?
		node.getStyleClass().add("uri-preview");

		if (StringUtils.isNotBlank(title))
		{
			var titleLabel = new Label(title);
			titleLabel.setWrapText(true);
			titleLabel.setMaxWidth(MAXIMUM_THUMBNAIL_WIDTH);
			titleLabel.getStyleClass().add(Styles.TEXT_CAPTION);
			node.getChildren().add(titleLabel);
		}

		if (StringUtils.isNotBlank(description))
		{
			var descriptionLabel = new Label(description);
			descriptionLabel.setWrapText(true);
			descriptionLabel.setMaxWidth(MAXIMUM_THUMBNAIL_WIDTH);
			descriptionLabel.getStyleClass().add(Styles.TEXT_SMALL);
			node.getChildren().add(descriptionLabel);
		}

		if (StringUtils.isNotBlank(site))
		{
			var siteLabel = new Label(site);
			siteLabel.setWrapText(true);
			siteLabel.setMaxWidth(MAXIMUM_THUMBNAIL_WIDTH);
			siteLabel.getStyleClass().add(Styles.TEXT_SMALL);
			node.getChildren().add(siteLabel);
		}

		hyperlink = new DisclosedHyperlink(uri.toUriString(), uri.toUriString(), false);
		hyperlink.setWrappingWidth(MAXIMUM_THUMBNAIL_WIDTH);
		UiUtils.setOnPrimaryMouseClicked(node, _ -> UiUtils.askBeforeOpeningIfNeeded(hyperlink, () -> action.accept(uri)));
		node.getChildren().add(hyperlink);
		initContextMenu();
	}

	private void initContextMenu()
	{
		node.setOnContextMenuRequested(event -> {
			contextMenu.show(node, event.getScreenX(), event.getScreenY());
			event.consume();
		});
	}

	@Override
	public Node getNode()
	{
		return node;
	}

	@Override
	public String asText()
	{
		return hyperlink.getText();
	}

	private static void copyToClipboard(ActionEvent event)
	{
		var selectedMenuItem = (MenuItem) event.getTarget();

		var popup = Objects.requireNonNull(selectedMenuItem.getParentPopup());
		ClipboardUtils.copyTextToClipboard(((DisclosedHyperlink) ((Pane) popup.getOwnerNode()).getChildren().stream()
				.filter(DisclosedHyperlink.class::isInstance)
				.findFirst().orElseThrow()).getUri());
	}
}
