/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Xeres. If not, see <http://www.gnu.org/licenses/>.
 */

package io.xeres.chess.app.xrs.service.chess;

import io.xeres.chess.common.dto.chess.ChessGameDTO;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/// Exports games played from the standard starting position using PGN and SAN.
public final class ChessPgn
{
	private ChessPgn()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static String export(String startedAt, String localName, ChessGameDTO game)
	{
		var date = "????.??.??";
		try
		{
			date = DateTimeFormatter.ofPattern("yyyy.MM.dd").withZone(ZoneOffset.UTC).format(Instant.parse(startedAt));
		}
		catch (DateTimeParseException ignored)
		{
			// Older history may not contain an ISO timestamp.
		}
		var result = switch (game.status())
		{
			case "CHECKMATE" -> game.whiteToMove() ? "0-1" : "1-0";
			case "RESIGNED" -> game.white() ? "0-1" : "1-0";
			case "OPPONENT_RESIGNED" -> game.white() ? "1-0" : "0-1";
			case "DRAW" -> "1/2-1/2";
			default -> "*";
		};
		var output = new StringBuilder();
		tag(output, "Event", "Xeres Chess");
		tag(output, "Site", "?");
		tag(output, "Date", date);
		tag(output, "Round", "?");
		tag(output, "White", game.white() ? localName : game.name());
		tag(output, "Black", game.white() ? game.name() : localName);
		tag(output, "Result", result);
		output.append('\n');
		var position = new ChessPosition();
		var lineLength = 0;
		for (var index = 0; index < game.moves().size(); index++)
		{
			var move = game.moves().get(index);
			var token = (index % 2 == 0 ? (index / 2 + 1) + ". " : "") + san(position, move);
			if (lineLength + token.length() > 79)
			{
				output.append('\n');
				lineLength = 0;
			}
			output.append(token).append(' ');
			lineLength += token.length() + 1;
			position = position.move(move);
		}
		if (lineLength + result.length() > 79) output.append('\n');
		return output.append(result).append("\n\n").toString();
	}

	static String san(ChessPosition position, String move)
	{
		var next = position.move(move); // Validate before using the coordinate notation.
		var from = ChessPosition.index(move.substring(0, 2));
		var to = ChessPosition.index(move.substring(2, 4));
		var board = position.squares();
		var piece = Character.toUpperCase(board.charAt(from));
		var notation = new StringBuilder();
		if (piece == 'K' && Math.abs(to - from) == 2)
		{
			notation.append(to > from ? "O-O" : "O-O-O");
		}
		else
		{
			var capture = board.charAt(to) != '.' || piece == 'P' && from % 8 != to % 8;
			if (piece != 'P')
			{
				notation.append(piece);
				var alternatives = position.legalMoves().stream()
						.filter(candidate -> !candidate.equals(move) && candidate.substring(2, 4).equals(move.substring(2, 4)))
						.map(candidate -> ChessPosition.index(candidate.substring(0, 2)))
						.filter(square -> board.charAt(square) == board.charAt(from)).toList();
				if (!alternatives.isEmpty())
				{
					if (alternatives.stream().noneMatch(square -> square % 8 == from % 8)) notation.append(move.charAt(0));
					else if (alternatives.stream().noneMatch(square -> square / 8 == from / 8)) notation.append(move.charAt(1));
					else notation.append(move, 0, 2);
				}
			}
			else if (capture) notation.append(move.charAt(0));
			if (capture) notation.append('x');
			notation.append(move, 2, 4);
			if (move.length() == 5) notation.append('=').append(Character.toUpperCase(move.charAt(4)));
		}
		if (next.inCheck(next.isWhiteToMove())) notation.append(next.legalMoves().isEmpty() ? '#' : '+');
		return notation.toString();
	}

	private static void tag(StringBuilder output, String name, String value)
	{
		var escaped = value == null || value.isBlank() ? "?" : value.replace("\\", "\\\\").replace("\"", "\\\"").replaceAll("[\\p{Cntrl}]", " ");
		output.append('[').append(name).append(" \"").append(escaped).append("\"]\n");
	}
}
