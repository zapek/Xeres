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

package io.xeres.app.configuration;

import io.xeres.app.application.environment.LocalPortFinder;
import io.xeres.app.service.SettingsService;
import io.xeres.common.properties.StartupProperties;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import static io.xeres.common.properties.StartupProperties.Property.CONTROL_PORT;

@Configuration
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>
{
	private final SettingsService settingsService;

	public WebServerConfiguration(SettingsService settingsService)
	{
		this.settingsService = settingsService;
	}

	@Override
	public void customize(ConfigurableServletWebServerFactory factory)
	{
		// If we are allowing remote access, bind to all interfaces
		if (StartupProperties.Property.CONTROL_ADDRESS.isUnset() && settingsService.isRemoteEnabled())
		{
			factory.setAddress(getAllInterfaces());
		}

		// If the port configured in the settings is different from CONTROL_PORT, then use it instead.
		if (StartupProperties.Property.CONTROL_PORT.isUnset() && settingsService.hasRemotePortConfigured() && settingsService.getRemotePort() != Objects.requireNonNull(StartupProperties.getInteger(StartupProperties.Property.CONTROL_PORT)))
		{
			StartupProperties.setPort(CONTROL_PORT, String.valueOf(settingsService.getRemotePort()), StartupProperties.Origin.PROPERTY);
			LocalPortFinder.ensureFreePort();
			factory.setPort(Objects.requireNonNull(StartupProperties.getInteger(StartupProperties.Property.CONTROL_PORT)));
		}
	}

	private static InetAddress getAllInterfaces()
	{
		try
		{
			return InetAddress.getByAddress(new byte[]{0, 0, 0, 0});
		}
		catch (UnknownHostException e)
		{
			throw new RuntimeException(e);
		}
	}
}
