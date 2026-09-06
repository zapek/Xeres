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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/// Chess rules and RetroChess's canonical FEN representation. Squares run from a8 (0) to h1 (63).
public final class ChessPosition
{
	private char[] board;
	private boolean white = true;
	private String castling = "KQkq";
	private int enPassant = -1;
	private int halfmove;
	private int fullmove = 1;

	public ChessPosition()
	{
		board = ("rnbqkbnrpppppppp" + ".".repeat(32) + "PPPPPPPPRNBQKBNR").toCharArray();
	}

	private ChessPosition(ChessPosition source)
	{
		board = source.board.clone();
		white = source.white;
		castling = source.castling;
		enPassant = source.enPassant;
		halfmove = source.halfmove;
		fullmove = source.fullmove;
	}

	public boolean isWhiteToMove()
	{
		return white;
	}

	public String squares()
	{
		return new String(board);
	}

	public int halfmoveClock()
	{
		return halfmove;
	}

	public List<String> legalMoves()
	{
		var moves = new ArrayList<String>();
		for (var from = 0; from < 64; from++)
		{
			if (board[from] == '.' || Character.isUpperCase(board[from]) != white)
			{
				continue;
			}
			for (var to = 0; to < 64; to++)
			{
				var promotions = Character.toUpperCase(board[from]) == 'P' && (to / 8 == 0 || to / 8 == 7) ? "qrbn" : "-";
				for (var promotion : promotions.toCharArray())
				{
					if (legal(from, to, promotion))
					{
						moves.add(square(from) + square(to) + (promotion == '-' ? "" : promotion));
					}
				}
			}
		}
		return moves;
	}

	public ChessPosition move(String uci)
	{
		if (uci == null || !uci.matches("[a-h][1-8][a-h][1-8][qrbn]?"))
		{
			throw new IllegalArgumentException("Invalid move");
		}
		var from = index(uci.substring(0, 2));
		var to = index(uci.substring(2, 4));
		var promotion = uci.length() == 5 ? uci.charAt(4) : '-';
		if (!legal(from, to, promotion))
		{
			throw new IllegalArgumentException("Illegal move");
		}
		var next = new ChessPosition(this);
		next.apply(from, to, promotion);
		return next;
	}

