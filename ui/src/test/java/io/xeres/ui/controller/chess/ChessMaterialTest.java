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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChessMaterialTest
{
	@Test
	void capturesFollowHistoryAndPlayerColor()
	{
		var positions = List.of(board("KQkqpP", true), board("KQkqP", false), board("KkqP", true));
		assertEquals("", ChessMaterial.captured(positions, 0, true));
		assertEquals("p", ChessMaterial.captured(positions, 1, true));
		assertEquals("", ChessMaterial.captured(positions, 1, false));
		assertEquals("Q", ChessMaterial.captured(positions, 2, false));
		assertEquals(-8, ChessMaterial.advantage(positions.getLast().squares(), true));
		assertEquals(8, ChessMaterial.advantage(positions.getLast().squares(), false));
	}

	@Test
	void promotionIsNotACaptureButPromotedPiecesCanBeCaptured()
	{
		var positions = List.of(board("KPk r", true), board("KQk", false), board("Kk", true));
		assertEquals("r", ChessMaterial.captured(positions, 1, true));
		assertEquals("", ChessMaterial.captured(positions, 1, false));
		assertEquals("Q", ChessMaterial.captured(positions, 2, false));
	}

	@Test
	void enPassantCountsThePawnRemovedFromAnotherSquare()
	{
		var positions = List.of(board("....pP..", true), board("P.......", false));
		assertEquals("p", ChessMaterial.captured(positions, 1, true));
	}

	@Test
	void emptyHistoryHasNoCaptures()
	{
		assertEquals("", ChessMaterial.captured(List.of(), 0, true));
	}

	private ChessBoardDTO board(String pieces, boolean whiteToMove)
	{
		return new ChessBoardDTO(pieces, whiteToMove, false);
	}
}

