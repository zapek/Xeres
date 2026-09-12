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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChessMoveSoundTest
{
	private ChessGameDTO position(String squares, List<String> moves)
	{
		var game = mock(ChessGameDTO.class);
		when(game.peer()).thenReturn("peer");
		when(game.status()).thenReturn("ACTIVE");
		when(game.fen()).thenReturn(squares + moves);
		when(game.squares()).thenReturn(squares);
		when(game.moves()).thenReturn(moves);
		return game;
	}

	@Test
	void moveCaptureAndEnPassantUseTheAppropriateSound()
	{
		var before = position("KPpk", List.of());
		assertEquals(SoundType.CHESS_MOVE, ChessMoveSound.forUpdate(before, position("K.Ppk", List.of("e2e4"))));
		assertEquals(SoundType.CHESS_CAPTURE, ChessMoveSound.forUpdate(before, position("KP.k", List.of("e5d6"))));
		assertEquals(SoundType.CHESS_CAPTURE, ChessMoveSound.forUpdate(before, position("KQ.k", List.of("g7h8q"))));
	}

	@Test
	void duplicatesAndHistoryRestoresAreSilent()
	{
		var before = position("KPpk", List.of());
		assertNull(ChessMoveSound.forUpdate(before, before));
		assertNull(ChessMoveSound.forUpdate(before, position("KPk", List.of("e2e4", "d7d5"))));
		when(before.status()).thenReturn("OUTGOING");
		assertNull(ChessMoveSound.forUpdate(before, position("K.Ppk", List.of("e2e4"))));
	}

	@Test
	void resultsPlayOnceAndCheckmateUsesLocalColor()
	{
		var active = position("Kpk", List.of());
		var result = position("Kpk", List.of());
		when(result.status()).thenReturn("DRAW");
		assertEquals(SoundType.CHESS_DRAW, ChessMoveSound.forUpdate(active, result));
		assertNull(ChessMoveSound.forUpdate(result, result));
		when(result.status()).thenReturn("RESIGNED");
		assertEquals(SoundType.CHESS_DEFEAT, ChessMoveSound.forUpdate(active, result));
		when(result.status()).thenReturn("OPPONENT_RESIGNED");
		assertEquals(SoundType.CHESS_VICTORY, ChessMoveSound.forUpdate(active, result));
		when(result.status()).thenReturn("CHECKMATE");
		for (var white : new boolean[]{false, true})
		{
			when(result.white()).thenReturn(white);
			when(result.whiteToMove()).thenReturn(white);
			assertEquals(SoundType.CHESS_DEFEAT, ChessMoveSound.forUpdate(active, result));
			when(result.whiteToMove()).thenReturn(!white);
			assertEquals(SoundType.CHESS_VICTORY, ChessMoveSound.forUpdate(active, result));
		}
		when(result.status()).thenReturn("CLOSED");
		assertNull(ChessMoveSound.forUpdate(active, result));
	}
	@Test
	void soundResourcesAreBundled()
	{
		assertNotNull(getClass().getResource("/sounds/chess-move.mp3"));
		assertNotNull(getClass().getResource("/sounds/chess-capture.mp3"));
		assertNotNull(getClass().getResource("/sounds/chess-draw.mp3"));
		assertNotNull(getClass().getResource("/sounds/chess-defeat.mp3"));
		assertNotNull(getClass().getResource("/sounds/chess-victory.mp3"));
	}
}