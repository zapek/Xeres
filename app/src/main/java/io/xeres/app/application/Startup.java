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

package io.xeres.app.application;

import io.xeres.app.XeresApplication;
import io.xeres.app.application.autostart.AutoStart;
import io.xeres.app.application.events.LocationReadyEvent;
import io.xeres.app.application.events.SettingsChangedEvent;
import io.xeres.app.configuration.DataDirConfiguration;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.settings.Settings;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.*;
import io.xeres.app.service.UiBridgeService.SplashStatus;
import io.xeres.app.service.notification.file.FileNotificationService;
import io.xeres.app.service.notification.status.StatusNotificationService;
import io.xeres.app.service.shell.ShellService;
import io.xeres.app.xrs.service.identity.IdentityManager;
import io.xeres.common.events.ConnectWebSocketsEvent;
import io.xeres.common.events.StartupEvent;
import io.xeres.common.mui.MinimalUserInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Component
public class Startup implements ApplicationRunner
{
	private static final Logger log = LoggerFactory.getLogger(Startup.class);

	private final LocationService locationService;
	private final SettingsService settingsService;
	private final DatabaseSessionManager databaseSessionManager;
	private final DataDirConfiguration dataDirConfiguration;
	private final NetworkService networkService;
	private final PeerConnectionManager peerConnectionManager;
	private final UiBridgeService uiBridgeService;
	private final IdentityManager identityManager;
	private final StatusNotificationService statusNotificationService;
	private final AutoStart autoStart;
	private final ShellService shellService;
	private final FileNotificationService fileNotificationService;
	private final InfoService infoService;
	private final UpgradeService upgradeService;
	private final ApplicationEventPublisher publisher;

	public Startup(LocationService locationService, SettingsService settingsService, DatabaseSessionManager databaseSessionManager, DataDirConfiguration dataDirConfiguration, NetworkService networkService, PeerConnectionManager peerConnectionManager, UiBridgeService uiBridgeService, IdentityManager identityManager, StatusNotificationService statusNotificationService, AutoStart autoStart, ShellService shellService, FileNotificationService fileNotificationService, InfoService infoService, UpgradeService upgradeService, ApplicationEventPublisher publisher)
	{
		this.locationService = locationService;
		this.settingsService = settingsService;
		this.databaseSessionManager = databaseSessionManager;
		this.dataDirConfiguration = dataDirConfiguration;
		this.networkService = networkService;
		this.peerConnectionManager = peerConnectionManager;
		this.uiBridgeService = uiBridgeService;
		this.identityManager = identityManager;
		this.statusNotificationService = statusNotificationService;
		this.autoStart = autoStart;
		this.shellService = shellService;
		this.fileNotificationService = fileNotificationService;
		this.infoService = infoService;
		this.upgradeService = upgradeService;
		this.publisher = publisher;
	}

	@Override
	public void run(ApplicationArguments args)
	{
		// This is a convenient place to start code as it works in both UI and non-UI mode
		infoService.showStartupInfo();
		checkRequirements();
		infoService.showCapabilities();
		infoService.showFeatures();
		infoService.showDebug();

		publisher.publishEvent(new StartupEvent());    // This is synchronous and allows WebClients to configure themselves.

		if (XeresApplication.isRemoteUiClient())
		{
			log.info("Remote UI mode");
			publisher.publishEvent(new ConnectWebSocketsEvent()); // Make sure the websockets connect
			return;
		}

		upgradeService.upgrade();

		if (networkService.checkReadiness())
		{
			uiBridgeService.setSplashStatus(SplashStatus.NETWORK);
		}
		else
		{
			log.info("Waiting... Use the user interface to send commands to create a profile");
			uiBridgeService.closeSplashScreen();
		}
	}

	/**
	 * Called when the application setup is ready (aka we have a location).
	 *
	 * @param ignoredEvent the {@link LocationReadyEvent}
	 */
	@EventListener
	public void onApplicationEvent(LocationReadyEvent ignoredEvent)
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			syncAutoStart();
			statusNotificationService.setTotalUsers((int) locationService.countLocations());
			networkService.start();
		}
		MinimalUserInterface.setShell(shellService);
		uiBridgeService.closeSplashScreen();
	}

	@EventListener
	public void onSettingsChangedEvent(SettingsChangedEvent event)
	{
		compareSettingsAndApplyActions(event.oldSettings(), event.newSettings());
	}

	@EventListener // We don't use @PreDestroy because netty uses other beans on shutdown, and we don't want them in shutdown state already
	public void onApplicationEvent(ContextClosedEvent ignored)
	{
		backupUserData();

		log.info("Shutting down...");
		identityManager.shutdown();
		peerConnectionManager.shutdown();

		statusNotificationService.shutdown();
		fileNotificationService.shutdown();

		networkService.stop();
	}

	private void backupUserData()
	{
		if (dataDirConfiguration.getDataDir() != null) // Don't back up the database when running unit tests
		{
			settingsService.backup(dataDirConfiguration.getDataDir());
		}
	}

	private static void checkRequirements()
	{
		if (Charset.defaultCharset() != StandardCharsets.UTF_8)
		{
			throw new IllegalArgumentException("Platform charset must be UTF-8, found: " + Charset.defaultCharset());
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
}
