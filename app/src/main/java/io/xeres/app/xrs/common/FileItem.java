/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.common;

import io.xeres.common.id.Sha1Sum;

import java.util.Set;

public record FileItem(
		long size,
		Sha1Sum hash,
		String name,
		String path,
		int popularity,
		int age,
		int pieceSize,
		Set<Sha1Sum> chunkHashes)
{
	@Override
	public String toString()
	{
		return "FileItem{" +
				"size=" + size +
				", hash=" + hash +
				", name='" + name + '\'' +
				", path='" + path + '\'' +
				", popularity=" + popularity +
				", age=" + age +
				", pieceSize=" + pieceSize +
				", chunkHashes=" + chunkHashes +
				'}';
	}
}
