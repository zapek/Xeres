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
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
public class FileService
{
	private static final Logger log = LoggerFactory.getLogger(FileService.class);

	private final FileNotificationService fileNotificationService;

	public FileService(FileNotificationService fileNotificationService)
	{
		this.fileNotificationService = fileNotificationService;
	}

	Sha1Sum calculateFileHash(Path path)
	{
		try (var fc = FileChannel.open(path, StandardOpenOption.READ)) // ExtendedOpenOption.DIRECT is useless for memory mapped files
		{
			var md = new Sha1MessageDigest();

			var size = fc.size();
			var offset = 0L;
			if (size == 0)
			{
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
			return md.getSum();
		}
		catch (IOException e)
		{
			log.warn("Error while trying to compute hash of file " + path, e);
			return null;
		}
	}
}
