/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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
import io.xeres.app.service.IdentityService;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.identity.Type;
import io.xeres.common.util.ExecutorUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
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

	/**
	 * Identities to set as friends. The ones that are in this list have been
	 * sent by discovery, but since we don't have them yet, we'll process them later.
	 */
	private final Set<GxsId> pendingFriendsGxsIds = new HashSet<>();

	/**
	 * Identities that have failed validation. We keep them on a list for some time
	 * to avoid asking for them again and again.
	 */
	private final Set<GxsId> rejectedGxsIds = new HashSet<>();

	private static final Duration TIME_BETWEEN_REQUESTS = Duration.ofSeconds(5);

	private static final int MAXIMUM_IDS_PER_LOCATION = 5;

	private static final Duration REJECTED_IDENTITIES_DELAY = Duration.ofHours(6);
	private Instant lastRejectionDelay = Instant.now();

	private final IdentityRsService identityRsService;
	private final IdentityService identityService;
	private final PeerConnectionManager peerConnectionManager;

	private final ScheduledExecutorService executorService;

	// XXX: try to fix the circular dependency injection
	public IdentityManager(@Lazy IdentityRsService identityRsService, IdentityService identityService, PeerConnectionManager peerConnectionManager)
	{
		this.identityRsService = identityRsService;
		this.identityService = identityService;
		this.peerConnectionManager = peerConnectionManager;

		executorService = ExecutorUtils.createFixedRateExecutor(this::requestIdentities,
				TIME_BETWEEN_REQUESTS.toSeconds());
	}

	public void shutdown()
	{
		ExecutorUtils.cleanupExecutor(executorService);
	}

	/**
	 * Gets an identity, if available. Otherwise, puts a request to fetch it later
	 *
	 * @param peerConnection the peer to try to get the identity from
	 * @param gxsId          the gxs id
	 * @return the identity, or null if not found yet
	 */
	public IdentityGroupItem getIdentity(PeerConnection peerConnection, GxsId gxsId)
	{
		synchronized (pendingGxsIds)
		{
			return identityService.findByGxsId(gxsId).orElseGet(() -> {
				var gxsIds = pendingGxsIds.getOrDefault(peerConnection.getLocation().getId(), ConcurrentHashMap.newKeySet());
				gxsIds.add(gxsId);
				pendingGxsIds.put(peerConnection.getLocation().getId(), gxsIds);
				return null;
			});
		}
	}

	/**
	 * Gets an identity.
	 *
	 * @param gxsId the gxs id
	 * @return the identity, null if not found
	 */
	public IdentityGroupItem getIdentity(GxsId gxsId)
	{
		return identityService.findByGxsId(gxsId).orElse(null);
	}

	/**
	 * Fetches a group of identities. Use this if you want them to appear as contacts.
	 * <p>Known usage: identities sent by discovery.
	 *
	 * @param peerConnection the peer
	 * @param gxsIds         the list of identities
	 */
	public void fetchIdentities(PeerConnection peerConnection, Set<GxsId> gxsIds)
	{
		synchronized (pendingGxsIds)
		{
			var existing = identityService.findAll(gxsIds).stream()
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

	/**
	 * Manages the discovered identities from a peer (sets them as friends, etc...)
	 *
	 * @param gxsIds the list of identities
	 */
	public void manageDiscoveredIdentities(PeerConnection peerConnection, Set<GxsId> gxsIds)
	{
		synchronized (pendingGxsIds)
		{
			var existing = identityService.findAll(gxsIds);
			var profile = peerConnection.getLocation().getProfile();

			// Check if there's an identity that we had previously but isn't linked yet to the profile of the
			// peer. If so, it needs to be validated again with the profile; that way we can have a proper contact.
			existing.stream()
					.filter(identity -> !identity.hasProfile() && identity.getName().equals(profile.getName())) // XXX: ideally we should check the pgp id of each identity but it is expensive. this could also cause a useless verification in case of name clashes
					.forEach(identity -> {
						identity.setNextValidation(Instant.now());
						identityRsService.saveIdentity(identity);
					});

			var remaining = setExistingAsFriend(existing);
			pendingFriendsGxsIds.addAll(remaining);
		}
	}

	/**
	 * Updates the identity usage.
	 *
	 * @param gxsIds the identities
	 * @param when   when they're last used (usually Instant.now())
	 */
	public void updateIdentityUsage(Set<GxsId> gxsIds, Instant when)
	{
		identityService.updateIdentityUsage(gxsIds, when);
	}

	/**
	 * Adds a rejected identity to make sure it's not immediately requested again.
	 *
	 * @param gxsId the gxsId to ignore for some time
	 */
	public void addRejectedIdentity(GxsId gxsId)
	{
		synchronized (pendingGxsIds)
		{
			rejectedGxsIds.add(gxsId);
		}
	}

	void requestIdentities()
	{
		synchronized (pendingGxsIds)
		{
			pendingGxsIds.forEach((locationId, gxsIds) -> {
				gxsIds.removeIf(rejectedGxsIds::contains);
				if (!gxsIds.isEmpty())
				{
					var gxsIdsToGet = gxsIds.stream()
							.limit(MAXIMUM_IDS_PER_LOCATION)
							.toList();
					var peerConnection = peerConnectionManager.getPeerByLocation(locationId);
					if (peerConnection != null)
					{
						identityRsService.requestGxsGroups(peerConnection, gxsIdsToGet);
						gxsIdsToGet.forEach(gxsIds::remove); // XXX: if the peer is not there anymore, we should try to get it from other peers...
					}
				}
			});

			// Remove all entries with empty sets
			pendingGxsIds.entrySet().removeIf(entry -> entry.getValue().isEmpty());

			// If there are some pending friend identities, check if we
			// can set them as friends now.
			pendingFriendsGxsIds.retainAll(setExistingAsFriend(pendingFriendsGxsIds));

			var now = Instant.now();
			if (Duration.between(lastRejectionDelay, now).compareTo(REJECTED_IDENTITIES_DELAY) > 0)
			{
				rejectedGxsIds.clear(); // We don't bother with exact time tracking; the goal is to save computation and bandwidth
				lastRejectionDelay = now;
			}
		}
	}

	/**
	 * Sets existing identities as friends.
	 *
	 * @param gxsIds the identities to set as friends
	 * @return the identities that we don't have
	 */
	private Set<GxsId> setExistingAsFriend(Set<GxsId> gxsIds)
	{
		var existing = identityService.findAll(gxsIds);
		return setExistingAsFriend(existing);
	}

	private Set<GxsId> setExistingAsFriend(List<IdentityGroupItem> existing)
	{
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
		return existing.stream()
				.map(GxsGroupItem::getGxsId)
				.filter(gxsId -> !existingGxsIds.contains(gxsId))
				.collect(Collectors.toSet());
	}
}
