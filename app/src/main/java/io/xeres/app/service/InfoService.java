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

package io.xeres.app.service;

import io.netty.util.ResourceLeakDetector;
import io.xeres.app.properties.NetworkProperties;
import io.xeres.app.xrs.service.RsServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.util.stream.Collectors;

@Service
public class InfoService
{
	private static final Logger log = LoggerFactory.getLogger(InfoService.class);

	private final BuildProperties buildProperties;
	private final Environment environment;
	private final NetworkProperties networkProperties;
	private final RsServiceRegistry rsServiceRegistry;

	public InfoService(BuildProperties buildProperties, Environment environment, NetworkProperties networkProperties, RsServiceRegistry rsServiceRegistry)
	{
		this.buildProperties = buildProperties;
		this.environment = environment;
		this.networkProperties = networkProperties;
		this.rsServiceRegistry = rsServiceRegistry;
	}

	public void showStartupInfo()
	{
		log.info("Startup sequence ({}, {}, {})",
				buildProperties.getName(),
				buildProperties.getVersion(),
				environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "prod");
	}

	public void showCapabilities()
	{
		var totalMemory = Runtime.getRuntime().totalMemory();
		log.info("OS: {} ({})", System.getProperty("os.name"), System.getProperty("os.arch"));
		log.info("JRE: {} {} ({})", System.getProperty("java.vendor"), System.getProperty("java.version"), System.getProperty("java.home"));
		log.info("Charset: {}", Charset.defaultCharset());
		log.debug("Working directory: {}", log.isDebugEnabled() ? System.getProperty("user.dir") : "");
		log.info("Number of processor threads: {}", Runtime.getRuntime().availableProcessors());
		log.info("Memory allocated for the JVM: {} MB", totalMemory / 1024 / 1024);
		log.info("Maximum allocatable memory: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
	}

	public void showFeatures()
	{
		if (log.isDebugEnabled())
		{
			log.debug("Network features: {}", networkProperties.getFeatures());
			log.debug("Services: {}", rsServiceRegistry.getServices().stream().map(rsService -> rsService.getServiceType().getName()).collect(Collectors.joining(", ")));
		}
	}

	public void showDebug()
	{
		if (!log.isDebugEnabled())
		{
			return;
		}

		if (ResourceLeakDetector.isEnabled())
		{
			log.debug("Netty leak detector level: {}", ResourceLeakDetector.getLevel());
		}
		else
		{
			log.debug("Netty leak detector disabled");
		}
	}
}
