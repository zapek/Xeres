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

package io.xeres.app.application;

import io.netty.util.ResourceLeakDetector;
import io.xeres.app.XeresApplication;
import io.xeres.app.application.autostart.AutoStart;
import io.xeres.app.application.events.LocationReadyEvent;
import io.xeres.app.application.events.SettingsChangedEvent;
import io.xeres.app.configuration.DataDirConfiguration;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.settings.Settings;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.properties.NetworkProperties;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.NetworkService;
import io.xeres.app.service.SettingsService;
import io.xeres.app.service.notification.status.StatusNotificationService;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.identity.IdentityManager;
import io.xeres.common.AppName;
import io.xeres.ui.support.splash.SplashService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@Component
public class Startup implements ApplicationRunner
{
	private static final Logger log = LoggerFactory.getLogger(Startup.class);

	private final LocationService locationService;
	private final SettingsService settingsService;
	private final BuildProperties buildProperties;
	private final Environment environment;
	private final NetworkProperties networkProperties;
	private final DatabaseSessionManager databaseSessionManager;
	private final DataDirConfiguration dataDirConfiguration;
	private final NetworkService networkService;
	private final PeerConnectionManager peerConnectionManager;
	private final SplashService splashService;
	private final IdentityManager identityManager;
	private final StatusNotificationService statusNotificationService;
	private final AutoStart autoStart;
	private final RsServiceRegistry rsServiceRegistry;

	public Startup(LocationService locationService, SettingsService settingsService, BuildProperties buildProperties, Environment environment, NetworkProperties networkProperties, DatabaseSessionManager databaseSessionManager, DataDirConfiguration dataDirConfiguration, NetworkService networkService, PeerConnectionManager peerConnectionManager, SplashService splashService, IdentityManager identityManager, StatusNotificationService statusNotificationService, AutoStart autoStart, RsServiceRegistry rsServiceRegistry)
	{
		this.locationService = locationService;
		this.settingsService = settingsService;
		this.buildProperties = buildProperties;
		this.environment = environment;
		this.networkProperties = networkProperties;
		this.databaseSessionManager = databaseSessionManager;
		this.dataDirConfiguration = dataDirConfiguration;
		this.networkService = networkService;
		this.peerConnectionManager = peerConnectionManager;
		this.splashService = splashService;
		this.identityManager = identityManager;
		this.statusNotificationService = statusNotificationService;
		this.autoStart = autoStart;
		this.rsServiceRegistry = rsServiceRegistry;
	}

	@Override
	public void run(ApplicationArguments args)
	{
		// This is a convenient place to start code as it works in both UI and non-UI mode
		checkSingleInstance();
		showStartupInfo();
		checkRequirements();
		showCapabilities();
		showFeatures();
		if (log.isDebugEnabled())
		{
			showDebug();
		}

		if (XeresApplication.isRemoteUiClient())
		{
			log.info("Remote UI mode");
			return;
		}

		configureDefaults();

		if (networkService.checkReadiness())
		{
			splashService.status("Starting network");
		}
		else
		{
			log.info("Waiting... Use the user interface to send commands to create a profile");
			splashService.close();
		}
	}

	/**
	 * Called when the application setup is ready (aka we have a location).
	 *
	 * @param event the LocationReadyEvent
	 */
	@EventListener
	public void onApplicationEvent(LocationReadyEvent event)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			syncAutoStart();
			statusNotificationService.setTotalUsers((int) locationService.getAllLocations().stream().filter(location -> !location.isOwn()).count());
			networkService.start();
		}
		splashService.close();
	}

	@EventListener
	public void onSettingsChangedEvent(SettingsChangedEvent event)
	{
		compareSettingsAndApplyActions(event.oldSettings(), event.newSettings());
	}

	@EventListener // We don't use @PreDestroy because netty uses other beans on shutdown, and we don't want them in shutdown state already
	public void onApplicationEvent(ContextClosedEvent event)
	{
		backupUserData();

		log.info("Shutting down...");
		identityManager.shutdown();
		peerConnectionManager.shutdown();

		statusNotificationService.shutdown();

		networkService.stop();
	}

	private void backupUserData()
	{
		if (dataDirConfiguration.getDataDir() != null) // Don't back up the database when running unit tests
		{
			settingsService.backup(dataDirConfiguration.getDataDir());
		}
	}

	private void showStartupInfo()
	{
		log.info("Startup sequence ({}, {}, {})",
				buildProperties.getName(),
				buildProperties.getVersion(),
				environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles()[0] : "prod");
	}

	private void showCapabilities()
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

	private void showFeatures()
	{
		if (log.isDebugEnabled())
		{
			log.debug("Network features: {}", networkProperties.getFeatures());
			log.debug("Services: {}", rsServiceRegistry.getServices().stream().map(rsService -> rsService.getServiceType().getName()).collect(Collectors.joining(", ")));
		}
	}

	private void showDebug()
	{
		if (ResourceLeakDetector.isEnabled())
		{
			log.debug("Netty leak detector level: {}", ResourceLeakDetector.getLevel());
		}
		else
		{
			log.debug("Netty leak detector disabled");
		}
	}

	private void checkRequirements()
	{
		if (Charset.defaultCharset() != StandardCharsets.UTF_8)
		{
			throw new IllegalArgumentException("Platform charset must be UTF-8, found: " + Charset.defaultCharset());
		}
	}

	private void checkSingleInstance()
	{
		if (!SingleInstanceRun.enforceSingleInstance(dataDirConfiguration.getDataDir()))
		{
			throw new IllegalStateException("An instance of " + AppName.NAME + " is already running.");
		}
	}

	private void compareSettingsAndApplyActions(Settings oldSettings, Settings newSettings)
	{
		networkService.compareSettingsAndApplyActions(oldSettings, newSettings);
		applyAutoStart(oldSettings, newSettings);
	}

	private void applyAutoStart(Settings oldSettings, Settings newSettings)
	{
		if (newSettings.isAutoStartEnabled() != oldSettings.isAutoStartEnabled())
		{
			if (newSettings.isAutoStartEnabled())
			{
				autoStart.enable();
			}
			else
			{
				autoStart.disable();
			}
		}
	}

	private void syncAutoStart()
	{
		if (settingsService.isAutoStartEnabled() != autoStart.isEnabled())
		{
			log.info("Autostart is desynced, forcing to {}", settingsService.isAutoStartEnabled());
			if (settingsService.isAutoStartEnabled())
			{
				autoStart.enable();
			}
			else
			{
				autoStart.disable();
			}
		}
	}

	/**
	 * Configures defaults that cannot be done on the database definition because
	 * they depend on some runtime parameters. This is not called in UI client
	 * only mode.
	 */
	private void configureDefaults()
	{
		if (!settingsService.hasIncomingDirectory() && dataDirConfiguration.getDataDir() != null) // Don't do it for tests
		{
			var incomingDirectory = Path.of(dataDirConfiguration.getDataDir(), "Incoming");
			if (Files.notExists(incomingDirectory))
			{
				try
				{
					Files.createDirectory(incomingDirectory);
				}
				catch (IOException e)
				{
					throw new IllegalStateException("Couldn't create incoming directory: " + incomingDirectory + ", :" + e.getMessage());
				}
			}
			settingsService.setIncomingDirectory(incomingDirectory.toString());
		}
	}
}
