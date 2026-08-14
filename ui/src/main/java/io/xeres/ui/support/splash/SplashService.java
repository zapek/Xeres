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

package io.xeres.ui.support.splash;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.util.ResourceBundle;

/// Handles the splash screen. It comes from the JVM `-splash` argument.
@Service
public final class SplashService
{
	private static final Logger log = LoggerFactory.getLogger(SplashService.class);

	public enum Status
	{
		DATABASE,
		NETWORK
	}

	private final ResourceBundle bundle;

	private SplashScreen splashScreen;
	private Graphics2D g2d;
	private Dimension dimension;
	private static SplashWindow splashWindow;

	static final int LOADING_TEXT_DISTANCE = 20;
	static final int MARGINS = 2;
	static final int BACKGROUND_COLOR = 0x414242;

	private static SplashParameters splashParameters;

	/// Prepares to save the splash screen when a Swing/AWT window is going to come up, as it will automatically close it.
	/// Use the [restore()] method to restore it afterwards.
	public static void save()
	{
		var splashScreen = SplashScreen.getSplashScreen();
		if (splashScreen != null && splashScreen.isVisible())
		{
			splashParameters = new SplashParameters(splashScreen.getImageURL(), splashScreen.getBounds().width, splashScreen.getBounds().height);
		}
	}

	/// Restores the splash screen after it has been automatically closed by a Swing/AWT window. [save()] must have been called before that.
	public static void restore()
	{
		setupWindowSplash();
	}

	public SplashService(ResourceBundle bundle)
	{
		this.bundle = bundle;

		setupNativeSplash();
	}

	/// Changes the status text displayed at the bottom of the splash screen.
	///
	/// @param status the status
	public void status(Status status)
	{
		if (g2d != null)
		{
			var y = dimension.getHeight() - LOADING_TEXT_DISTANCE;

			g2d.clearRect(MARGINS, (int) y, (int) dimension.getWidth() - MARGINS * 2, LOADING_TEXT_DISTANCE - MARGINS);
			drawStringCentered(getDescriptionFromStatus(status) + "…", (int) y);
			splashScreen.update();
		}
		else if (splashWindow != null)
		{
			splashWindow.setStatus(getDescriptionFromStatus(status) + "…");
		}
	}

	/// Closes the splash screen.
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
		else if (splashWindow != null)
		{
			splashWindow.close();
			splashWindow = null;
		}
	}

	private void setupNativeSplash()
	{
		try
		{
			splashScreen = SplashScreen.getSplashScreen();
		}
		catch (UnsupportedOperationException _)
		{
			// No splash screen supported
		}

		if (splashScreen != null && splashScreen.isVisible())
		{
			g2d = splashScreen.createGraphics();
			dimension = splashScreen.getSize();

			if (g2d != null)
			{
				g2d.setBackground(new Color(BACKGROUND_COLOR));
				g2d.setColor(Color.BLACK);
				g2d.setFont(g2d.getFont().deriveFont(Font.BOLD));
				g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			}
		}
	}

	private static void setupWindowSplash()
	{
		if (splashParameters == null || SplashScreen.getSplashScreen() != null)
		{
			return;
		}

		try
		{
			splashWindow = SplashWindow.create(splashParameters.imageUrl(), splashParameters.width(), splashParameters.height());
		}
		catch (IOException e)
		{
			log.error("Failed to create the splash screen: {}", e.getMessage());
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

	private void drawStringCentered(String s, int y)
	{
		var metrics = g2d.getFontMetrics();
		var x = ((int) dimension.getWidth() - metrics.stringWidth(s)) / 2;

		g2d.drawString(s, x, y + metrics.getAscent());
	}
}
