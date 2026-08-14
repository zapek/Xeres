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

package io.xeres.ui.support.splash;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import static io.xeres.ui.support.splash.SplashService.*;

/**
 * A window that mimics the native splash screen as closely as possible. It is
 * only used when the native splash screen got disabled by a window being shown
 * (for example the password dialog) so that the user can still see the loading
 * progress.
 */
final class SplashWindow
{
	private final JWindow window;
	private final BufferedImage image;
	private final int width;
	private final int height;

	private String statusText;

	private SplashWindow(URL imageUrl, int width, int height) throws IOException
	{
		this.width = width;
		this.height = height;

		image = ImageIO.read(imageUrl);
		if (image == null)
		{
			throw new IOException("Unsupported splash screen image format: " + imageUrl);
		}

		window = new JWindow();
		window.setAlwaysOnTop(true);
		window.setSize(width, height);
		window.setLocationRelativeTo(null);
		window.setContentPane(new SplashPanel());
		window.setVisible(true);
	}

	static SplashWindow create(URL imageUrl, int width, int height) throws IOException
	{
		return new SplashWindow(imageUrl, width, height);
	}

	void setStatus(String text)
	{
		statusText = text;
		window.repaint();
	}

	void close()
	{
		window.setVisible(false);
		window.dispose();
	}

	private class SplashPanel extends JPanel
	{
		@Override
		protected void paintComponent(Graphics g)
		{
			var g2d = (Graphics2D) g.create();
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.drawImage(image, 0, 0, width, height, null);

			if (statusText != null)
			{
				var y = height - LOADING_TEXT_DISTANCE;
				g2d.setBackground(new Color(BACKGROUND_COLOR));
				g2d.clearRect(MARGINS, y, width - MARGINS * 2, LOADING_TEXT_DISTANCE - MARGINS);
				g2d.setColor(Color.BLACK);
				g2d.setFont(g2d.getFont().deriveFont(Font.BOLD));
				var metrics = g2d.getFontMetrics();
				var x = (width - metrics.stringWidth(statusText)) / 2;
				g2d.drawString(statusText, x, y + metrics.getAscent());
			}
			g2d.dispose();
		}
	}
}
