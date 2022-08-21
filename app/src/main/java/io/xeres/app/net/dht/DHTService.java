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

package io.xeres.app.net.dht;

import io.xeres.app.application.events.DhtReadyEvent;
import io.xeres.app.configuration.DataDirConfiguration;
import io.xeres.app.service.StatusNotificationService;
import io.xeres.common.id.LocationId;
import io.xeres.common.protocol.ip.IP;
import io.xeres.common.rest.notification.DhtInfo;
import io.xeres.common.rest.notification.DhtStatus;
import io.xeres.common.util.NoSuppressedRunnable;
import lbms.plugins.mldht.DHTConfiguration;
import lbms.plugins.mldht.kad.*;
import lbms.plugins.mldht.kad.tasks.PeerLookupTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import static lbms.plugins.mldht.kad.DHT.DHTtype.IPV4_DHT;
import static lbms.plugins.mldht.kad.DHT.LogLevel.Fatal;

@Service
public class DHTService implements DHTStatusListener, DHTConfiguration, DHTStatsListener
{
	private static final Logger log = LoggerFactory.getLogger(DHTService.class);

	private static final String DHT_DATA_DIR = "dht";
	private static final Duration STATS_DELAY = Duration.ofMinutes(1);

	private DHT dht;
	private int localPort;

	private Instant lastStats;

	private final DataDirConfiguration dataDirConfiguration;
	private final ApplicationEventPublisher publisher;

	private final StatusNotificationService statusNotificationService;

	public DHTService(DataDirConfiguration dataDirConfiguration, ApplicationEventPublisher publisher, StatusNotificationService statusNotificationService)
	{
		this.dataDirConfiguration = dataDirConfiguration;
		this.publisher = publisher;
		this.statusNotificationService = statusNotificationService;
	}

	public void start(int localPort)
	{
		if (dht != null && dht.isRunning())
		{
			return;
		}

		this.localPort = localPort;

		DHT.setLogger(new DHTSpringLog());
		DHT.setLogLevel(Fatal);
		dht = new DHT(IPV4_DHT);
		dht.addStatusListener(this);
		dht.addStatsListener(this);
		lastStats = Instant.now();

		try
		{
			dht.start(this);
			dht.getNode().getNumEntriesInRoutingTable();
			dht.bootstrap();
			if (dht.getNode().getNumEntriesInRoutingTable() < 10)
			{
				addBootstrappingNodes(); // help the bootstrapping process, in case nothing resolves
			}
		}
		catch (IOException e)
		{
			log.error("Error while setting up DHT: {}", e.getMessage(), e);
		}

		try
		{
			dht.getServerManager().awaitActiveServer().get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			log.error("Error while setting up DHT: {}", e.getMessage());
		}
	}

	public void stop()
	{
		if (dht != null && dht.isRunning())
		{
			dht.stop();
		}
	}

	public void search(LocationId locationId)
	{
		var peerLookupTask = dht.createPeerLookup(InfoHash.makeInfoHash(locationId));
		if (peerLookupTask != null)
		{
			peerLookupTask.setNoAnnounce(true);
			peerLookupTask.setInfo(locationId.toString());
			peerLookupTask.setNoSeeds(false); // XXX: not sure...
			peerLookupTask.addListener(task -> log.debug("Task finished: {}, items: {}", task.getInfo(), ((PeerLookupTask) task).getReturnedItems().size()));
			dht.getTaskManager().addTask(peerLookupTask);
			log.debug("Added PeerLookupTask {} for locationId {}", peerLookupTask, peerLookupTask.getInfo());
		}
	}

