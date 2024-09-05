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

package io.xeres.app.application.environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public final class TemporaryFiles
{
	private static final Logger log = LoggerFactory.getLogger(TemporaryFiles.class);

	private TemporaryFiles()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Deletes leftover of temporary files.
	 * <p>
	 * This should be called on startup, otherwise the deletion will fail because the file is in use and there's no
	 * way to unload a DLL before doing so. For now, we care about netty because it leaves a 2.7 MB file on every
	 * run and that can become significant after many runs.
	 */
	public static void cleanup()
	{
		var tempDir = new File(System.getProperty("java.io.tmpdir"));
		if (!tempDir.exists() || !tempDir.isDirectory())
		{
			log.warn("Temporary directory {} doesn't exist or is not a directory", tempDir);
			return;
		}

		var tempFiles = tempDir.listFiles((dir, name) -> name.startsWith("netty_tcnative_"));
		if (tempFiles != null)
		{
			for (var tempFile : tempFiles)
			{
				var deleted = tempFile.delete();
				log.debug("Trying to delete temporary file {}: {}", tempFile, deleted ? "OK" : "FAIL");
			}
		}
	}
}
