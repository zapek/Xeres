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

package io.xeres.app.application;

import io.netty.util.ResourceLeakDetector;
import io.xeres.app.XeresApplication;
import io.xeres.app.application.events.LocationReadyEvent;
import io.xeres.app.configuration.DataDirConfiguration;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.net.bdisc.BroadcastDiscoveryService;
import io.xeres.app.net.dht.DHTService;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.net.upnp.UPNPService;
import io.xeres.app.properties.NetworkProperties;
import io.xeres.app.service.ChatRoomService;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.PeerService;
import io.xeres.app.service.PrefsService;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.common.AppName;
import io.xeres.common.properties.StartupProperties;
import io.xeres.common.protocol.ip.IP;
import io.xeres.ui.support.splash.SplashService;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Component
public class Startup implements ApplicationRunner
{
	private static final Logger log = LoggerFactory.getLogger(Startup.class);

	private final PeerService peerService;
	private final UPNPService upnpService;
	private final BroadcastDiscoveryService broadcastDiscoveryService;
	private final DHTService dhtService;
	private final LocationService locationService;
	private final PrefsService prefsService;
	private final BuildProperties buildProperties;
	private final Environment environment;
	private final ApplicationEventPublisher publisher;
	private final NetworkProperties networkProperties;
	private final DatabaseSessionManager databaseSessionManager;
	private final DataDirConfiguration dataDirConfiguration;
	private final ChatRoomService chatRoomService;
	private final PeerConnectionManager peerConnectionManager;
	private final SplashService splashService;

	public Startup(PeerService peerService, UPNPService upnpService, BroadcastDiscoveryService broadcastDiscoveryService, DHTService dhtService, LocationService locationService, PrefsService prefsService, BuildProperties buildProperties, Environment environment, ApplicationEventPublisher publisher, NetworkProperties networkProperties, DatabaseSessionManager databaseSessionManager, DataDirConfiguration dataDirConfiguration, ChatRoomService chatRoomService, PeerConnectionManager peerConnectionManager, SplashService splashService)
	{
		this.peerService = peerService;
		this.upnpService = upnpService;
		this.broadcastDiscoveryService = broadcastDiscoveryService;
		this.dhtService = dhtService;
		this.locationService = locationService;
		this.prefsService = prefsService;
		this.buildProperties = buildProperties;
		this.environment = environment;
		this.publisher = publisher;
		this.networkProperties = networkProperties;
		this.databaseSessionManager = databaseSessionManager;
		this.dataDirConfiguration = dataDirConfiguration;
		this.chatRoomService = chatRoomService;
		this.peerConnectionManager = peerConnectionManager;
		this.splashService = splashService;
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

		if (prefsService.isOwnProfilePresent())
		{
			splashService.status("Starting network");
			try (var session = new DatabaseSession(databaseSessionManager))
			{
				var location = locationService.findOwnLocation().orElseThrow();
				String localIpAddress = Optional.ofNullable(IP.getLocalIpAddress()).orElseThrow(() -> new IllegalStateException("Current host has no IP address. Please configure your network"));

				// If there's no --server-port specified, get the previously saved port. If there isn't any because there was an
				// error on initialization, simply try to get a new one.
				int localPort = Optional.ofNullable(StartupProperties.getInteger(StartupProperties.Property.SERVER_PORT))
						.orElseGet(() -> location.getConnections().stream()
								.filter(not(Connection::isExternal))
								.findFirst()
								.orElseGet(() -> Connection.from(PeerAddress.from(localIpAddress, IP.getFreeLocalPort())))
								.getPort());

				// Send the event asynchronously so that our transaction can complete first
				CompletableFuture.runAsync(() -> publisher.publishEvent(new LocationReadyEvent(localIpAddress, localPort)));
			}
		}
		else
		{
			log.info("Waiting... Use the user interface to send commands to create a profile");
			splashService.close();
		}
	}

	/**
	 * Called when the network is ready (aka we have a location).
	 *
	 * @param event the LocationReadyEvent
	 */
	@EventListener
	public void onApplicationEvent(LocationReadyEvent event)
	{
		try (var session = new DatabaseSession(databaseSessionManager))
		{
			locationService.markAllConnectionsAsDisconnected();
			chatRoomService.markAllChatRoomsAsLeft();

			log.info("Starting network services...");
			var ownAddress = PeerAddress.from(event.localIpAddress(), event.localPort());
			if (ownAddress.isValid())
			{
				locationService.updateConnection(locationService.findOwnLocation().orElseThrow(), ownAddress);
				if (ownAddress.isLAN())
				{
					log.info("We are on a LAN. Launching UPNP and Broadcast discovery...");
					upnpService.start(event.localIpAddress(), event.localPort());
					broadcastDiscoveryService.start(event.localIpAddress(), event.localPort());
				}
				if (networkProperties.isDht())
				{
					dhtService.start(event.localPort());
				}
				peerService.start(event.localPort());
			}
			else
			{
				log.error("Local address is invalid: {}, can't start network services", event.localIpAddress());
			}
		}
		splashService.close();
	}

	@EventListener // We don't use @PreDestroy because netty uses other beans on shutdown and we don't want them in shutdown state already
	public void onApplicationEvent(ContextClosedEvent event)
	{
		backupUserData();

		log.info("Shutting down...");
		peerConnectionManager.shutdown();
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
			prefsService.backup(backupFile);
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
		long totalMemory = Runtime.getRuntime().totalMemory();
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
			log.debug("Services: {}", RsServiceRegistry.getServices().stream().map(rsService -> rsService.getServiceType().getName()).collect(Collectors.joining(", ")));
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
}
