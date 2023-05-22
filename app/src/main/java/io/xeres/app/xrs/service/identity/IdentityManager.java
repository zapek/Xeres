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

package io.xeres.app.xrs.service.identity;

import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.identity.Type;
import io.xeres.common.util.NoSuppressedRunnable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manages GxsId requests, caching and storage in an intelligent way, like:
 * <ul>
 *     <li>group requests to ask in batches</li>
 *     <li>remembers which peer is likely to answer requests (basic routing)</li>
 *     <li>caches recent GxsIds</li>
 * </ul>
 */
@Component
public class IdentityManager
{
	private final Map<Long, Set<GxsId>> pendingGxsIds = new HashMap<>();
	private final Set<GxsId> friendGxsIds = new HashSet<>();

	private static final Duration TIME_BETWEEN_REQUESTS = Duration.ofSeconds(5);

	private static final int MAXIMUM_IDS_PER_LOCATION = 5;

	private final IdentityRsService identityRsService;
	private final PeerConnectionManager peerConnectionManager;

	private final ScheduledExecutorService executorService;

	// XXX: try to fix the circular dependency injection
	public IdentityManager(@Lazy IdentityRsService identityRsService, PeerConnectionManager peerConnectionManager)
	{
		this.identityRsService = identityRsService;
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
		try
		{
			executorService.awaitTermination(2, TimeUnit.SECONDS);
		}
		catch (InterruptedException ignored)
		{
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Gets a gxs group, if available. Otherwise, put a request to fetch it later
	 *
	 * @param peerConnection the peer to try to get the gxs group from
	 * @param gxsId          the gxs group id
	 * @return the gxs group, or null if not found yet
	 */
	public IdentityGroupItem getGxsGroup(PeerConnection peerConnection, GxsId gxsId)
	{
		synchronized (pendingGxsIds)
		{
			return identityRsService.findByGxsId(gxsId).orElseGet(() -> {
				var gxsIds = pendingGxsIds.getOrDefault(peerConnection.getLocation().getId(), ConcurrentHashMap.newKeySet());
				gxsIds.add(gxsId);
				pendingGxsIds.put(peerConnection.getLocation().getId(), gxsIds);
				return null;
			});
		}
	}

	public void fetchGxsGroups(PeerConnection peerConnection, Set<GxsId> gxsIds)
	{
		synchronized (pendingGxsIds)
		{
			var existing = identityRsService.findAll(gxsIds).stream()
					.map(GxsGroupItem::getGxsId)
					.collect(Collectors.toSet());
			var remaining = gxsIds.stream()
					.filter(gxsId -> !existing.contains(gxsId))
					.collect(Collectors.toSet());
			if (!remaining.isEmpty())
			{
				var pendingMap = pendingGxsIds.getOrDefault(peerConnection.getLocation().getId(), ConcurrentHashMap.newKeySet());
				pendingMap.addAll(gxsIds);
				pendingGxsIds.put(peerConnection.getLocation().getId(), pendingMap);
			}
		}
	}

	public void setAsFriend(Set<GxsId> gxsIds)
	{
		synchronized (pendingGxsIds)
		{
			var remaining = setExistingAsFriend(gxsIds);
			friendGxsIds.addAll(remaining);
		}
	}

	void requestGxsIds()
	{
		synchronized (pendingGxsIds)
		{
			pendingGxsIds.forEach((locationId, gxsIds) -> {
				var gxsIdsToGet = gxsIds.stream().limit(MAXIMUM_IDS_PER_LOCATION).toList();
				var peerConnection = peerConnectionManager.getPeerByLocationId(locationId);
				if (peerConnection != null)
				{
					identityRsService.requestGxsGroups(peerConnection, gxsIdsToGet);
					gxsIdsToGet.forEach(gxsIds::remove); // XXX: if the peer is not there anymore, we should try to get it from other peers...
				}
			});

			// Remove all entries with empty sets
			pendingGxsIds.entrySet().removeIf(entry -> entry.getValue().isEmpty());

			// Set peer identities as friends
			friendGxsIds.clear();
			friendGxsIds.addAll(setExistingAsFriend(friendGxsIds));
		}
	}

	private Set<GxsId> setExistingAsFriend(Set<GxsId> gxsIds)
	{
		var existing = identityRsService.findAll(gxsIds);
		var convertible = existing.stream()
				.filter(identityGroupItem -> identityGroupItem.getType() == Type.OTHER)
				.collect(Collectors.toSet());

		convertible.forEach(identityGroupItem -> {
			identityGroupItem.setType(Type.FRIEND);
			identityRsService.saveIdentity(identityGroupItem);
		});

		var existingGxsIds = existing.stream()
				.map(GxsGroupItem::getGxsId)
				.collect(Collectors.toSet());
		return gxsIds.stream()
				.filter(gxsId -> !existingGxsIds.contains(gxsId))
				.collect(Collectors.toSet());
	}
}
