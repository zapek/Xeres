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

package io.xeres.ui.support.window;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inspired from <a href="https://pablofernandez.tech/2017/12/20/restoring-window-sizes-in-javafx/">the blog post of Pablo Fernandez</a>
 */
final class WindowResizer
{
	private static final Logger log = LoggerFactory.getLogger(WindowResizer.class);

	private static final double MINIMUM_VISIBLE_WIDTH = 100.0;
	private static final double MINIMUM_VISIBLE_HEIGHT = 50.0;

	private WindowResizer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void ensureWindowIsVisible(Stage stage)
	{
		if (isWindowOutOfBounds(stage))
		{
			log.debug("Window out of bounds, repositioning...");
			moveToPrimaryScreen(stage);
		}
	}

	private static boolean isWindowOutOfBounds(Stage stage)
	{
		for (Screen screen : Screen.getScreens())
		{
			Rectangle2D bounds = screen.getVisualBounds();
			if (stage.getX() + stage.getWidth() - MINIMUM_VISIBLE_WIDTH >= bounds.getMinX() &&
					stage.getX() + MINIMUM_VISIBLE_WIDTH <= bounds.getMaxX() &&
					bounds.getMinY() <= stage.getY() && // We want the title bar to always be visible.
					stage.getY() + MINIMUM_VISIBLE_HEIGHT <= bounds.getMaxY())
			{
				return false;
			}
		}
		return true;
	}

	private static void moveToPrimaryScreen(Stage stage)
	{
		Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
		stage.setX((bounds.getWidth() / 2) - (stage.getWidth() / 2));
		stage.setY((bounds.getHeight() / 2) - (stage.getHeight() / 2));
	}
}
