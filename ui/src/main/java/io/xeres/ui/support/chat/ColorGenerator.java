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

package io.xeres.ui.support.chat;

import javafx.scene.paint.Color;

import java.util.Objects;

public final class ColorGenerator
{
	private ColorGenerator()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Colors nicked from <a href="https://github.com/quassel">Quassel</a>
	 * because they are great against a white background.
	 */
	private enum ColorSpec
	{
		COLOR_00(Color.rgb(204, 0, 0)),
		COLOR_01(Color.rgb(0, 108, 173)),
		COLOR_02(Color.rgb(77, 153, 0)),
		COLOR_03(Color.rgb(102, 0, 204)),
		COLOR_04(Color.rgb(166, 125, 0)),
		COLOR_05(Color.rgb(0, 153, 39)),
		COLOR_06(Color.rgb(0, 48, 192)),
		COLOR_07(Color.rgb(204, 0, 154)),
		COLOR_08(Color.rgb(185, 70, 0)),
		COLOR_09(Color.rgb(134, 153, 0)),
		COLOR_10(Color.rgb(20, 153, 0)),
		COLOR_11(Color.rgb(0, 153, 96)),
		COLOR_12(Color.rgb(0, 108, 173)),
		COLOR_13(Color.rgb(0, 153, 204)),
		COLOR_14(Color.rgb(179, 0, 204)),
		COLOR_15(Color.rgb(204, 0, 77));

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
		Objects.requireNonNull(s);
		return ColorSpec.values()[Math.floorMod(s.hashCode(), ColorSpec.values().length)].getColor();
	}
}
