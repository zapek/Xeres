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

package io.xeres.ui.controller.chess;

import io.xeres.common.dto.chess.ChessBoardDTO;

import java.util.List;

final class ChessMaterial
{
	private ChessMaterial()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static String captured(List<ChessBoardDTO> positions, int ply, boolean byWhite)
	{
		var pieces = byWhite ? "qrbnp" : "QRBNP";
		var captured = new int[pieces.length()];
		for (var i = 1; i <= ply && i < positions.size(); i++)
		{
			var before = positions.get(i - 1);
			// Only count the opponent's losses on this turn, excluding promotion.
			if (before.whiteToMove() != byWhite)
			{
				continue;
			}
			var after = positions.get(i).squares();
			for (var p = 0; p < pieces.length(); p++)
			{
				var piece = pieces.charAt(p);
				captured[p] += Math.max(0, count(before.squares(), piece) - count(after, piece));
			}
		}
		var result = new StringBuilder();
		for (var p = 0; p < pieces.length(); p++)
		{
			result.append(String.valueOf(pieces.charAt(p)).repeat(captured[p]));
		}
		return result.toString();
	}

	static int advantage(String squares, boolean white)
	{
		var score = 0;
		for (var piece : squares.toCharArray())
		{
			var value = switch (Character.toUpperCase(piece))
			{
				case 'P' -> 1;
				case 'N', 'B' -> 3;
				case 'R' -> 5;
				case 'Q' -> 9;
				default -> 0;
			};
			score += Character.isUpperCase(piece) == white ? value : -value;
		}
		return score;
	}

	private static int count(String squares, char piece)
	{
		return (int) squares.chars().filter(value -> value == piece).count();
	}
}

