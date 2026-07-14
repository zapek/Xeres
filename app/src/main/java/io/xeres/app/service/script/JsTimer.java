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

package io.xeres.app.service.script;

import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class JsTimer
{
	private static final Logger log = LoggerFactory.getLogger(JsTimer.class);

	private final Map<Integer, ScheduledFuture<?>> timers = new HashMap<>();
	private int idCounter;

	private final ScheduledExecutorService executorService;

	@FunctionalInterface
	interface SetFunction
	{
		@SuppressWarnings("unused")
		int execute(Value callback, int delay);
	}

	@FunctionalInterface
	interface ClearFunction
	{
		@SuppressWarnings("unused")
		void execute(int id);
	}

	JsTimer(ScheduledExecutorService executorService)
	{
		this.executorService = executorService;
	}

	public int setTimeout(Value function, int delay)
	{
		int id = ++idCounter;

		var future = executorService.schedule(() ->
		{
			try
			{
				function.execute();
			}
			finally
			{
				timers.remove(id);
			}
		}, delay, TimeUnit.MILLISECONDS);
		timers.put(id, future);
		return id;
	}

	public int setInterval(Value function, int delay)
	{
		int id = ++idCounter;
		var future = executorService.scheduleAtFixedRate(() ->
		{
			try
			{
				function.execute();
			}
			catch (Exception e)
			{
				// Log error but keep interval running unless explicitly cleared
				log.error(e.getMessage(), e);
			}
		}, delay, delay, TimeUnit.MILLISECONDS);
		timers.put(id, future);
		return id;
	}

	public void clearInterval(int id)
	{
		var future = timers.remove(id);
		if (future != null)
		{
			future.cancel(false);
		}
	}

	public void clearTimeout(int id)
	{
		var future = timers.remove(id);
		if (future != null)
		{
			future.cancel(false);
		}
	}
}
