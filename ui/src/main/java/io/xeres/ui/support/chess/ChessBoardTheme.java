/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.chess;

public enum ChessBoardTheme
{
	BROWN("#f0d9b5", "#b58863", "#f4b886"),
	GREEN("#eeeed2", "#769656", "#9fcb74"),
	SKY("#eef1f0", "#c4dbe5", "#dbeef7"),
	BLUE("#f1f5f7", "#5593ec", "#9bc4ff"),
	DARK_BLUE("#eeeed2", "#4b7396", "#659bcb"),
	CHECKERS("#c4484c", "#303030", "#414141");

	private final String light;
	private final String dark;
	private final String border;

	ChessBoardTheme(String light, String dark, String border)
	{
		this.light = light;
		this.dark = dark;
		this.border = border;
	}

	public String light() { return light; }
	public String dark() { return dark; }
	public String border() { return border; }

	public static ChessBoardTheme fromPreference(String value)
	{
		try
		{
			return valueOf(value);
		}
		catch (IllegalArgumentException exception)
		{
			return BROWN;
		}
	}
}