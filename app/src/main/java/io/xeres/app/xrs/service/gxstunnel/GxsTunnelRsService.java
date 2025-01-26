/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.gxstunnel;

import io.xeres.app.crypto.dh.DiffieHellman;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.service.IdentityService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemUtils;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceMaster;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxstunnel.item.GxsTunnelDhPublicKeyItem;
import io.xeres.app.xrs.service.turtle.TurtleRouter;
import io.xeres.app.xrs.service.turtle.TurtleRsClient;
import io.xeres.app.xrs.service.turtle.item.TunnelDirection;
import io.xeres.app.xrs.service.turtle.item.TurtleGenericDataItem;
import io.xeres.app.xrs.service.turtle.item.TurtleGenericTunnelItem;
import io.xeres.app.xrs.service.turtle.item.TurtleSearchResultItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Sha1Sum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.xeres.app.xrs.service.RsServiceType.GXS_TUNNEL;
import static io.xeres.app.xrs.service.RsServiceType.TURTLE;
import static io.xeres.app.xrs.service.gxstunnel.TunnelDhInfo.Status.HALF_KEY_DONE;
import static io.xeres.app.xrs.service.gxstunnel.TunnelDhInfo.Status.UNINITIALIZED;
import static io.xeres.app.xrs.service.gxstunnel.TunnelPeerInfo.Status.CAN_TALK;

@Component
public class GxsTunnelRsService extends RsService implements RsServiceMaster<GxsTunnelRsClient>, TurtleRsClient
{
	private static final Logger log = LoggerFactory.getLogger(GxsTunnelRsService.class);

	private static final Duration TUNNEL_DELAY_BETWEEN_RESEND = Duration.ofSeconds(10);

	private static final Duration TUNNEL_KEEP_ALIVE_TIMEOUT = Duration.ofSeconds(6);

	private final Map<Integer, GxsTunnelRsClient> clients = new HashMap<>();
	private final RsServiceRegistry rsServiceRegistry;

	private final Map<Integer, TunnelPeerInfo> contacts = new ConcurrentHashMap<>();
	private final Map<Location, TunnelDhInfo> peers = new ConcurrentHashMap<>();

	private final GxsId ownGxsId;
	private TurtleRouter turtleRouter;

	public GxsTunnelRsService(RsServiceRegistry rsServiceRegistry, DatabaseSessionManager databaseSessionManager, IdentityService identityService)
	{
		super(rsServiceRegistry);
		this.rsServiceRegistry = rsServiceRegistry;

		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			ownGxsId = identityService.getOwnIdentity().getGxsId();
		}
	}

	@Override
	public RsServiceType getServiceType()
	{
		return GXS_TUNNEL;
	}

	@Override
	public void addRsSlave(GxsTunnelRsClient client)
	{
		var serviceId = client.initializeGxsTunnel(this);
		clients.put(serviceId, client);
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		// Nothing to handle
	}

	@Override
	public void initializeTurtle(TurtleRouter turtleRouter)
	{
		this.turtleRouter = turtleRouter;
	}

	@Override
	public boolean handleTunnelRequest(PeerConnection sender, Sha1Sum hash)
	{
		// Only answer request that are for us.
		var destination = DestinationHash.getGxsIdFromHash(hash);
		return ownGxsId.equals(destination);
	}

	@Override
	public void receiveTurtleData(TurtleGenericTunnelItem item, Sha1Sum hash, Location virtualLocation, TunnelDirection tunnelDirection)
	{
		switch (item)
		{
			case TurtleGenericDataItem turtleGenericDataItem ->
			{
				var buf = ByteBuffer.wrap(turtleGenericDataItem.getTunnelData());

				// The packet's first 8 bytes contain the IV
				if (buf.remaining() < 8)
				{
					log.error("Gxs tunnel data contains less than 8 bytes, dropping");
					return;
				}

				if (hasNoIv(buf))
				{
					// Clear data
					buf.position(4);
					buf.compact();
					var deserializedItem = ItemUtils.deserializeItem(buf.array(), rsServiceRegistry);
					if (deserializedItem instanceof GxsTunnelDhPublicKeyItem gxsTunnelDhPublicKeyItem)
					{
						handleRecvDhPublicKeyItem(virtualLocation, gxsTunnelDhPublicKeyItem);
					}
					else
					{
						log.warn("Unknown deserialized item: {}", deserializedItem);
					}
				}
				else
				{
					// Encrypted data
					handleEncryptedData(hash, virtualLocation, buf);
				}
			}
			case null -> throw new IllegalStateException("Null item");
			default -> log.warn("Unknown packet type received: {}", item.getSubType());
		}
	}

	private boolean hasNoIv(ByteBuffer buf)
	{
		return buf.getLong(0) == 0L;
	}

	private void handleRecvDhPublicKeyItem(Location virtualLocation, GxsTunnelDhPublicKeyItem item)
	{
		// XXX
	}

	private void handleEncryptedData(Sha1Sum hash, Location virtualLocation, ByteBuffer buf)
	{

	}

	@Override
	public List<byte[]> receiveSearchRequest(byte[] query, int maxHits)
	{
		return List.of();
	}

	@Override
	public void receiveSearchRequestString(PeerConnection sender, String keywords)
	{

	}

	@Override
	public void receiveSearchResult(int requestId, TurtleSearchResultItem item)
	{

	}

	@Override
	public void addVirtualPeer(Sha1Sum hash, Location virtualLocation, TunnelDirection direction)
	{
		log.debug("Received new virtual peer {} for hash {}, direction {}", virtualLocation, hash, direction);

		var dhInfo = peers.getOrDefault(virtualLocation, new TunnelDhInfo());
		// XXX: fill in dhInfo
		dhInfo.setDirection(direction);
		dhInfo.setHash(hash);
		dhInfo.setStatus(UNINITIALIZED);

		if (direction == TunnelDirection.SERVER)
		{
			var found = contacts.values().stream()
					.filter(tunnelPeerInfo -> tunnelPeerInfo.getHash().equals(hash))
					.findAny();

			if (found.isEmpty())
			{
				log.error("No pre-registered peer for hash {} on client side", hash);
				return;
			}

			if (found.get().getStatus() == CAN_TALK)
			{
				log.error("Session already opened and alive");
				return;
			}
		}
		log.debug("Adding virtual peer {} for hash {}", virtualLocation, hash);

		restartDhSession(virtualLocation);
	}

	@Override
	public void removeVirtualPeer(Sha1Sum hash, Location virtualLocation)
	{

	}

	private void restartDhSession(Location virtualLocation)
	{
		var dhInfo = peers.getOrDefault(virtualLocation, new TunnelDhInfo());
		dhInfo.setStatus(UNINITIALIZED);
		dhInfo.setKeyPair(DiffieHellman.generateKeys());
		dhInfo.setStatus(HALF_KEY_DONE);

		sendDhPublicKey(dhInfo.getKeyPair(), virtualLocation);
	}

	private void sendDhPublicKey(KeyPair keyPair, Location virtualLocation)
	{
		assert keyPair != null;

		// XXX: send public key... check how RS does it... it's complex...
	}

	@Override
	public RsServiceType getMasterServiceType()
	{
		return TURTLE;
	}

	public Location createTunnel(GxsId from, GxsId to) // XXX: missing service ID
	{
		var hash = DestinationHash.createRandomHash(to);
		var tunnelId = VirtualLocation.fromGxsIds(from, to);
		// XXX: find in gxs tunnel contacts...

		// XXX: insert in contacts (as info)

		turtleRouter.startMonitoringTunnels(hash, this, false);

		return tunnelId;
	}
}
