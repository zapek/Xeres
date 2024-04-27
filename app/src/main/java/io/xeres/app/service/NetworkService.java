/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

import io.xeres.app.application.events.IpChangedEvent;
import io.xeres.app.application.events.LocationReadyEvent;
import io.xeres.app.application.events.NetworkReadyEvent;
import io.xeres.app.application.events.UpnpEvent;
import io.xeres.app.database.model.settings.Settings;
import io.xeres.app.net.bdisc.BroadcastDiscoveryService;
import io.xeres.app.net.dht.DhtService;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.net.upnp.UPNPService;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.common.properties.StartupProperties;
import io.xeres.common.protocol.ip.IP;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.xeres.common.properties.StartupProperties.Property.SERVER_PORT;

@Service
public class NetworkService
{
	private static final Logger log = LoggerFactory.getLogger(NetworkService.class);

	private String localIpAddress;

	private final ProfileService profileService;
	private final LocationService locationService;
	private final IdentityRsService identityRsService;
	private final PeerService peerService;
	private final UPNPService upnpService;
	private final BroadcastDiscoveryService broadcastDiscoveryService;
	private final DhtService dhtService;
	private final SettingsService settingsService;
	private final ApplicationEventPublisher publisher;

	private final AtomicBoolean running = new AtomicBoolean();
	private boolean startWhenPossible;

	public NetworkService(ProfileService profileService, LocationService locationService, IdentityRsService identityRsService, PeerService peerService, UPNPService upnpService, BroadcastDiscoveryService broadcastDiscoveryService, DhtService dhtService, SettingsService settingsService, ApplicationEventPublisher publisher)
	{
		this.profileService = profileService;
		this.locationService = locationService;
		this.identityRsService = identityRsService;
		this.peerService = peerService;
		this.upnpService = upnpService;
		this.broadcastDiscoveryService = broadcastDiscoveryService;
		this.dhtService = dhtService;
		this.settingsService = settingsService;
		this.publisher = publisher;
	}

	public boolean checkReadiness()
	{
		if (profileService.hasOwnProfile() && locationService.hasOwnLocation() && identityRsService.hasOwnIdentity())
		{
			configure();
			return true;
		}
		return false;
	}

	private void configure()
	{
		configureLocalPort();
		publisher.publishEvent(new LocationReadyEvent());
	}

	private int configureLocalPort()
	{
		if (settingsService.getLocalPort() == 0)
		{
			var localPort = Optional.ofNullable(StartupProperties.getInteger(SERVER_PORT)).orElseGet(IP::getFreeLocalPort);
			if (localPort != 0)
			{
				log.info("Using local port {}", localPort);
				settingsService.setLocalPort(localPort);
			}
			else
			{
				log.warn("No network available to configure the local port");
			}
		}
		return settingsService.getLocalPort();
	}

	public String getLocalIpAddress()
	{
		return localIpAddress;
	}

	public int getPort()
	{
		return settingsService.getLocalPort();
	}

	public void start()
	{
		if (running.compareAndSet(false, true))
		{
			localIpAddress = IP.getLocalIpAddress();
			var localPort = configureLocalPort();

			locationService.markAllConnectionsAsDisconnected();

			log.info("Starting network services...");
			var ownAddress = PeerAddress.from(localIpAddress, localPort);
			if (ownAddress.isValid())
			{
				locationService.updateConnection(locationService.findOwnLocation().orElseThrow(), ownAddress);
				startHelperServices(ownAddress.isLAN(), false);

				peerService.start(localPort);

				startWhenPossible = false;

				publisher.publishEvent(new NetworkReadyEvent());
			}
			else
			{
				log.error("Local address is invalid: {}, can't start network services", localIpAddress);
				running.set(false);
				startWhenPossible = true;
			}
		}
	}

