/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.app.job;

import io.xeres.app.application.events.DhtNodeFoundEvent;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.dht.DhtService;
import io.xeres.app.net.peer.bootstrap.PeerTcpClient;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.PeerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static java.util.function.Predicate.not;

@Component
public class DhtFinderJob
{
	private static final Logger log = LoggerFactory.getLogger(DhtFinderJob.class);

	private static final int SIMULTANEOUS_DHT_LOOKUPS = 4;

	private final LocationService locationService;
	private final PeerService peerService;
	private final DhtService dhtService;
	private final PeerTcpClient peerTcpClient;

	private Slice<Location> locations;
	private int pageIndex;

	public DhtFinderJob(LocationService locationService, PeerService peerService, DhtService dhtService, PeerTcpClient peerTcpClient)
	{
		this.locationService = locationService;
		this.peerService = peerService;
		this.dhtService = dhtService;
		this.peerTcpClient = peerTcpClient;
	}

	@Scheduled(initialDelay = 2, fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
	void checkDht()
	{
		if (JobUtils.canRun(peerService) && dhtService.isReady())
		{
			findInDht();
		}
	}

	private void findInDht()
	{
		locations = locationService.getUnconnectedLocationsWithDht(PageRequest.of(getPageIndex(), SIMULTANEOUS_DHT_LOOKUPS, Sort.by("lastConnected")));

		locations.stream()
				.filter(not(Location::isOwn))
				.forEach(location -> dhtService.search(location.getLocationId()));
	}

	@EventListener
	public void dhtNodeFoundEvent(DhtNodeFoundEvent event)
	{
		var peerAddress = PeerAddress.from(event.hostPort());
		log.debug("Trying to connect to LocationId: {} using {} from DHT lookup", event.locationId(), event.hostPort());

		// We don't update the connection table of the location here because there's no guarantee that the DHT node that answered is
		// the right one (could be fake), but discovery will be able to update it.
		peerTcpClient.connect(peerAddress);
	}

	private int getPageIndex()
	{
		if (locations == null || locations.isLast())
		{
			pageIndex = 0;
		}
		else
		{
			pageIndex++;
		}
		return pageIndex;
	}
}
