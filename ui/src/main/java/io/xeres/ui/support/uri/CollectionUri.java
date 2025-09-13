/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.support.uri;

public record CollectionUri(String name, long size, String radix, int count) implements Uri
{
	static final String AUTHORITY = "collection";

	static final String PARAMETER_NAME = "name";
	static final String PARAMETER_SIZE = "size";
	static final String PARAMETER_RADIX = "radix";
	static final String PARAMETER_FILES = "files";

	@Override
	public String toUriString()
	{
		return Uri.buildUri(AUTHORITY,
				PARAMETER_NAME, name,
				PARAMETER_SIZE, String.valueOf(size),
				PARAMETER_RADIX, radix,
				PARAMETER_FILES, String.valueOf(count));
	}

	@Override
	public String toString()
	{
		return toUriString();
	}
}
