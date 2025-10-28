/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.net.peer;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.FailedFuture;
import io.xeres.app.application.events.PeerConnectedEvent;
import io.xeres.app.application.events.PeerDisconnectedEvent;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.service.notification.availability.AvailabilityNotificationService;
import io.xeres.app.service.notification.status.StatusNotificationService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.sliceprobe.item.SliceProbeItem;
import io.xeres.common.location.Availability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static io.xeres.app.net.peer.PeerAttribute.PEER_CONNECTION;

/**
 * This component manages connected peers (addition, removals, writing data to them, etc...)
 */
@Component
public class PeerConnectionManager
{
	private static final Logger log = LoggerFactory.getLogger(PeerConnectionManager.class);

	private final StatusNotificationService statusNotificationService;
	private final AvailabilityNotificationService availabilityNotificationService;
	private final ApplicationEventPublisher publisher;

	private final Map<Long, PeerConnection> peers = new ConcurrentHashMap<>();

	PeerConnectionManager(StatusNotificationService statusNotificationService, AvailabilityNotificationService availabilityNotificationService, ApplicationEventPublisher publisher)
	{
		this.statusNotificationService = statusNotificationService;
		this.availabilityNotificationService = availabilityNotificationService;
		this.publisher = publisher;
	}

	/**
	 * Adds a connected peer.
	 *
	 * @param location the location of the peer
	 * @param ctx      the context
	 * @return a peer connection
	 */
	public PeerConnection addPeer(Location location, ChannelHandlerContext ctx)
	{
		if (peers.containsKey(location.getId()))
		{
			throw new IllegalStateException("Location " + location + " added already");
		}
		var peerConnection = new PeerConnection(location, ctx);
		peers.put(location.getId(), peerConnection);
		ctx.channel().attr(PEER_CONNECTION).set(peerConnection);
		availabilityNotificationService.changeAvailability(location, Availability.AVAILABLE);
		updateCurrentUsersCount();
		publisher.publishEvent(new PeerConnectedEvent(location.getLocationIdentifier()));
		return peerConnection;
	}

	/**
	 * Removes a peer because it disconnected.
	 *
	 * @param location the location of the peer
	 */
	public void removePeer(Location location)
	{
		if (!peers.containsKey(location.getId()))
		{
			throw new IllegalStateException("Location " + location + " is not in the list of peers");
		}
		peers.remove(location.getId());
		availabilityNotificationService.changeAvailability(location, Availability.OFFLINE);
		updateCurrentUsersCount();
		publisher.publishEvent(new PeerDisconnectedEvent(location.getLocationIdentifier()));
	}

	/**
	 * Gets a peer by its location id
	 *
	 * @param id the id of the location
	 * @return the peer connection
	 */
	public PeerConnection getPeerByLocation(long id)
	{
		return peers.get(id);
	}

	/**
	 * Gets a random peer.
	 *
	 * @return a random peer
	 */
	public PeerConnection getRandomPeer()
	{
		if (peers.isEmpty())
		{
			return null;
		}
		return peers.values().stream()
				.skip(ThreadLocalRandom.current().nextInt(peers.size()))
				.findFirst().orElse(null);
	}

	public void shutdown()
	{
		peers.forEach((_, peerConnection) -> peerConnection.shutdown());
		availabilityNotificationService.shutdown();
	}

	/**
	 * Writes an item to a location.
	 *
	 * @param location  the target location
	 * @param item      the item to write
	 * @param rsService the service concerned
	 * @return an ItemFuture containing the item's write state and item serialized state
	 */
	public ItemFuture writeItem(Location location, Item item, RsService rsService)
	{
		var peer = peers.get(location.getId());
		if (peer != null)
		{
			return setOutgoingAndWriteItem(peer, item, rsService);
		}
		return new DefaultItemFuture(new FailedFuture<>(null, new IllegalStateException("Peer with connection " + location + " not found while trying to write item. User disconnected?")));
	}

	/**
	 * Writes an item to a peer.
	 *
	 * @param peerConnection the target peer
	 * @param item           the item to write
	 * @param rsService      the service concerned
	 * @return an ItemFuture containing the item's write state and item serialized state
	 */
	public ItemFuture writeItem(PeerConnection peerConnection, Item item, RsService rsService)
	{
		var peer = peers.get(peerConnection.getLocation().getId());
		if (peer != null)
		{
			return setOutgoingAndWriteItem(peer, item, rsService);
		}
		return new DefaultItemFuture(new FailedFuture<>(null, new IllegalStateException("Peer with connection " + peerConnection.getLocation() + " not found while trying to write item. User disconnected?")));
	}

	/**
	 * Executes an action for all peers.
	 *
	 * @param action    the action to execute
	 * @param rsService the service that has to be enabled for the peer as well. Can be null, in that case, all peers are considered for the action regardless of the service they're running
	 */
	public void doForAllPeers(Consumer<PeerConnection> action, RsService rsService)
	{
		peers.forEach((_, peerConnection) ->
		{
			if (rsService == null || peerConnection.isServiceSupported(rsService))
			{
				action.accept(peerConnection);
			}
		});
	}

	/**
	 * Executes an action for all peers except the originator.
	 *
	 * @param action    the action to execute
	 * @param sender    the originator of the action
	 * @param rsService the service that has to be enabled for the peer as well. Can be null, in that case, all peers are considered for the action regardless of the service they're running
	 */
	public void doForAllPeersExceptSender(Consumer<PeerConnection> action, PeerConnection sender, RsService rsService)
	{
		peers.values().stream()
				.filter(peerConnection -> !peerConnection.equals(sender))
				.filter(peerConnection -> rsService == null || peerConnection.isServiceSupported(rsService))
				.forEach(action);
	}

	public boolean isServiceSupported(Location location, int serviceId)
	{
		var peer = peers.get(location.getId());
		if (peer != null)
		{
			return peer.isServiceSupported(serviceId);
		}
		return false;
	}

	/**
	 * Writes the slice probe item. This is only needed for very particular cases.
	 *
	 * @param ctx the context
	 */
	public static void writeSliceProbe(ChannelHandlerContext ctx)
	{
		var item = SliceProbeItem.from(ctx);
		var rawItem = item.serializeItem(EnumSet.noneOf(SerializationFlags.class));
		ctx.writeAndFlush(rawItem);
	}

	/**
	 * Returns the number of connected peers.
	 *
	 * @return the number of connected peers
	 */
	public int getNumberOfPeers()
	{
		return peers.size();
	}

	private static ItemFuture setOutgoingAndWriteItem(PeerConnection peerConnection, Item item, RsService rsService)
	{
		item.setOutgoing(peerConnection.getCtx().alloc(), rsService);
		return writeItem(peerConnection, item);
	}

	private static ItemFuture writeItem(PeerConnection peerConnection, Item item)
	{
		var rawItem = item.serializeItem(EnumSet.noneOf(SerializationFlags.class));
		var size = rawItem.getSize(); // get it before it's written
		log.debug("==> {}", item);
		log.trace("Message content: {}", rawItem);
		peerConnection.incrementSentCounter(size);
		return new DefaultItemFuture(peerConnection.getCtx().writeAndFlush(rawItem), size);
	}

	private void updateCurrentUsersCount()
	{
		statusNotificationService.setCurrentUsersCount(peers.size());
	}
}
