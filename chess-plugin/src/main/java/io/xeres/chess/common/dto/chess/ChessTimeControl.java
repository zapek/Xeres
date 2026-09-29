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

package io.xeres.chess.common.dto.chess;

/// Chess time control, same values and wire format as RetroChess' `ChessTimeControl`.
///
/// The network string is `unlimited` or `minutes+increment` (for example `5+3`).
public record ChessTimeControl(boolean unlimited, int minutes, int increment)
{
	public static final ChessTimeControl UNLIMITED = new ChessTimeControl(true, 10, 0);
	public static final int MAX_MINUTES = 180;
	public static final int MAX_INCREMENT = 180;

	public static ChessTimeControl of(int minutes, int increment)
	{
		if (minutes < 1 || minutes > MAX_MINUTES || increment < 0 || increment > MAX_INCREMENT)
		{
			throw new IllegalArgumentException("Invalid time control " + minutes + "+" + increment);
		}
		return new ChessTimeControl(false, minutes, increment);
	}

	/// Parses a network string; anything invalid is an unlimited game (like RetroChess).
	public static ChessTimeControl fromNetString(String value)
	{
		if (value == null || value.isEmpty() || value.equals("unlimited"))
		{
			return UNLIMITED;
		}
		var plus = value.indexOf('+');
		if (plus < 1)
		{
			return UNLIMITED;
		}
		try
		{
			var minutes = Integer.parseInt(value.substring(0, plus));
			var increment = Integer.parseInt(value.substring(plus + 1));
			if (minutes < 1 || minutes > MAX_MINUTES || increment < 0 || increment > MAX_INCREMENT)
			{
				return UNLIMITED;
			}
			return new ChessTimeControl(false, minutes, increment);
		}
		catch (NumberFormatException e)
		{
			return UNLIMITED;
		}
	}

	public String toNetString()
	{
		return unlimited ? "unlimited" : minutes + "+" + increment;
	}

	/// Estimated game length in seconds, used for the category.
	public int estimatedSeconds()
	{
		return minutes * 60 + increment * 40;
	}

	/// "Unlimited", "Bullet", "Blitz", "Rapid" or "Classical" (Lichess thresholds, like RetroChess).
	public String category()
	{
		if (unlimited)
		{
			return "Unlimited";
		}
		var seconds = estimatedSeconds();
		if (seconds < 179) return "Bullet";
		if (seconds < 479) return "Blitz";
		if (seconds < 1499) return "Rapid";
		return "Classical";
	}

	public long initialMs()
	{
		return minutes * 60_000L;
	}

	public long incrementMs()
	{
		return increment * 1000L;
	}
}