	private void startHelperServices(boolean isLan, boolean restart)
	{
		if (isLan)
		{
			if (settingsService.isUpnpEnabled())
			{
				if (restart)
				{
					dhtService.stop();
					upnpService.stop();
				}
				upnpService.start(localIpAddress, settingsService.getLocalPort());
			}
			else
			{
				startDhtIfNeeded(restart);
			}
			startBroadcastDiscoveryIfNeeded(restart);
		}
		else
		{
			upnpService.stop();
			broadcastDiscoveryService.stop();
			startDhtIfNeeded(restart);
		}
	}

	public void stop()
	{
		startWhenPossible = false;

		if (running.compareAndSet(true, false))
		{
			dhtService.stop();
			upnpService.stop();
			broadcastDiscoveryService.stop();
			peerService.stop();

			upnpService.waitForTermination();
		}
	}

	public void compareSettingsAndApplyActions(Settings oldSettings, Settings newSettings)
	{
		applyBroadcastDiscovery(oldSettings, newSettings);
		applyDht(oldSettings, newSettings);
		applyUpnp(oldSettings, newSettings);
		applyTor(oldSettings, newSettings);
		applyI2p(oldSettings, newSettings);
	}

	@Scheduled(initialDelay = 2, fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
	void checkIp()
	{
		if (!locationService.hasOwnLocation())
		{
			return;
		}

		var newLocalIpAddress = IP.getLocalIpAddress();
		if (newLocalIpAddress == null)
		{
			log.error("No TCP/IP stack available...");
			return;
		}

		if (!newLocalIpAddress.equals(localIpAddress))
		{
			log.warn("Local IP address changed: {} -> {}", localIpAddress, newLocalIpAddress);
			publisher.publishEvent(new IpChangedEvent(newLocalIpAddress));
		}
	}

	@EventListener
	public void onIpChangedEvent(IpChangedEvent event)
	{
		log.warn("IP change event received, possibly restarting some services...");

		if (!running.get() && startWhenPossible)
		{
			start();
			return;
		}

		if (!IP.isRoutableIp(localIpAddress))
		{
			stop();
			startWhenPossible = true;
			return;
		}

		localIpAddress = event.localIpAddress();

		startHelperServices(IP.isLanIp(localIpAddress), true);
	}

	@EventListener
	public void onUpnpEvent(UpnpEvent event)
	{
		if (event.portsForwarded())
		{
			log.info("Ports forwarded on the router");
		}
		else
		{
			log.info("Ports not forwarded on the router");
		}
		if (!event.externalIpFound())
		{
			log.warn("External IP address not found");
		}

		// We start the DHT here because it's better when the incoming port is working first.
		// But it can still work without it.
		if (settingsService.isDhtEnabled())
		{
			dhtService.start(locationService.findOwnLocation().orElseThrow().getLocationId(), event.localPort());
		}
	}

	private void startDhtIfNeeded(boolean restart)
	{
		if (settingsService.isDhtEnabled())
		{
			if (restart)
			{
				dhtService.stop();
			}
			dhtService.start(locationService.findOwnLocation().orElseThrow().getLocationId(), settingsService.getLocalPort());
		}
	}

	private void startBroadcastDiscoveryIfNeeded(boolean restart)
	{
		if (settingsService.isBroadcastDiscoveryEnabled())
		{
			if (restart)
			{
				broadcastDiscoveryService.stop();
			}
			broadcastDiscoveryService.start(localIpAddress, settingsService.getLocalPort());
		}
	}

	private void applyBroadcastDiscovery(Settings oldSettings, Settings newSettings)
	{
		if (newSettings.isBroadcastDiscoveryEnabled() != oldSettings.isBroadcastDiscoveryEnabled())
		{
			if (newSettings.isBroadcastDiscoveryEnabled())
			{
				broadcastDiscoveryService.start(localIpAddress, newSettings.getLocalPort());
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
				upnpService.start(localIpAddress, newSettings.getLocalPort());
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
}
