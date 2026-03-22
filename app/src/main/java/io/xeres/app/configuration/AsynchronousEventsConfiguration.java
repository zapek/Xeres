/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.configuration;

import io.xeres.common.events.SynchronousEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.ResolvableType;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.concurrent.RejectedExecutionException;

/**
 * This configuration makes the events asynchronous, that is, the method
 * publishing them will return immediately instead of blocking. If you want synchronous events,
 * just make them implement SynchronousEvent.
 */
@Configuration
public class AsynchronousEventsConfiguration
{
	@Bean(name = "applicationEventMulticaster")
	public ApplicationEventMulticaster simpleApplicationEventMulticaster()
	{
		var eventMulticaster = new SimpleApplicationEventMulticaster()
		{
			@Override
			public void multicastEvent(ApplicationEvent event, ResolvableType eventType)
			{
				var type = eventType != null ? eventType : ResolvableType.forInstance(event);
				var executor = getTaskExecutor();

				for (ApplicationListener<?> listener : getApplicationListeners(event, type))
				{
					if (!(executor != null && listener.supportsAsyncExecution() && !isSynchronousEvent(event)))
					{
						invokeListener(listener, event);
						return;
					}
					try
					{
						executor.execute(() -> invokeListener(listener, event));
					}
					catch (RejectedExecutionException _)
					{
						invokeListener(listener, event);
					}
				}
			}
		};
		eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
		return eventMulticaster;
	}

	private static boolean isSynchronousEvent(ApplicationEvent event)
	{
		if (event instanceof SynchronousEvent)
		{
			return true;
		}
		//noinspection RedundantIfStatement
		if (event instanceof PayloadApplicationEvent && ((PayloadApplicationEvent<?>) event).getPayload() instanceof SynchronousEvent)
		{
			return true;
		}
		return false;
	}
}
