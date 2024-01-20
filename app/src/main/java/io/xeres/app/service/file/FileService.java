/*
 * Copyright (c) 2023-2024 by David Gerber - https://zapek.com
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

import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.app.database.model.file.File;
import io.xeres.app.database.model.share.Share;
import io.xeres.app.database.repository.FileRepository;
import io.xeres.app.database.repository.ShareRepository;
import io.xeres.app.service.notification.file.FileNotificationService;
import io.xeres.common.id.Sha1Sum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Service
public class FileService
{
	private static final Logger log = LoggerFactory.getLogger(FileService.class);

	private final FileNotificationService fileNotificationService;

	private final ShareRepository shareRepository;

	private final FileRepository fileRepository;

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

	public FileService(FileNotificationService fileNotificationService, ShareRepository shareRepository, FileRepository fileRepository)
	{
		this.fileNotificationService = fileNotificationService;
		this.shareRepository = shareRepository;
		this.fileRepository = fileRepository;
	}

	public void addShare(Share share)
	{
		saveFullPath(share.getFile());
		shareRepository.save(share); // XXX: check if already here and ignore if so?
		scanShare(share.getFile());
	}

	private void saveFullPath(File file)
	{
		List<File> tree = new ArrayList<>();

		tree.add(file);
		while (file.getParent() != null)
		{
			tree.add(file.getParent());
			file = file.getParent();
		}
		Collections.reverse(tree);
		// XXX: following fails because fileToUpdate.getParent() is not saved to disk (so has no ID).. chicken & egg problem
		tree.forEach(fileToUpdate -> fileRepository.findByNameAndParent(fileToUpdate.getName(), fileToUpdate.getParent()).ifPresent(fileFound -> fileToUpdate.setId(fileFound.getId())));
		fileRepository.saveAll(tree); // XXX: if that resets the last modified... we need to change how we resolve the path and so on
	}

	void scanShare(File directory)
	{
		try
		{
			var directoryPath = getFilePath(directory);
			Files.walkFileTree(directoryPath, new TrackingFileVisitor(directory)
			{
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				{
					Objects.requireNonNull(file);
					Objects.requireNonNull(attrs);
					if (isIndexableFile(file, attrs))
					{
						log.debug("Checking file {}, modification time: {}", file, attrs.lastModifiedTime());
						var currentFile = fileRepository.findByNameAndParent(file.getFileName().toString(), getCurrentDirectory()).orElseGet(() -> File.createFile(getCurrentDirectory(), file.getFileName().toString(), null));
						var lastModified = attrs.lastModifiedTime().toInstant();
						if (currentFile.getModified() == null || lastModified.isAfter(currentFile.getModified()))
						{
							var hash = calculateFileHash(file);
							currentFile.setHash(hash);
							currentFile.setModified(lastModified);
							fileRepository.save(currentFile);
						}
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
						super.preVisitDirectory(dir, attrs);
						log.debug("Entering directory {}", dir);
						var directory = getCurrentDirectory();
						if (fileRepository.findByNameAndParent(directory.getName(), directory.getParent()).isEmpty())
						{
							fileRepository.save(directory);
						}
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
					super.postVisitDirectory(dir, exc);
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
			directory.setModified(Files.getLastModifiedTime(directoryPath).toInstant());
			fileRepository.save(directory);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private Path getFilePath(File file)
	{
		if (file.hasParent())
		{
			return getFilePath(file.getParent()).resolve(file.getName());
		}
		return Path.of(file.getName());
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
