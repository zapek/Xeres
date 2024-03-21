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
import io.xeres.app.database.repository.FileRepository;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class TrackingFileVisitor implements FileVisitor<Path>
{
	private final FileRepository fileRepository;
	private boolean skipFirst; // XXX: lame hack, find something better (this is because the first entered directory is already the root directory)
	private final List<File> directories = new ArrayList<>();
	private boolean foundChanges;

	public TrackingFileVisitor(FileRepository fileRepository, File rootDirectory)
	{
		this.fileRepository = fileRepository;
		directories.addLast(rootDirectory);
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
	{
		if (!skipFirst)
		{
			skipFirst = true;
		}
		else
		{
			var directory = fileRepository.findByNameAndParent(dir.getFileName().toString(), directories.getLast()).orElseGet(() -> File.createDirectory(directories.getLast(), dir.getFileName().toString(), attrs.lastModifiedTime().toInstant()));
			directories.addLast(directory);
		}
		return FileVisitResult.CONTINUE;
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

	public boolean foundChanges()
	{
		return foundChanges;
	}

	void setChanged()
	{
		foundChanges = true;
	}
}
