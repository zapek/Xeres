/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.app.service;

import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.app.service.notification.file.FileNotificationService;
import io.xeres.common.id.Sha1Sum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Objects;

@Service
public class FileService
{
	private static final Logger log = LoggerFactory.getLogger(FileService.class);

	private final FileNotificationService fileNotificationService;

	private static final String[] ignoredSuffixes = {
			".bak",
			".sys",
			".com",
			".class",
			".obj",
			".o",
			".tmp",
			".temp",
			".cache",
			"~"
	};

	private static final String[] ignoredPrefixes = {
			"thumbs",
			"temp."
	};

	public FileService(FileNotificationService fileNotificationService)
	{
		this.fileNotificationService = fileNotificationService;
	}

	public void scanShare(Path directory)
	{
		try
		{
			Files.walkFileTree(directory, new SimpleFileVisitor<>()
			{
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				{
					Objects.requireNonNull(file);
					Objects.requireNonNull(attrs);
					if (isIndexableFile(file, attrs))
					{
						log.debug("Checking file {}, modification time: {}", file, attrs.lastModifiedTime()); // XXX: skip calculation if the modification time is not after what we already had
						calculateFileHash(file); // XXX: store, etc...
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
				{
					Objects.requireNonNull(dir);
					Objects.requireNonNull(attrs);
					if (isIndexableDirectory(dir, attrs))
					{
						log.debug("Entering directory {}", dir); // XXX: add it to the database, we need that for file tree building
						return FileVisitResult.CONTINUE;
					}
					else
					{
						return FileVisitResult.SKIP_SUBTREE;
					}
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc)
				{
					Objects.requireNonNull(dir);
					if (exc != null)
					{
						log.debug("Failed to fully scan directory {}: {}", dir, exc.getMessage());
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc)
				{
					Objects.requireNonNull(file);
					log.debug("Visiting file {} failed: {}", file, exc.getMessage());
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private boolean isIndexableFile(Path file, BasicFileAttributes attrs)
	{
		if (attrs.isRegularFile() && attrs.size() > 0)
		{
			var fileName = file.getFileName().toString();
			return !isIgnoredFile(fileName);
		}
		return false;
	}

	private boolean isIndexableDirectory(Path directory, BasicFileAttributes attrs)
	{
		if (attrs.isDirectory())
		{
			var directoryName = directory.getFileName().toString();
			return !isIgnoredDirectory(directoryName);
		}
		return false;
	}

	private boolean isIgnoredFile(String fileName)
	{
		fileName = fileName.toLowerCase(Locale.ROOT);

		for (var ignoredSuffix : ignoredSuffixes)
		{
			if (fileName.endsWith(ignoredSuffix))
			{
				return true;
			}
		}

		for (var ignoredPrefix : ignoredPrefixes)
		{
			if (fileName.startsWith(ignoredPrefix))
			{
				return true;
			}
		}
		return false;
	}

	private boolean isIgnoredDirectory(String dirName)
	{
		if (dirName.startsWith("."))
		{
			return true;
		}
		return false;
	}

	Sha1Sum calculateFileHash(Path path)
	{
		log.debug("Calculating file hash of file {}", path);
		try (var fc = FileChannel.open(path, StandardOpenOption.READ)) // ExtendedOpenOption.DIRECT is useless for memory mapped files
		{
			var md = new Sha1MessageDigest();

			var size = fc.size();
			var offset = 0L;
			if (size == 0)
			{
				log.debug("File is empty, ignoring");
				return null; // We ignore empty files
			}

			while (size > 0)
			{
				var bufferSize = Math.min(size, Integer.MAX_VALUE);
				var buffer = fc.map(FileChannel.MapMode.READ_ONLY, offset, bufferSize);

				md.update(buffer);
				offset += bufferSize;
				size -= bufferSize;
			}
			log.debug("Calculated hash: {}", md.getSum());
			return md.getSum();
		}
		catch (IOException e)
		{
			log.warn("Error while trying to compute hash of file " + path, e);
			return null;
		}
	}
}
