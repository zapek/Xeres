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

package io.xeres.chess.ui.controller.chess;

import io.xeres.chess.FXTest;
import io.xeres.chess.common.dto.chess.ChessBoardDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChessMoveCellTest extends FXTest
{
	@Test
	void usesPieceBeforeMoveAndClearsRecycledGraphics() throws Exception
	{
		org.testfx.util.WaitForAsyncUtils.asyncFx(() -> {
			var initial = new ChessBoardDTO("rnbqkbnrpppppppp................................PPPPPPPPRNBQKBNR", true, false);
			var cell = new ChessMoveCell<Object>(() -> List.of(initial), true);
			cell.updateIndex(0);
			cell.updateItem("g1f3", false);
			assertEquals("g1-f3", cell.getText());
			assertInstanceOf(ChessPieceView.class, cell.getGraphic());
			assertEquals('N', ChessMoveCell.movedPiece(List.of(initial), 0, "g1f3"));
			assertEquals('n', ChessMoveCell.movedPiece(List.of(initial), 0, "b8c6"));
			assertEquals('K', ChessMoveCell.movedPiece(List.of(initial), 0, "e1g1"));
			cell.updateItem("e2e4", false);
			assertEquals("e2-e4", cell.getText());
			assertNull(cell.getGraphic());
			cell.updateItem(null, true);
			assertNull(cell.getText());
			assertNull(cell.getGraphic());
			assertEquals('.', ChessMoveCell.movedPiece(List.of(), 0, "g1f3"));
			assertEquals('.', ChessMoveCell.movedPiece(List.of(initial), 0, "invalid"));
			var promotion = new ChessBoardDTO(".......r......P." + ".".repeat(48), true, false);
			assertEquals('P', ChessMoveCell.movedPiece(List.of(promotion), 0, "g7h8q"));
		}).get(10, TimeUnit.SECONDS);
	}
}