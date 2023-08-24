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
import io.xeres.app.application.events.*;
import io.xeres.app.configuration.DataDirConfiguration;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.database.model.settings.Settings;
import io.xeres.app.net.bdisc.BroadcastDiscoveryService;
import io.xeres.app.net.dht.DhtService;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.net.upnp.UPNPService;
import io.xeres.app.properties.NetworkProperties;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.PeerService;
import io.xeres.app.service.SettingsService;
import io.xeres.app.service.notification.status.StatusNotificationService;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.identity.IdentityManager;
import io.xeres.common.AppName;
import io.xeres.common.properties.StartupProperties;
import io.xeres.common.protocol.ip.IP;
import io.xeres.ui.support.splash.SplashService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.xeres.common.properties.StartupProperties.Property.SERVER_PORT;
import static java.util.function.Predicate.not;

@Component
public class Startup implements ApplicationRunner
{
	private static final Logger log = LoggerFactory.getLogger(Startup.class);

	private final PeerService peerService;
	private final UPNPService upnpService;
	private final BroadcastDiscoveryService broadcastDiscoveryService;
	private final DhtService dhtService;
	private final LocationService locationService;
	private final SettingsService settingsService;
	private final BuildProperties buildProperties;
	private final Environment environment;
	private final ApplicationEventPublisher publisher;
	private final NetworkProperties networkProperties;
	private final DatabaseSessionManager databaseSessionManager;
	private final DataDirConfiguration dataDirConfiguration;
	private final PeerConnectionManager peerConnectionManager;
	private final SplashService splashService;
	private final IdentityManager identityManager;
	private final StatusNotificationService statusNotificationService;
	private final AutoStart autoStart;
	private final RsServiceRegistry rsServiceRegistry;

	public Startup(PeerService peerService, UPNPService upnpService, BroadcastDiscoveryService broadcastDiscoveryService, DhtService dhtService, LocationService locationService, SettingsService settingsService, BuildProperties buildProperties, Environment environment, ApplicationEventPublisher publisher, NetworkProperties networkProperties, DatabaseSessionManager databaseSessionManager, DataDirConfiguration dataDirConfiguration, PeerConnectionManager peerConnectionManager, SplashService splashService, IdentityManager identityManager, StatusNotificationService statusNotificationService, AutoStart autoStart, RsServiceRegistry rsServiceRegistry)
	{
		this.peerService = peerService;
		this.upnpService = upnpService;
		this.broadcastDiscoveryService = broadcastDiscoveryService;
		this.dhtService = dhtService;
		this.locationService = locationService;
		this.settingsService = settingsService;
		this.buildProperties = buildProperties;
		this.environment = environment;
		this.publisher = publisher;
		this.networkProperties = networkProperties;
		this.databaseSessionManager = databaseSessionManager;
		this.dataDirConfiguration = dataDirConfiguration;
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

		if (settingsService.isOwnProfilePresent())
		{
			splashService.status("Starting network");
			try (var ignored = new DatabaseSession(databaseSessionManager))
			{
				var location = locationService.findOwnLocation().orElseThrow();
				var localIpAddress = Optional.ofNullable(IP.getLocalIpAddress()).orElseThrow(() -> new IllegalStateException("Current host has no IP address. Please configure your network."));

				// If there's no --server-port specified, get the previously saved port. If there isn't any because there was an
				// error on initialization, simply try to get a new one.
				int localPort = Optional.ofNullable(StartupProperties.getInteger(SERVER_PORT))
						.orElseGet(() -> location.getConnections().stream()
								.filter(not(Connection::isExternal))
								.findFirst()
								.orElseGet(() -> Connection.from(PeerAddress.from(localIpAddress, IP.getFreeLocalPort())))
								.getPort());

				// Send the event asynchronously so that our transaction can complete first
				publisher.publishEvent(new LocationReadyEvent(localIpAddress, localPort));
			}
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
			settingsService.setLocalIpAddressAndPort(event.localIpAddress(), event.localPort());
			syncAutoStart();
			statusNotificationService.setTotalUsers((int) locationService.getAllLocations().stream().filter(location -> !location.isOwn()).count());
			startNetworkServices();
		}
		splashService.close();
	}

	@EventListener
	public void onSettingsChangedEvent(SettingsChangedEvent event)
	{
		compareSettingsAndApplyActions(event.oldSettings(), event.newSettings());
	}

	@EventListener
	public void onIpChangedEvent(IpChangedEvent event)
	{
		log.warn("IP change event received, possibly restarting some services...");
		settingsService.setLocalIpAddress(event.localIpAddress());

		if (settingsService.isUpnpEnabled())
		{
			if (settingsService.isDhtEnabled())
			{
				dhtService.stop();
			}
			upnpService.stop();
			upnpService.start(settingsService.getLocalIpAddress(), settingsService.getLocalPort());
		}
		else
		{
			if (settingsService.isDhtEnabled())
			{
				dhtService.stop();
				dhtService.start(locationService.findOwnLocation().orElseThrow().getLocationId(), settingsService.getLocalPort());
			}
		}

		if (settingsService.isBroadcastDiscoveryEnabled())
		{
			broadcastDiscoveryService.stop();
			broadcastDiscoveryService.start(settingsService.getLocalIpAddress(), settingsService.getLocalPort());
		}
	}

