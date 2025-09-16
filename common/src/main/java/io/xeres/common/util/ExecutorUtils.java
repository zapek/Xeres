/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ExecutorUtils
{
	private static final Logger log = LoggerFactory.getLogger(ExecutorUtils.class);

	private ExecutorUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ScheduledExecutorService createFixedRateExecutor(NoSuppressedRunnable command, long period)
	{
		return createFixedRateExecutor(command, period, period);
	}

	public static ScheduledExecutorService createFixedRateExecutor(NoSuppressedRunnable command, long initialDelay, long period)
	{
		var executorService = Executors.newSingleThreadScheduledExecutor();

		executorService.scheduleAtFixedRate(command,
				initialDelay,
				period,
				TimeUnit.SECONDS);

		return executorService;
	}

	public static void cleanupExecutor(ScheduledExecutorService executorService)
	{
		if (executorService != null)
		{
			executorService.shutdownNow();
			try
			{
				var success = executorService.awaitTermination(2, TimeUnit.SECONDS);
				if (!success)
				{
					log.warn("Executor {} failed to terminate during the waiting period", executorService);
				}
			}
			catch (InterruptedException _)
			{
				Thread.currentThread().interrupt();
			}
		}
	}
}