	public void announce(LocationId locationId)
	{
		if (dht == null || !dht.isRunning())
		{
			return;
		}

		var peerLookupTask = dht.createPeerLookup(InfoHash.makeInfoHash(locationId));
		if (peerLookupTask != null)
		{
			peerLookupTask.setInfo(locationId.toString());
			peerLookupTask.addListener(task -> dht.announce((PeerLookupTask) task, true, localPort));
			dht.getTaskManager().addTask(peerLookupTask);
			log.debug("Added PeerLookupTask + announce {} for locationId {} -> infohash: {}", peerLookupTask, peerLookupTask.getInfo(), peerLookupTask.getInfoHash().toString(false));
		}
	}

	@Override
	public void statusChanged(DHTStatus newStatus, DHTStatus oldStatus)
	{
		switch (newStatus)
		{
			case Running ->
			{
				log.info("DHT status -> running");
				statusNotificationService.setDhtInfo(DhtInfo.fromStatus(DhtStatus.RUNNING));
				CompletableFuture.runAsync((NoSuppressedRunnable) () -> publisher.publishEvent(new DhtReadyEvent()));
			}

			case Stopped -> log.info("DHT status -> stopped");

			case Initializing -> log.info("DHT status -> initializing");
		}
	}

	@Override
	public boolean isPersistingID()
	{
		return true;
	}

	@Override
	public Path getStoragePath()
	{
		var path = Path.of(dataDirConfiguration.getDataDir(), DHT_DATA_DIR);
		if (Files.notExists(path))
		{
			try
			{
				Files.createDirectory(path);
			}
			catch (IOException e)
			{
				log.error("Failed to create DHT storage directory: {}, storage disabled", e.getMessage());
				return null;
			}
		}
		return path;
	}

	@Override
	public int getListeningPort()
	{
		return localPort;
	}

	@Override
	public boolean noRouterBootstrap()
	{
		return false;
	}

	@Override
	public boolean allowMultiHoming()
	{
		return false;
	}

	@Override
	public Predicate<InetAddress> filterBindAddress()
	{
		return address -> IP.isRoutableIp(address.getHostAddress());
	}

	@Override
	public void statsUpdated(DHTStats dhtStats)
	{
		var now = Instant.now();

		if (Duration.between(lastStats, now).compareTo(STATS_DELAY) > 0)
		{
			log.debug("Peers: {}, recv pkt: {} ({} KB), sent pkt: {} ({} KB), keys: {}, items: {}",
					dhtStats.getNumPeers(),
					dhtStats.getNumReceivedPackets(),
					dhtStats.getRpcStats().getReceivedBytes() / 1024,
					dhtStats.getNumSentPackets(),
					dhtStats.getRpcStats().getSentBytes() / 1024,
					dhtStats.getDbStats().getKeyCount(),
					dhtStats.getDbStats().getItemCount());

			if (dht.getStatus() == DHTStatus.Running)
			{
				statusNotificationService.setDhtInfo(DhtInfo.fromStats(
						dhtStats.getNumPeers(),
						dhtStats.getNumReceivedPackets(),
						dhtStats.getRpcStats().getReceivedBytes(),
						dhtStats.getNumSentPackets(),
						dhtStats.getRpcStats().getSentBytes(),
						dhtStats.getDbStats().getKeyCount(),
						dhtStats.getDbStats().getItemCount()));
			}
			lastStats = now;
		}
	}

	private void addBootstrappingNodes()
	{
		var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(this.getClass().getResourceAsStream("/bdboot.txt"))));
		var line = "";

		try
		{
			while (reader.ready())
			{
				line = reader.readLine();
				var tokens = line.split(" ");
				var ip = tokens[0];
				var port = Integer.parseInt(tokens[1]);

				if (!IP.isRoutableIp(ip))
				{
					throw new IllegalArgumentException("IP is invalid");
				}
				if (IP.isInvalidPort(port))
				{
					throw new IllegalArgumentException("Port is invalid");
				}
				log.debug("adding node {}:{}", ip, port);
				dht.addDHTNode(ip, port);
			}
		}
		catch (IOException | IllegalArgumentException e)
		{
			log.warn("Couldn't parse ip<space>port of line: {} ({})", line, e.getMessage());
		}
	}
}
