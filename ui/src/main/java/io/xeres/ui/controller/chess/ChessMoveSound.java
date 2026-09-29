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

import io.xeres.common.dto.chess.ChessGameDTO;
import io.xeres.ui.support.sound.SoundPlayerService.SoundType;
import org.jspecify.annotations.Nullable;

final class ChessMoveSound
{
	private ChessMoveSound()
	{
	}

	static @Nullable SoundType forUpdate(ChessGameDTO previous, ChessGameDTO current)
	{
		if (previous.peer().equals(current.peer()) && previous.status().equals("ACTIVE"))
		{
			var result = switch (current.status())
			{
				case "DRAW" -> SoundType.CHESS_DRAW;
				case "RESIGNED" -> SoundType.CHESS_DEFEAT;
				case "OPPONENT_RESIGNED" -> SoundType.CHESS_VICTORY;
				case "CHECKMATE" -> current.white() == current.whiteToMove() ? SoundType.CHESS_DEFEAT : SoundType.CHESS_VICTORY;
				default -> null;
			};
			if (result != null) return result;
		}
		var count = previous.moves().size();
		if (!previous.peer().equals(current.peer()) || !previous.status().equals("ACTIVE")
				|| current.moves().size() != count + 1
				|| !current.moves().subList(0, count).equals(previous.moves())
				|| current.fen().equals(previous.fen()))
		{
			return null;
		}
		// Counting pieces also handles en passant and capture promotions.
		var before = previous.squares().chars().filter(piece -> piece != '.').count();
		var after = current.squares().chars().filter(piece -> piece != '.').count();
		return after < before ? SoundType.CHESS_CAPTURE : SoundType.CHESS_MOVE;
	}
}