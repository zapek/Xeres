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

import io.xeres.app.configuration.DataDirConfiguration;
import io.xeres.common.protocol.ip.IP;
import lbms.plugins.mldht.DHTConfiguration;
import lbms.plugins.mldht.kad.*;
import lbms.plugins.mldht.kad.messages.MessageBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.function.Predicate;

import static lbms.plugins.mldht.kad.DHT.DHTtype.IPV4_DHT;
import static lbms.plugins.mldht.kad.DHT.LogLevel.Fatal;

@Service
public class DHTService implements DHTStatusListener, DHTConfiguration, DHTStatsListener, DHT.IncomingMessageListener
{
	private static final Logger log = LoggerFactory.getLogger(DHTService.class);

	private static final String DHT_DATA_DIR = "dht";
	private static final Duration STATS_DELAY = Duration.ofMinutes(1);

	private DHT dht;
	private int localPort;

	private Instant lastStats;

	private final DataDirConfiguration dataDirConfiguration;

	public DHTService(DataDirConfiguration dataDirConfiguration)
	{
		this.dataDirConfiguration = dataDirConfiguration;
	}

	public void start(int localPort)
	{
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
			addBootstrappingNodes();
		}
		catch (IOException e)
		{
			log.error("Error while setting up DHT: {}", e.getMessage(), e);
		}

		dht.getServerManager().awaitActiveServer(); // XXX: catch the completable future to get a RPCServer to work with
		// see https://github.com/the8472/mldht/blob/master/docs/use-as-library.md
	}

	public void stop()
	{
		if (dht != null && dht.isRunning())
		{
			dht.stop();
		}
	}

	@Override
	public void statusChanged(DHTStatus newStatus, DHTStatus oldStatus)
	{
		switch (newStatus)
		{
			case Running -> log.info("DHT status -> running");

			// XXX: wait for that event before making us as usable
			case Stopped -> log.info("DHT status -> stopped");

			// XXX: allow to wait on that while shutting down
			case Initializing -> log.info("DHT status -> initializing");
		}
	}

	@Override
	public boolean isPersistingID()
	{
		return false;
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
		return true;
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
			log.debug("Num peers: {}, recv pkt: {} ({} KB), sent pkt: {} ({} KB), keys: {}, items: {}",
					dhtStats.getNumPeers(),
					dhtStats.getNumReceivedPackets(),
					dhtStats.getRpcStats().getReceivedBytes() / 1024,
					dhtStats.getNumSentPackets(),
					dhtStats.getRpcStats().getSentBytes() / 1024,
					dhtStats.getDbStats().getKeyCount(),
					dhtStats.getDbStats().getItemCount());

			lastStats = now;
		}
	}

	@Override
	public void received(DHT dht, MessageBase messageBase)
	{
		// XXX: handle messages here. called from message processing thread so must be non blocking and thread safe
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
				String[] tokens = line.split(" ");
				String ip = tokens[0];
				var port = Integer.parseInt(tokens[1]);

				if (!IP.isRoutableIp(ip))
				{
					throw new IllegalArgumentException("IP is invalid");
				}
				if (!IP.isValidPort(port))
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
