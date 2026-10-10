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

import io.xeres.common.protocol.xrs.RsServiceDescriptor;

public enum ChessServiceType implements RsServiceDescriptor
{
	CHESS;

	@Override public int getType() { return 0xC4E5; }
	@Override public String getName() { return "RetroChess"; }
	@Override public short getVersionMajor() { return 1; }
	@Override public short getVersionMinor() { return 0; }
	@Override public short getMinVersionMajor() { return 1; }
	@Override public short getMinVersionMinor() { return 0; }
}
