/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.chat;

import javafx.scene.paint.Color;

public final class ColorGenerator
{
	private ColorGenerator()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Those colors are color-blind proof (protanopia, deuteranopia and tritanopia).
	 *
	 * @see <a href="https://www.nature.com/articles/nmeth.1618">Bang Wong's paper</a>
	 */
	private enum ColorSpec
	{
		ORANGE(Color.rgb(230, 159, 0)),
		SKY_BLUE(Color.rgb(86, 180, 233)),
		BLUISH_GREEN(Color.rgb(0, 158, 115)),
		YELLOW(Color.rgb(240, 228, 66)),
		BLUE(Color.rgb(0, 114, 178)),
		VERMILLION(Color.rgb(213, 94, 0)),
		REDDISH_PURPLE(Color.rgb(204, 121, 167));

		private final Color color;

		ColorSpec(Color color)
		{
			this.color = color;
		}

		public Color getColor()
		{
			return color;
		}
	}

	public static Color generateColor(String s)
	{
		return ColorSpec.values()[Math.floorMod(s.hashCode(), ColorSpec.values().length)].getColor();
	}
}
