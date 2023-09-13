/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

import io.xeres.app.application.autostart.AutoStarter;
import io.xeres.app.application.autostart.autostarter.AutoStarterGeneric;
import io.xeres.app.application.autostart.autostarter.AutoStarterWindows;
import io.xeres.common.condition.OnWindowsCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * This configuration sets up the autostart feature that
 * starts Xeres when the users logs in.
 */
@Configuration
public class AutoStartConfiguration
{
	@Bean
	@Conditional(OnWindowsCondition.class)
	AutoStarter windowsAutoStarter()
	{
		return new AutoStarterWindows();
	}

	@Bean
	@ConditionalOnMissingBean
	AutoStarter genericAutoStarter()
	{
		return new AutoStarterGeneric();
	}
}
