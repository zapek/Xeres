/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public final class ThreadUtils
{
	private static final Logger log = LoggerFactory.getLogger(ThreadUtils.class);

	private ThreadUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void waitForThread(Thread thread)
	{
		if (thread == null)
		{
			return;
		}

		try
		{
			if (!thread.join(Duration.ofSeconds(5)))
			{
				log.warn("Thread {} timed out", thread.getName());
			}
		}
		catch (InterruptedException e)
		{
			log.error("Failed to wait for termination on thread {}: {}", thread.getName(), e.getMessage(), e);
			Thread.currentThread().interrupt();
		}
	}
}
