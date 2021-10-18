/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.application;


import io.xeres.common.AppName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Locale;
import java.util.Optional;

/**
 * Utility class to detect if an application is already running.
 */
public final class SingleInstanceRun
{
	private static final Logger log = LoggerFactory.getLogger(SingleInstanceRun.class);

	private static final String LOCK_FILE_NAME = "." + AppName.NAME.toLowerCase(Locale.ROOT) + ".lock";

	private static File file;
	private static RandomAccessFile randomAccessFile;
	private static FileLock lock;

	private SingleInstanceRun()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Enforce an application to have a single instance of itself given a certain directory.
	 *
	 * @param dataDir the directory to be used by the application
	 * @return true if the application can run without conflicts, false if it's already running
	 */
	public static boolean enforceSingleInstance(String dataDir)
	{
		file = new File(dataDir, LOCK_FILE_NAME);

		var result = false;
		try
		{
			randomAccessFile = new RandomAccessFile(file, "rw");

			lock = Optional.ofNullable(randomAccessFile.getChannel().tryLock()).orElseThrow(IllegalStateException::new);
			if (lock != null)
			{
				result = true;
				Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));
			}
		}
		catch (IOException | IllegalStateException | IllegalArgumentException e)
		{
			log.debug("Couldn't enforce single instance: {}.", e.getMessage());
		}
		catch (SecurityException e)
		{
			log.warn("Shutdown hook denied by SecurityManager; There will be a dangling lock file at {}", LOCK_FILE_NAME);
		}
		return result;
	}

	private static class ShutdownHook implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				lock.release();
				randomAccessFile.close();
				Files.delete(file.toPath());
			}
			catch (NoSuchFileException e)
			{
				log.warn("Lockfile missing. Was it deleted manually?");
			}
			catch (IOException | SecurityException e)
			{
				log.warn("Failed to delete lockfile: {}. Is it locked by some external process?", e.getClass());
			}
		}
	}
}
