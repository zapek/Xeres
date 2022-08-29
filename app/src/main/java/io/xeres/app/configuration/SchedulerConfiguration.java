/*
 * Copyright (c) 2019-2020 by David Gerber - https://zapek.com
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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configuration of the scheduler. Just enables it. We also provide
 * a thread pool because by default it just uses one task for all
 * Scheduled beans.
 */
@Configuration
@EnableScheduling
public class SchedulerConfiguration implements SchedulingConfigurer
{
	public static final int CORE_POOL_SIZE = 3;

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar)
	{
		taskRegistrar.setScheduler(taskExecutor());
	}

	@SuppressWarnings("ContextJavaBeanUnresolvedMethodsInspection")
	@Bean(destroyMethod = "shutdown") // make sure task executor is properly shut down when spring exits
	public Executor taskExecutor()
	{
		return Executors.newScheduledThreadPool(CORE_POOL_SIZE);
	}
}
