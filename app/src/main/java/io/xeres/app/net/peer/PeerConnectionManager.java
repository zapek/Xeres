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

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.FailedFuture;
import io.netty.util.concurrent.Future;
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

@Component
public class PeerConnectionManager
{
	private static final Logger log = LoggerFactory.getLogger(PeerConnectionManager.class);

	private final StatusNotificationService statusNotificationService;
	private final AvailabilityNotificationService availabilityNotificationService;
	private final ApplicationEventPublisher publisher;

	private final Map<Long, PeerConnection> peers = new ConcurrentHashMap<>();

	public PeerConnectionManager(StatusNotificationService statusNotificationService, AvailabilityNotificationService availabilityNotificationService, ApplicationEventPublisher publisher)
	{
		this.statusNotificationService = statusNotificationService;
		this.availabilityNotificationService = availabilityNotificationService;
		this.publisher = publisher;
	}

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

	public void updatePeer(Location location)
	{
		if (!peers.containsKey(location.getId()))
		{
			throw new IllegalStateException("Location " + location + " is not in the list of peers");
		}
		peers.get(location.getId()).updateLocation(location);
	}

	public PeerConnection getPeerByLocation(long id)
	{
		return peers.get(id);
	}

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
		peers.forEach((id, peerConnection) -> peerConnection.shutdown());
		availabilityNotificationService.shutdown();
	}

	public Future<Void> writeItem(Location location, Item item, RsService rsService)
	{
		var peer = peers.get(location.getId());
		if (peer != null)
		{
			return setOutgoingAndWriteItem(peer, item, rsService);
		}
		return new FailedFuture<>(null, new IllegalStateException("Peer with connection " + location + " not found while trying to write item. User disconnected?"));
	}

	public Future<Void> writeItem(PeerConnection peerConnection, Item item, RsService rsService)
	{
		var peer = peers.get(peerConnection.getLocation().getId());
		if (peer != null)
		{
			return setOutgoingAndWriteItem(peer, item, rsService);
		}
		return new FailedFuture<>(null, new IllegalStateException("Peer with connection " + peerConnection.getLocation() + " not found while trying to write item. User disconnected?"));
	}

	private static ChannelFuture setOutgoingAndWriteItem(PeerConnection peerConnection, Item item, RsService rsService)
	{
		item.setOutgoing(peerConnection.getCtx().alloc(), rsService);
		return writeItem(peerConnection, item);
	}

	/**
	 * Execute an action for all peers.
	 *
	 * @param action    the action to execute
	 * @param rsService the service that has to be enabled for the peer as well. Can be null, in that case, all peers are considered for the action regardless of the service they're running
	 */
	public void doForAllPeers(Consumer<PeerConnection> action, RsService rsService)
	{
		peers.forEach((peerId, peerConnection) ->
		{
			if (rsService == null || peerConnection.isServiceSupported(rsService))
			{
				action.accept(peerConnection);
			}
		});
	}

	public void doForAllPeersExceptSender(Consumer<PeerConnection> action, PeerConnection sender, RsService rsService)
	{
		peers.values().stream()
				.filter(peerConnection -> !peerConnection.equals(sender))
				.filter(peerConnection -> peerConnection.isServiceSupported(rsService))
				.forEach(action);
	}

	private static ChannelFuture writeItem(PeerConnection peerConnection, Item item)
	{
		var rawItem = item.serializeItem(EnumSet.noneOf(SerializationFlags.class));
		log.debug("==> {}", item);
		log.trace("Message content: {}", rawItem);
		peerConnection.incrementSentCounter(rawItem.getSize());
		return peerConnection.getCtx().writeAndFlush(rawItem);
	}

	public static void writeSliceProbe(ChannelHandlerContext ctx)
	{
		var item = SliceProbeItem.from(ctx);
		var rawItem = item.serializeItem(EnumSet.noneOf(SerializationFlags.class));
		ctx.writeAndFlush(rawItem);
	}

	public int getNumberOfPeers()
	{
		return peers.size();
	}

	private void updateCurrentUsersCount()
	{
		statusNotificationService.setCurrentUsersCount(peers.size());
	}
}
