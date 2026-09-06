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

package io.xeres.app.xrs.service.chess;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChessPositionTest
{
	@Test
	void initialPositionHasStandardMoveTree()
	{
		var position = new ChessPosition();
		assertEquals(20, perft(position, 1));
		assertEquals(400, perft(position, 2));
		assertEquals(8902, perft(position, 3));
	}

	@Test
	void fenAndHashMatchRetroChessWireFixture()
	{
		var position = play("e2e4");
		assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", position.fen());
		assertEquals("952a5e992e65efab", position.hash());
	}

	@Test
	void castleMovesRookAndRemovesRights()
	{
		var position = play("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6", "e1g1");
		assertEquals('R', position.squares().charAt(61));
		assertEquals('K', position.squares().charAt(62));
		assertEquals('.', position.squares().charAt(63));
		assertEquals("kq", position.fen().split(" ")[2]);
	}

	@Test
	void enPassantRemovesCapturedPawnAndExpires()
	{
		var position = play("e2e4", "a7a6", "e4e5", "d7d5");
		var captured = position.move("e5d6");
		assertEquals('.', captured.squares().charAt(ChessPosition.index("d5")));
		assertEquals('P', captured.squares().charAt(ChessPosition.index("d6")));
		assertEquals(0, captured.halfmoveClock());
		var expired = position.move("g1f3").move("a6a5");
		assertThrows(IllegalArgumentException.class, () -> expired.move("e5d6"));
	}

	@Test
	void promotionSupportsAllFourPiecesAndRequiresChoice()
	{
		var position = play("a2a4", "h7h5", "a4a5", "h5h4", "a5a6", "h4h3", "a6b7", "h3g2");
		for (var piece : "qrbn".toCharArray())
		{
			assertEquals(Character.toUpperCase(piece), position.move("b7a8" + piece).squares().charAt(0));
		}
		assertThrows(IllegalArgumentException.class, () -> position.move("b7a8"));
	}

	@Test
	void pinnedPieceCannotExposeKing()
	{
		var position = play("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "d7d6", "a2a3");
		assertThrows(IllegalArgumentException.class, () -> position.move("c6b4"));
	}

	@Test
	void foolsMateHasNoLegalReply()
	{
		var position = play("f2f3", "e7e5", "g2g4", "d8h4");
		assertTrue(position.inCheck(true));
		assertTrue(position.legalMoves().isEmpty());
	}

	@Test
	void invalidMoveDoesNotMutateBoard()
	{
		var position = new ChessPosition();
		var fen = position.fen();
		assertThrows(IllegalArgumentException.class, () -> position.move("e2e5"));
		assertThrows(IllegalArgumentException.class, () -> position.move("e7e5"));
		assertThrows(IllegalArgumentException.class, () -> position.move("a0a9"));
		assertEquals(fen, position.fen());
	}

	private ChessPosition play(String... moves)
	{
		var position = new ChessPosition();
		for (var move : moves)
		{
			position = position.move(move);
		}
		return position;
	}

	private long perft(ChessPosition position, int depth)
	{
		if (depth == 0)
		{
			return 1;
		}
		return position.legalMoves().stream().mapToLong(move -> perft(position.move(move), depth - 1)).sum();
	}
}