	private boolean legal(int from, int to, char promotion)
	{
		if (from == to || board[from] == '.' || Character.isUpperCase(board[from]) != white ||
				(board[to] != '.' && Character.isUpperCase(board[to]) == white) || Character.toUpperCase(board[to]) == 'K')
		{
			return false;
		}
		var piece = Character.toUpperCase(board[from]);
		var dr = to / 8 - from / 8;
		var dc = to % 8 - from % 8;
		var direction = white ? -1 : 1;
		if ((promotion != '-') != (piece == 'P' && (to / 8 == 0 || to / 8 == 7)))
		{
			return false;
		}
		var possible = switch (piece)
		{
			case 'P' -> dc == 0 && board[to] == '.' && (dr == direction ||
					dr == 2 * direction && from / 8 == (white ? 6 : 1) && board[from + 8 * direction] == '.') ||
					Math.abs(dc) == 1 && dr == direction && (board[to] != '.' ||
					to == enPassant && board[to - 8 * direction] == (white ? 'p' : 'P'));
			case 'N' -> Math.abs(dr) * Math.abs(dc) == 2;
			case 'B' -> Math.abs(dr) == Math.abs(dc) && clear(from, to);
			case 'R' -> (dr == 0 || dc == 0) && clear(from, to);
			case 'Q' -> (dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc)) && clear(from, to);
			case 'K' -> Math.max(Math.abs(dr), Math.abs(dc)) == 1 || canCastle(from, to);
			default -> false;
		};
		if (!possible)
		{
			return false;
		}
		var next = new ChessPosition(this);
		next.apply(from, to, promotion);
		return !next.inCheck(white);
	}

	private boolean canCastle(int from, int to)
	{
		var king = white ? 60 : 4;
		if (from != king || to / 8 != from / 8 || Math.abs(to - from) != 2 || inCheck(white))
		{
			return false;
		}
		var right = to > from ? (white ? 'K' : 'k') : (white ? 'Q' : 'q');
		var rook = to > from ? from + 3 : from - 4;
		var step = Integer.signum(to - from);
		if (castling.indexOf(right) < 0 || board[rook] != (white ? 'R' : 'r') || !clear(from, rook))
		{
			return false;
		}
		var transit = new ChessPosition(this);
		transit.board[from] = '.';
		transit.board[from + step] = white ? 'K' : 'k';
		return !transit.inCheck(white);
	}

	private boolean clear(int from, int to)
	{
		var step = Integer.signum(to / 8 - from / 8) * 8 + Integer.signum(to % 8 - from % 8);
		for (var at = from + step; at != to; at += step)
		{
			if (board[at] != '.')
			{
				return false;
			}
		}
		return true;
	}

	public boolean inCheck(boolean side)
	{
		var king = squares().indexOf(side ? 'K' : 'k');
		for (var at = 0; at < 64; at++)
		{
			if (board[at] == '.' || Character.isUpperCase(board[at]) == side)
			{
				continue;
			}
			var dr = king / 8 - at / 8;
			var dc = king % 8 - at % 8;
			var attacked = switch (Character.toUpperCase(board[at]))
			{
				case 'P' -> dr == (side ? 1 : -1) && Math.abs(dc) == 1;
				case 'N' -> Math.abs(dr) * Math.abs(dc) == 2;
				case 'K' -> Math.max(Math.abs(dr), Math.abs(dc)) == 1;
				case 'B' -> Math.abs(dr) == Math.abs(dc) && clear(at, king);
				case 'R' -> (dr == 0 || dc == 0) && clear(at, king);
				case 'Q' -> (dr == 0 || dc == 0 || Math.abs(dr) == Math.abs(dc)) && clear(at, king);
				default -> false;
			};
			if (attacked)
			{
				return true;
			}
		}
		return false;
	}

	private void apply(int from, int to, char promotion)
	{
		var piece = board[from];
		var pawn = Character.toUpperCase(piece) == 'P';
		var capture = board[to] != '.' || pawn && to == enPassant;
		if (pawn && to == enPassant)
		{
			board[to + (white ? 8 : -8)] = '.';
		}
		board[from] = '.';
		board[to] = promotion == '-' ? piece : white ? Character.toUpperCase(promotion) : promotion;
		if (Character.toUpperCase(piece) == 'K')
		{
			castling = castling.replace(white ? "K" : "k", "").replace(white ? "Q" : "q", "");
			if (Math.abs(to - from) == 2)
			{
				var rook = to > from ? from + 3 : from - 4;
				board[(from + to) / 2] = board[rook];
				board[rook] = '.';
			}
		}
		var corners = new int[]{63, 56, 7, 0};
		for (var i = 0; i < corners.length; i++)
		{
			if (from == corners[i] || to == corners[i])
			{
				castling = castling.replace(String.valueOf("KQkq".charAt(i)), "");
			}
		}
		enPassant = pawn && Math.abs(to - from) == 16 ? (from + to) / 2 : -1;
		halfmove = pawn || capture ? 0 : halfmove + 1;
		if (!white)
		{
			fullmove++;
		}
		white = !white;
	}

	public boolean insufficientMaterial()
	{
		var minor = 0;
		var bishops = 0;
		var bishopColor = -1;
		for (var at = 0; at < 64; at++)
		{
			var piece = Character.toUpperCase(board[at]);
			if (piece == '.' || piece == 'K')
			{
				continue;
			}
			if (piece != 'B' && piece != 'N')
			{
				return false;
			}
			minor++;
			if (piece == 'B')
			{
				var color = (at / 8 + at % 8) % 2;
				if (bishopColor != -1 && bishopColor != color)
				{
					return false;
				}
				bishopColor = color;
				bishops++;
			}
		}
		return minor <= 1 || minor == bishops;
	}

	public String fen()
	{
		var result = new StringBuilder();
		for (var row = 0; row < 8; row++)
		{
			var empty = 0;
			for (var col = 0; col < 8; col++)
			{
				var piece = board[row * 8 + col];
				if (piece == '.')
				{
					empty++;
				}
				else
				{
					if (empty > 0)
					{
						result.append(empty);
						empty = 0;
					}
					result.append(piece);
				}
			}
			if (empty > 0)
			{
				result.append(empty);
			}
			if (row < 7)
			{
				result.append('/');
			}
		}
		return result + (white ? " w " : " b ") + (castling.isEmpty() ? "-" : castling) + " " +
				(enPassant < 0 ? "-" : square(enPassant)) + " " + halfmove + " " + fullmove;
	}

	public String repetitionKey()
	{
		var parts = fen().split(" ");
		// An en-passant target changes repetition only when a legal capture exists.
		if (enPassant >= 0 && legalMoves().stream().noneMatch(move -> index(move.substring(2, 4)) == enPassant && Character.toUpperCase(board[index(move.substring(0, 2))]) == 'P'))
		{
			parts[3] = "-";
		}
		return String.join(" ", parts[0], parts[1], parts[2], parts[3]);
	}

	public String hash()
	{
		try
		{
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(fen().getBytes(StandardCharsets.UTF_8))).substring(0, 16);
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException(e);
		}
	}

	public static String square(int index)
	{
		if (index < 0 || index >= 64)
		{
			throw new IllegalArgumentException("Invalid square");
		}
		return "" + (char) ('a' + index % 8) + (8 - index / 8);
	}

	public static int index(String square)
	{
		return (8 - (square.charAt(1) - '0')) * 8 + square.charAt(0) - 'a';
	}
}
