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

package io.xeres.app.net.peer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.service.RsService;
import io.xeres.common.id.Identifier;
import io.xeres.common.message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static io.xeres.app.net.peer.PeerAttribute.PEER_CONNECTION;
import static io.xeres.common.message.MessageHeaders.buildMessageHeaders;

@Component
public class PeerConnectionManager
{
	private static final Logger log = LoggerFactory.getLogger(PeerConnectionManager.class);

	private final SimpMessageSendingOperations messagingTemplate;

	private final Map<Long, PeerConnection> peers = new ConcurrentHashMap<>();

	public PeerConnectionManager(SimpMessageSendingOperations messagingTemplate)
	{
		this.messagingTemplate = messagingTemplate;
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
		return peerConnection;
	}

	public void updatePeer(Location location)
	{
		if (!peers.containsKey(location.getId()))
		{
			throw new IllegalStateException("Location " + location + " is not in the list of peers");
		}
		peers.get(location.getId()).updateLocation(location);
	}

	public PeerConnection getPeerByLocationId(long id)
	{
		return peers.get(id);
	}

	public void removePeer(Location location)
	{
		if (!peers.containsKey(location.getId()))
		{
			throw new IllegalStateException("Location " + location + " is not in the list of peers");
		}
		peers.remove(location.getId());
	}

	public void shutdown()
	{
		peers.forEach((id, peerConnection) -> peerConnection.shutdown());
	}

	public ChannelFuture writeItem(Location location, Item item, RsService rsService)
	{
		PeerConnection peer = peers.get(location.getId());
		if (peer != null)
		{
			return writeItem(peer, item, rsService);
		}
		log.warn("Peer with location {} not found while trying to write item. User disconnected?", location);
		return null; // XXX: use executor.newFailedFuture()? but where do I get the executor from?
	}

	public ChannelFuture writeItem(PeerConnection peerConnection, Item item, RsService rsService)
	{
		item.setOutgoing(peerConnection.getCtx().alloc(), 2, rsService.getServiceType(), rsService.getItemSubtype(item));
		return writeItem(peerConnection.getCtx(), item);
	}

	/**
	 * Serializes an item to make a signature out of it.
	 *
	 * @param item      the item
	 * @param rsService the service
	 * @return a ByteBuf. Don't forget to release() it once you're done
	 */
	public ByteBuf serializeItemForSignature(Item item, RsService rsService)
	{
		item.setSerialization(Unpooled.buffer().alloc(), 2, rsService.getServiceType(), rsService.getItemSubtype(item));
		return item.serializeItem(EnumSet.of(SerializationFlags.SIGNATURE)).getBuffer();
	}

	public void doForAllPeers(Consumer<PeerConnection> action, RsService rsService)
	{
		peers.forEach((peerId, peerConnection) ->
		{
			if (peerConnection.isServiceSupported(rsService))
			{
				action.accept(peerConnection);
			}
		});
	}

	public static ChannelFuture writeItem(ChannelHandlerContext ctx, Item item)
	{
		var rawItem = item.serializeItem(EnumSet.noneOf(SerializationFlags.class));
		log.debug("==> {}", item);
		log.trace("Message content: {}", rawItem);
		return ctx.writeAndFlush(rawItem);
	}

	public void sendToSubscriptions(String path, MessageType type, Object payload)
	{
		Map<String, Object> headers = buildMessageHeaders(type);
		sendToSubscriptions(path, headers, payload);
	}

	public void sendToSubscriptions(String path, MessageType type, long destination, Object payload)
	{
		Map<String, Object> headers = buildMessageHeaders(type, String.valueOf(destination));
		sendToSubscriptions(path, headers, payload);
	}

	public void sendToSubscriptions(String path, MessageType type, Identifier destination, Object payload)
	{
		Map<String, Object> headers = buildMessageHeaders(type, destination.toString());
		sendToSubscriptions(path, headers, payload);
	}

	public void sendToSubscriptions(String path, Map<String, Object> headers, Object payload)
	{
		messagingTemplate.convertAndSend(path, payload, headers);
	}
}
