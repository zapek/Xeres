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

package io.xeres.ui.model.file;

import io.xeres.common.file.FileType;

import java.util.Objects;

// XXX: missing children, parent? actually I should rename this because it's more like a Turtle/FT FileSearch than a File on disk..
public record File(
		String name,
		long size,
		FileType type,
		String hash
)
{
	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		File file = (File) o;
		return Objects.equals(hash, file.hash);
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(hash);
	}
}
