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

package io.xeres.ui.support.util;

import io.xeres.ui.custom.DelayedTooltip;
import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TooltipUtils
{
	private TooltipUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	private static final Duration DURATION = Duration.minutes(1.0);

	public static void install(@SuppressWarnings("rawtypes") Cell cell, Supplier<String> textSupplier, Supplier<ImageView> graphicSupplier)
	{
		cell.addEventFilter(MouseEvent.MOUSE_ENTERED, _ -> {
			if (cell.getItem() == null)
			{
				return;
			}
			if (textSupplier == null && graphicSupplier == null)
			{
				return;
			}
			var text = textSupplier != null ? textSupplier.get() : null;
			if (StringUtils.isBlank(text))
			{
				return;
			}
			var tooltip = new Tooltip(text);
			if (graphicSupplier != null)
			{
				tooltip.setGraphic(graphicSupplier.get());
			}
			formatTextIfNeeded(tooltip, text);
			tooltip.setShowDuration(DURATION);
			Tooltip.install(cell, tooltip);
		});
		cell.addEventFilter(MouseEvent.MOUSE_EXITED, _ -> {
			if (cell.getItem() != null && cell.getTooltip() != null)
			{
				cell.getTooltip().hide();
				Tooltip.uninstall(cell, cell.getTooltip());
			}
		});
	}

	public static void install(Node node, String text)
	{
		install(node, text, false);
	}

	public static void install(Node node, String text, boolean immediate)
	{
		var tooltip = new Tooltip(text);
		tooltip.setShowDuration(DURATION);
		if (immediate)
		{
			tooltip.setShowDelay(Duration.ZERO);
		}
		formatTextIfNeeded(tooltip, text);
		Tooltip.install(node, tooltip);
	}

	/**
	 * Installs a Tooltip that needs to compute what it is going to show only when it's about to
	 * be shown (for example network call, or heavy computation).
	 *
	 * @param node     the node
	 * @param consumer the consumer that will perform the computation/network call. It has to call {@link DelayedTooltip#show(String)} once it's done to make the tooltip visible
	 */
	public static void install(Node node, Consumer<DelayedTooltip> consumer)
	{
		var tooltip = new DelayedTooltip(consumer);
		tooltip.setShowDuration(DURATION);
		Tooltip.install(node, tooltip);
	}

	public static void uninstall(Node node)
	{
		Tooltip.uninstall(node, null);
	}

	public static void toast(Region node, String text)
	{
		var tooltip = new Tooltip(text);
		Tooltip.install(node, tooltip);
		var p = node.localToScreen(node.getWidth() / 2, node.getHeight());
		tooltip.show(node.getScene().getWindow(), p.getX(), p.getY());
		PauseTransition delay = new PauseTransition(javafx.util.Duration.seconds(2));
		delay.setOnFinished(_ -> {
			tooltip.hide();
			Tooltip.uninstall(node, tooltip);
		});
		delay.playFromStart();
	}

	private static void formatTextIfNeeded(Tooltip tooltip, String text)
	{
		if (text != null && text.length() > 100 && !text.contains("\n"))
		{
			tooltip.setMaxWidth(300.0);
			tooltip.setWrapText(true);
		}
	}
}
