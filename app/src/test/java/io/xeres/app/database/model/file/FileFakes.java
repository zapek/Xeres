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

package io.xeres.app.database.model.file;

import io.xeres.common.id.Sha1Sum;

import java.time.Instant;

public final class FileFakes
{
	private FileFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static File createFile(String name)
	{
		return createFile(name, null);
	}

	public static File createFile(String name, File parent)
	{
		var file = new File();
		file.setName(name);

		if (parent != null)
		{
			file.setParent(parent);
		}
		return file;
	}

	public static File createFile(String name, long size)
	{
		return createFile(name, size, null);
	}

	public static File createFile(String name, long size, Instant modified)
	{
		return createFile(name, size, modified, null);
	}

	public static File createFile(String name, long size, Instant modified, Sha1Sum hash)
	{
		var file = new File();
		file.setName(name);
		file.setSize(size);
		file.setModified(modified);
		file.setHash(hash);
		return file;
	}
}
