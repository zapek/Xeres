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

import io.xeres.chess.common.dto.chess.ChessBoardDTO;
import javafx.scene.control.TableCell;

import java.util.List;
import java.util.function.Supplier;

/// Move artwork comes from the position before the move, including captures and promotions.
final class ChessMoveCell<T> extends TableCell<T, String>
{
	private final Supplier<List<ChessBoardDTO>> positions;
	private final int side;

	ChessMoveCell(Supplier<List<ChessBoardDTO>> positions, boolean white)
	{
		this.positions = positions;
		side = white ? 0 : 1;
	}

	@Override
	protected void updateItem(String move, boolean empty)
	{
		super.updateItem(move, empty);
		setGraphic(null);
		setText(null);
		if (empty || move == null || move.isBlank()) return;
		setText(move.matches("[a-h][1-8][a-h][1-8][qrbn]?")
				? move.substring(0, 2) + "-" + move.substring(2) : move);
		var piece = movedPiece(positions.get(), getIndex() * 2 + side, move);
		if (piece != '.' && Character.toUpperCase(piece) != 'P')
		{
			var icon = new ChessPieceView(piece);
			icon.setMinSize(18, 18);
			icon.setPrefSize(18, 18);
			icon.setMaxSize(18, 18);
			setGraphic(icon);
		}
	}

	static char movedPiece(List<ChessBoardDTO> positions, int ply, String move)
	{
		if (ply < 0 || ply >= positions.size() || move == null || !move.matches("[a-h][1-8][a-h][1-8][qrbn]?")) return '.';
		var squares = positions.get(ply).squares();
		if (squares == null || squares.length() != 64) return '.';
		var index = (8 - (move.charAt(1) - '0')) * 8 + move.charAt(0) - 'a';
		var piece = squares.charAt(index);
		return "KQRBNPkqrbnp".indexOf(piece) >= 0 ? piece : '.';
	}
}