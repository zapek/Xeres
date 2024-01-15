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

package io.xeres.app.service.file;

import io.xeres.app.database.model.file.File;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class TrackingFileVisitor implements FileVisitor<Path>
{
	private List<File> directories = new ArrayList<>();

	public TrackingFileVisitor(Path currentDirectory)
	{
		try
		{
			preVisitDirectory(currentDirectory, Files.readAttributes(currentDirectory, BasicFileAttributes.class));
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
	{
		var directory = File.createDirectory(directories.getLast(), dir.getFileName().toString(), attrs.lastModifiedTime().toInstant());
		directories.addLast(directory);
		return null;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
	{
		return null;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc)
	{
		return null;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc)
	{
		directories.removeLast();
		return null;
	}

	public File getCurrentDirectory()
	{
		return directories.getLast();
	}
}
