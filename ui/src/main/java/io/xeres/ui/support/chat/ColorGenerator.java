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

import java.util.Arrays;
import java.util.List;
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
		COLOR_00("color-00"),
		COLOR_01("color-01"),
		COLOR_02("color-02"),
		COLOR_03("color-03"),
		COLOR_04("color-04"),
		COLOR_05("color-05"),
		COLOR_06("color-06"),
		COLOR_07("color-07"),
		COLOR_08("color-08"),
		COLOR_09("color-09"),
		COLOR_10("color-10"),
		COLOR_11("color-11"),
		COLOR_12("color-12"),
		COLOR_13("color-13"),
		COLOR_14("color-14"),
		COLOR_15("color-15");

		private final String color;

		ColorSpec(String color)
		{
			this.color = color;
		}

		public String getColor()
		{
			return color;
		}
	}

	public static String generateColor(String s)
	{
		Objects.requireNonNull(s);
		return ColorSpec.values()[Math.floorMod(s.hashCode(), ColorSpec.values().length)].getColor();
	}

	public static List<String> getAllColors()
	{
		return Arrays.stream(ColorSpec.values()).map(ColorSpec::getColor).toList();
	}
}
