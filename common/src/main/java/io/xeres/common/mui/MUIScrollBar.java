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

package io.xeres.common.mui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class MUIScrollBar extends BasicScrollBarUI
{
	@Override
	protected void configureScrollBarColors()
	{
		super.configureScrollBarColors();
		thumbColor = new Color(102, 136, 187);
		trackColor = Color.DARK_GRAY;
	}

	@Override
	protected JButton createDecreaseButton(int orientation)
	{
		return createZeroButton();
	}

	@Override
	protected JButton createIncreaseButton(int orientation)
	{
		return createZeroButton();
	}

	private JButton createZeroButton()
	{
		var button = new JButton();
		button.setPreferredSize(new Dimension(0, 0));
		button.setMinimumSize(new Dimension(0, 0));
		button.setMaximumSize(new Dimension(0, 0));
		return button;
	}

	@Override
	protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(thumbColor);
		g2.fill3DRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, true);

		g2.dispose();
	}

	@Override
	protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2.setColor(trackColor);
		g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);

		g2.dispose();
	}
}
