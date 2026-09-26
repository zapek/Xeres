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

package io.xeres.chess;

import static io.xeres.common.message.MessagePath.BROKER_PREFIX;

public final class ChessPaths
{
	public static final String CHESS_PATH = "/api/v1/chess";
	public static final String CHESS_ROOT = "/chess";

	private ChessPaths()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static String chessDestination()
	{
		return BROKER_PREFIX + CHESS_ROOT;
	}
}
