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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Xeres.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.xeres.app.xrs.service;

public final class RsServiceConstants
{
	/// When a message is bigger than this value, it is split.
	/// Retroshare itself splits to 15000 bytes (which is dumb),
	/// instead we split to slightly smaller than the buffer
	/// allocated by its PQI Streamer (262_143 bytes), which is
	/// the maximum packet size that it can send or receive.
	public static final int MESSAGE_SPLIT_SLICE_SIZE_MAX = 260_000;

	private RsServiceConstants()
	{
		throw new UnsupportedOperationException("Utility class");
	}
}
