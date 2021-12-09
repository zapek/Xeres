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

package io.xeres.app.xrs.service.gxsid;

import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.IdentityService;
import io.xeres.app.xrs.service.gxsid.item.GxsIdGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.util.NoSuppressedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages GxsId requests, caching and storage in an intelligent way, like:
 * <ul>
 *     <li>group requests to ask in batches</li>
 *     <li>remembers which peer is likely to answer requests (basic routing)</li>
 *     <li>caches recent GxsIds</li>
 * </ul>
 */
@Component
public class GxsIdManager
{
	private static final Logger log = LoggerFactory.getLogger(GxsIdManager.class);

	private final Map<Long, Set<GxsId>> pendingGxsIds = new ConcurrentHashMap<>();

	private static final Duration TIME_BETWEEN_REQUESTS = Duration.ofSeconds(5);

	private static final int MAXIMUM_IDS_PER_LOCATION = 5;

	private final GxsIdService gxsIdService;
	private final IdentityService identityService;
	private final PeerConnectionManager peerConnectionManager;

	private final ScheduledExecutorService executorService;

	public GxsIdManager(GxsIdService gxsIdService, IdentityService identityService, PeerConnectionManager peerConnectionManager)
	{
		this.gxsIdService = gxsIdService;
		this.identityService = identityService;
		this.peerConnectionManager = peerConnectionManager;

		executorService = Executors.newSingleThreadScheduledExecutor();

		executorService.scheduleAtFixedRate((NoSuppressedRunnable) this::requestGxsIds,
				TIME_BETWEEN_REQUESTS.toSeconds(),
				TIME_BETWEEN_REQUESTS.toSeconds(),
				TimeUnit.SECONDS);
	}

	public void shutdown()
	{
		executorService.shutdownNow();
	}

	// XXX: not sure the synchronization will work... maybe needs synchronized...
	public GxsIdGroupItem getGxsGroup(PeerConnection peerConnection, GxsId gxsId)
	{
		var gxsIdentity = identityService.getGxsIdentity(gxsId);
		if (gxsIdentity.isPresent())
		{
			return gxsIdentity.get();
		}
		else
		{
			var gxsIds = pendingGxsIds.get(peerConnection.getLocation().getId());
			if (gxsIds != null)
			{
				gxsIds.add(gxsId);
			}
			else
			{
				Set<GxsId> set = ConcurrentHashMap.newKeySet();
				set.add(gxsId);
				pendingGxsIds.put(peerConnection.getLocation().getId(), set);
			}

			var set = pendingGxsIds.getOrDefault(peerConnection.getLocation().getId(), ConcurrentHashMap.newKeySet());
			set.add(gxsId);
			return null;
		}
	}

	void requestGxsIds()
	{
		pendingGxsIds.forEach((locationId, gxsIds) -> {
			var gxsIdsToGet = gxsIds.stream().limit(MAXIMUM_IDS_PER_LOCATION).toList();
			var peerConnection = peerConnectionManager.getPeerByLocationId(locationId);
			if (peerConnection != null)
			{
				gxsIdService.requestGxsGroups(peerConnection, gxsIdsToGet);
				gxsIdsToGet.forEach(gxsIds::remove); // XXX: if the peer is  not there anymore, we should try to get it from other peers...
				if (gxsIds.size() == 0)
				{
					pendingGxsIds.remove(locationId);
				}
			}
		});
	}
}
