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

package io.xeres.ui.support.tooltip;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.stage.Window;
import javafx.util.Duration;
import org.apache.commons.lang3.StringUtils;

/// A tooltip that shows below a node, instead of where the mouse pointer is.
public class BelowTooltip extends Tooltip
{
	private final Node targetNode;

	public BelowTooltip(Node node, String text)
	{
		super(text);
		targetNode = node;
		setShowDelay(Duration.millis(500));
		setShowDuration(Duration.seconds(2));
	}

	@Override
	public void show(Window ownerWindow, double screenX, double screenY)
	{
		var bounds = targetNode.localToScreen(targetNode.getBoundsInLocal());

		double anchorX = bounds.getMinX();
		double anchorY = bounds.getMaxY() + 8;

		setAnchorX(anchorX);
		setAnchorY(anchorY);

		super.show(targetNode, anchorX, anchorY);
	}

	public static void install(Node node, String text)
	{
		var tooltip = new BelowTooltip(node, text);
		if (StringUtils.isNotBlank(text))
		{
			Tooltip.install(node, tooltip);
		}
	}
}
