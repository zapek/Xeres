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

package io.xeres.common.util;

import org.slf4j.LoggerFactory;

/**
 * This interface should be used instead of Runnable for executors so that any
 * exception is printed. If it's a scheduled executor, it will also keep running.
 * <br>
 * Example:
 * {@snippet :
 * executorService.scheduleAtFixedRate((NoSuppressedRunnable) this::manageChatRooms, 10, 10, TimeUnit.SECONDS);
 *}
 */
@FunctionalInterface
public interface NoSuppressedRunnable extends Runnable
{
	@Override
	default void run()
	{
		try
		{
			doRun();
		}
		catch (Exception e)
		{
			LoggerFactory.getLogger(NoSuppressedRunnable.class).error("Exception in executor: ", e);
		}
	}

	void doRun();
}
