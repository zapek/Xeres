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

import io.xeres.common.i18n.I18nUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;

/// Handles the splash screen. It comes from the JVM `-splash` argument. It is not a spring bean
/// because it has to be available after the spring context is closed to be able to show the
/// shutdown process.
public final class SplashService
{
	private static final Logger log = LoggerFactory.getLogger(SplashService.class);

	public enum Status
	{
		INITIALIZING,
		DATABASE,
		NETWORK,
		SHUTDOWN
	}

	public enum State
	{
		NONE,
		NATIVE,
		WINDOW
	}

	private Graphics2D g2d;
	private Dimension dimension;
	private SplashWindow splashWindow;

	static final int LOADING_TEXT_DISTANCE = 20;
	static final int MARGINS = 2;
	static final int BACKGROUND_COLOR = 0x414242;

	private SplashParameters splashParameters;
	private Status status = Status.INITIALIZING;
	private State state = State.NONE;

	public SplashService()
	{
	}

	private static class SingletonHelper
	{
		private static final SplashService INSTANCE = new SplashService();
	}

	/// Gets the instance of SplashService.
	///
	/// @return the instance
	public static SplashService getInstance()
	{
		return SingletonHelper.INSTANCE;
	}

	/// Shows the splash window. Will automatically reuse the splash window (launched by the JVM's -splash argument) or create
	/// a new one if the original has been closed already. It will reuse the last set status.
	public void show()
	{
		switch (state)
		{
			case NONE -> setupNativeSplash();
			case NATIVE, WINDOW -> setupWindowSplash();
		}
	}

	/// Changes the status text displayed at the bottom of the splash screen.
	///
	/// @param status the status
	public void status(Status status)
	{
		this.status = status;
		var splashScreen = getSplashScreen();
		if (splashScreen != null && g2d != null)
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
		var splashScreen = getSplashScreen();
		if (splashScreen != null)
		{
			// We don't need the splash screen anymore, so let the GC collect it
			splashScreen.close();
			g2d = null;
			dimension = null;
		}
		else if (splashWindow != null)
		{
			splashWindow.close();
			splashWindow = null;
		}
	}

	private SplashScreen getSplashScreen()
	{
		try
		{
			return SplashScreen.getSplashScreen();
		}
		catch (UnsupportedOperationException _)
		{
			return null;
		}
	}

	private void setupNativeSplash()
	{
		var splashScreen = getSplashScreen();

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

				splashParameters = new SplashParameters(splashScreen.getImageURL(), splashScreen.getBounds().width, splashScreen.getBounds().height);
				status(status);
				state = State.NATIVE;
			}
		}
	}

	private void setupWindowSplash()
	{
		if (splashParameters == null || getSplashScreen() != null || splashWindow != null)
		{
			return;
		}

		try
		{
			splashWindow = SplashWindow.create(splashParameters.imageUrl(), splashParameters.width(), splashParameters.height());
			status(status);
			state = State.WINDOW;
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
			case INITIALIZING -> I18nUtils.getBundle().getString("splash.status.initializing");
			case DATABASE -> I18nUtils.getBundle().getString("splash.status.database");
			case NETWORK -> I18nUtils.getBundle().getString("splash.status.network");
			case SHUTDOWN -> I18nUtils.getBundle().getString("splash.status.shutdown");
		};
	}

	private void drawStringCentered(String s, int y)
	{
		var metrics = g2d.getFontMetrics();
		var x = ((int) dimension.getWidth() - metrics.stringWidth(s)) / 2;

		g2d.drawString(s, x, y + metrics.getAscent());
	}
}
