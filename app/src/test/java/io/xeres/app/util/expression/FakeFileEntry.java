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

package io.xeres.app.util.expression;

import io.xeres.common.id.Sha1Sum;

class FakeFileEntry implements FileEntry
{
	private final String name;
	private final long size;
	private final int lastModified;
	private final int popularity;
	private final String parentPath;
	private final Sha1Sum hash;

	public FakeFileEntry(String name, long size, int lastModified, int popularity, String parentPath, Sha1Sum hash)
	{
		this.name = name;
		this.size = size;
		this.lastModified = lastModified;
		this.popularity = popularity;
		this.parentPath = parentPath;
		this.hash = hash;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public long getSize()
	{
		return size;
	}

	@Override
	public int getLastModified()
	{
		return lastModified;
	}

	@Override
	public int getPopularity()
	{
		return popularity;
	}

	@Override
	public String getParentPath()
	{
		return parentPath;
	}

	@Override
	public Sha1Sum getHash()
	{
		return hash;
	}
}
