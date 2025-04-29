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

package io.xeres.ui.support.splash;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.ResourceBundle;

@Service
public final class SplashService
{
	public enum Status
	{
		DATABASE,
		NETWORK
	}

	private final ResourceBundle bundle;

	private SplashScreen splashScreen;
	private Graphics2D g2d;
	private Dimension dimension;

	private static final int LOADING_TEXT_DISTANCE = 20;
	private static final int MARGINS = 2;
	private static final int BACKGROUND_COLOR = 0x414242;

	public SplashService(ResourceBundle bundle)
	{
		this.bundle = bundle;

		try
		{
			splashScreen = SplashScreen.getSplashScreen();
		}
		catch (UnsupportedOperationException ignored)
		{
			// No splash screen supported
		}

		if (splashScreen != null && splashScreen.isVisible())
		{
			g2d = splashScreen.createGraphics();
			dimension = splashScreen.getSize();

			g2d.setBackground(new Color(BACKGROUND_COLOR));
			g2d.setColor(Color.BLACK);
			g2d.setFont(g2d.getFont().deriveFont(Font.BOLD));
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
	}

	public void status(Status status)
	{
		if (g2d != null)
		{
			var y = dimension.getHeight() - LOADING_TEXT_DISTANCE;

			g2d.clearRect(MARGINS, (int) y, (int) dimension.getWidth() - MARGINS * 2, LOADING_TEXT_DISTANCE - MARGINS);
			drawStringCentered(getDescriptionFromStatus(status) + "\u2026", (int) y);
			splashScreen.update();
		}
	}

	private String getDescriptionFromStatus(Status status)
	{
		return switch (status)
		{
			case DATABASE -> bundle.getString("splash.status.database");
			case NETWORK -> bundle.getString("splash.status.network");
		};
	}

	public void close()
	{
		if (splashScreen != null)
		{
			// We don't need the splash screen anymore, so let the GC collect it
			splashScreen.close();
			g2d = null;
			dimension = null;
			splashScreen = null;
		}
	}

	private void drawStringCentered(String s, int y)
	{
		var metrics = g2d.getFontMetrics();
		var x = ((int) dimension.getWidth() - metrics.stringWidth(s)) / 2;

		g2d.drawString(s, x, y + metrics.getAscent());
	}
}