	@EventListener
	public void onPortsForwarded(PortsForwardedEvent event)
	{
		log.info("Ports forwarded on the router");

		if (settingsService.isDhtEnabled())
		{
			dhtService.start(locationService.findOwnLocation().orElseThrow().getLocationId(), event.localPort());
		}
	}

	@EventListener
	public void onDhtReady(DhtReadyEvent event)
	{
		// Unused for now
	}

	@EventListener // We don't use @PreDestroy because netty uses other beans on shutdown, and we don't want them in shutdown state already
	public void onApplicationEvent(ContextClosedEvent event)
	{
		backupUserData();

		log.info("Shutting down...");
		identityManager.shutdown();
		peerConnectionManager.shutdown();

		statusNotificationService.shutdown();

		stopNetworkServices();
	}

	void startNetworkServices()
	{
		var localIpAddress = settingsService.getLocalIpAddress();
		var localPort = settingsService.getLocalPort();

		locationService.markAllConnectionsAsDisconnected();

		log.info("Starting network services...");
		var ownAddress = PeerAddress.from(localIpAddress, localPort);
		if (ownAddress.isValid())
		{
			locationService.updateConnection(locationService.findOwnLocation().orElseThrow(), ownAddress);
			if (ownAddress.isLAN())
			{
				if (settingsService.isUpnpEnabled())
				{
					upnpService.start(localIpAddress, localPort);
				}
				else
				{
					if (settingsService.isDhtEnabled())
					{
						dhtService.start(locationService.findOwnLocation().orElseThrow().getLocationId(), localPort);
					}
				}
				if (settingsService.isBroadcastDiscoveryEnabled())
				{
					broadcastDiscoveryService.start(localIpAddress, localPort);
				}
			}
			peerService.start(localPort);

			// Send the event asynchronously so that our transaction can complete first
			publisher.publishEvent(new NetworkReadyEvent());
		}
		else
		{
			log.error("Local address is invalid: {}, can't start network services", localIpAddress);
		}
	}

	void stopNetworkServices()
	{
		dhtService.stop();
		upnpService.stop();
		broadcastDiscoveryService.stop();
		peerService.stop();

		upnpService.waitForTermination();
	}

	private void backupUserData()
	{
		// Right now we perform a backup on every shutdown, see #26 for possible improvements
		if (dataDirConfiguration.getDataDir() != null)
		{
			var backupFile = Path.of(dataDirConfiguration.getDataDir(), "backup.zip").toString();
			log.info("Doing backup of database to {}", backupFile);
			settingsService.backup(backupFile);
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
		log.debug("Working directory: {}", System.getProperty("user.dir"));
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
		applyBroadcastDiscovery(oldSettings, newSettings);
		applyDht(oldSettings, newSettings);
		applyUpnp(oldSettings, newSettings);
		applyTor(oldSettings, newSettings);
		applyI2p(oldSettings, newSettings);
		applyAutoStart(oldSettings, newSettings);
	}

	private void applyBroadcastDiscovery(Settings oldSettings, Settings newSettings)
	{
		if (newSettings.isBroadcastDiscoveryEnabled() != oldSettings.isBroadcastDiscoveryEnabled())
		{
			if (newSettings.isBroadcastDiscoveryEnabled())
			{
				broadcastDiscoveryService.start(newSettings.getLocalIpAddress(), newSettings.getLocalPort());
			}
			else
			{
				broadcastDiscoveryService.stop();
			}
		}
	}

	private void applyDht(Settings oldSettings, Settings newSettings)
	{
		if (newSettings.isDhtEnabled() != oldSettings.isDhtEnabled())
		{
			if (newSettings.isDhtEnabled())
			{
				dhtService.start(locationService.findOwnLocation().orElseThrow().getLocationId(), newSettings.getLocalPort());
			}
			else
			{
				dhtService.stop();
			}
		}
	}

	private void applyUpnp(Settings oldSettings, Settings newSettings)
	{
		if (newSettings.isUpnpEnabled() != oldSettings.isUpnpEnabled())
		{
			if (newSettings.isUpnpEnabled())
			{
				upnpService.start(newSettings.getLocalIpAddress(), newSettings.getLocalPort());
			}
			else
			{
				upnpService.stop();
			}
		}
	}

	private void applyTor(Settings oldSettings, Settings newSettings)
	{
		if (!StringUtils.equals(newSettings.getTorSocksHost(), oldSettings.getTorSocksHost()) || newSettings.getTorSocksPort() != oldSettings.getTorSocksPort())
		{
			peerService.restartTor();
		}
	}

	private void applyI2p(Settings oldSettings, Settings newSettings)
	{
		if (!StringUtils.equals(newSettings.getI2pSocksHost(), oldSettings.getI2pSocksHost()) || newSettings.getI2pSocksPort() != oldSettings.getI2pSocksPort())
		{
			peerService.restartI2p();
		}
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
