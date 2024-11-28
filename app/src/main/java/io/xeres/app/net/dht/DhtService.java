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

package io.xeres.app.net.dht;

import io.xeres.app.application.events.DhtNodeFoundEvent;
import io.xeres.app.configuration.DataDirConfiguration;
import io.xeres.app.service.notification.status.StatusNotificationService;
import io.xeres.common.id.Id;
import io.xeres.common.id.LocationId;
import io.xeres.common.protocol.HostPort;
import io.xeres.common.protocol.ip.IP;
import io.xeres.common.rest.notification.status.DhtInfo;
import io.xeres.common.rest.notification.status.DhtStatus;
import io.xeres.common.util.ByteUnitUtils;
import lbms.plugins.mldht.DHTConfiguration;
import lbms.plugins.mldht.kad.*;
import lbms.plugins.mldht.kad.messages.MessageBase;
import lbms.plugins.mldht.kad.tasks.NodeLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static lbms.plugins.mldht.kad.DHT.DHTtype.IPV4_DHT;
import static lbms.plugins.mldht.kad.DHT.LogLevel.Fatal;

@Service
public class DhtService implements DHTStatusListener, DHTConfiguration, DHTStatsListener, DHT.IncomingMessageListener
{
	private static final Logger log = LoggerFactory.getLogger(DhtService.class);

	private static final String DHT_DATA_DIR = "dht";

	// That file name must not be changed as it's what mldht uses internally
	private static final String DHT_FILE_NAME = "baseID.config";
	private static final Duration STATS_DELAY = Duration.ofMinutes(1);

	private DHT dht;

	private LocationId locationId;
	private int localPort;

	private Instant lastStats;

	private final Map<Key, LocationId> searchedKeys = new ConcurrentHashMap<>();

	private final AtomicBoolean isReady = new AtomicBoolean();

	private final DataDirConfiguration dataDirConfiguration;
	private final ApplicationEventPublisher publisher;
	private final StatusNotificationService statusNotificationService;

	public DhtService(DataDirConfiguration dataDirConfiguration, ApplicationEventPublisher publisher, StatusNotificationService statusNotificationService)
	{
		this.dataDirConfiguration = dataDirConfiguration;
		this.publisher = publisher;
		this.statusNotificationService = statusNotificationService;
	}

	public void start(LocationId locationId, int localPort)
	{
		if (dht != null && dht.isRunning())
		{
			return;
		}

		this.locationId = locationId;
		this.localPort = localPort;

		DHT.setLogger(new DHTSpringLog());
		DHT.setLogLevel(Fatal);
		dht = new DHT(IPV4_DHT);
		dht.addStatusListener(this);
		dht.addStatsListener(this);
		dht.addIncomingMessageListener(this);
		lastStats = Instant.now();

		try
		{
			dht.start(this);
			dht.bootstrap();
			if (dht.getNode().getNumEntriesInRoutingTable() < 10)
			{
				addBootstrappingNodes(); // help the bootstrapping process, in case nothing resolves
			}
			dht.getServerManager().awaitActiveServer().get();
		}
		catch (IOException | ExecutionException | InterruptedException | IllegalStateException e)
		{
			log.error("Error while setting up DHT: {}", e.getMessage());
			dht.stop();
			if (e instanceof InterruptedException)
			{
				Thread.currentThread().interrupt();
			}
		}
	}

	public void stop()
	{
		if (dht != null && dht.isRunning())
		{
			try
			{
				dht.stop();
			}
			catch (RuntimeException e)
			{
				// Sometimes DHT fails to shut down cleanly, but
				// it shouldn't disrupt the rest of the shutdown
				// process.
				log.error("DHT error: {}", e.getMessage(), e);
			}
		}
	}

	public void search(LocationId locationId)
	{
		if (dht == null || !dht.isRunning())
		{
			log.warn("Search is not available yet, DHT is not ready");
			return;
		}

		var key = new Key(NodeId.create(locationId));
		log.debug("Searching LocationId {} -> node id: {}", locationId, key);
		searchedKeys.put(key, locationId);

		var rpcServer = dht.getServerManager().getRandomActiveServer(false);
		if (rpcServer == null)
		{
			log.debug("No RPC server, cannot perform DHT search");
			return;
		}
		var nodeLookupTask = new NodeLookup(key, rpcServer, dht.getNode(), false);
		nodeLookupTask.setInfo(locationId.toString());
		nodeLookupTask.addListener(task -> log.debug("Task finished: {}", task.getInfo()));
		dht.getTaskManager().addTask(nodeLookupTask);
	}

	@Override
	public void statusChanged(DHTStatus newStatus, DHTStatus oldStatus)
	{
		switch (newStatus)
		{
			case Running ->
			{
				log.info("DHT status -> running");
				isReady.set(true);
				statusNotificationService.setDhtInfo(DhtInfo.fromStatus(DhtStatus.RUNNING));
			}

			case Stopped ->
			{
				log.info("DHT status -> stopped");
				isReady.set(false);
				statusNotificationService.setDhtInfo(DhtInfo.fromStatus(DhtStatus.OFF));
			}

			case Initializing ->
			{
				log.info("DHT status -> initializing");
				isReady.set(false);
				statusNotificationService.setDhtInfo(DhtInfo.fromStatus(DhtStatus.INITIALIZING));
			}
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
		var directoryPath = Path.of(dataDirConfiguration.getDataDir(), DHT_DATA_DIR);
		var filePath = directoryPath.resolve(DHT_FILE_NAME);

		if (Files.notExists(directoryPath) || Files.notExists(filePath))
		{
			try
			{
				Files.createDirectory(directoryPath);

				var nodeId = Id.toString(NodeId.create(locationId)).toUpperCase(Locale.ROOT);
				log.debug("Storing own NodeID: {}", nodeId);

				Files.createFile(filePath);
				Files.write(filePath, Collections.singleton(nodeId), StandardCharsets.ISO_8859_1);
			}
			catch (IOException e)
			{
				throw new IllegalStateException("Failed to create DHT data storage: " + e.getMessage(), e);
			}
		}
		return directoryPath;
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
			traceDhtStats(dhtStats);

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

	@Override
	public void received(DHT dht, MessageBase messageBase)
	{
		if (messageBase.getType() == MessageBase.Type.RSP_MSG && messageBase.getMethod() == MessageBase.Method.FIND_NODE)
		{
			var foundLocationId = searchedKeys.get(messageBase.getID());
			if (foundLocationId != null)
			{
				log.debug("Found node for id {}, IP: {}", foundLocationId, messageBase.getOrigin());
				searchedKeys.remove(messageBase.getID());
				publisher.publishEvent(new DhtNodeFoundEvent(foundLocationId, new HostPort(messageBase.getOrigin().getAddress().getHostAddress(), messageBase.getOrigin().getPort())));
			}
		}
	}

	public boolean isReady()
	{
		return isReady.get();
	}

	private void addBootstrappingNodes()
	{
		var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/bdboot.txt"))));
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

	private static void traceDhtStats(DHTStats dhtStats)
	{
		if (log.isTraceEnabled())
		{
			log.debug("Peers: {}, recv pkt: {} ({}), sent pkt: {} ({}), keys: {}, items: {}",
					dhtStats.getNumPeers(),
					dhtStats.getNumReceivedPackets(),
					ByteUnitUtils.fromBytes(dhtStats.getRpcStats().getReceivedBytes()),
					dhtStats.getNumSentPackets(),
					ByteUnitUtils.fromBytes(dhtStats.getRpcStats().getSentBytes()),
					dhtStats.getDbStats().getKeyCount(),
					dhtStats.getDbStats().getItemCount());
		}
	}
}
