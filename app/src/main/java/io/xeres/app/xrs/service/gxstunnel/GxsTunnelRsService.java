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

import io.xeres.app.crypto.aes.AES;
import io.xeres.app.crypto.dh.DiffieHellman;
import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.app.crypto.hmac.sha1.Sha1HMac;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.service.IdentityService;
import io.xeres.app.xrs.common.SecurityKey;
import io.xeres.app.xrs.common.Signature;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemUtils;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceMaster;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxstunnel.item.*;
import io.xeres.app.xrs.service.turtle.TurtleRouter;
import io.xeres.app.xrs.service.turtle.TurtleRsClient;
import io.xeres.app.xrs.service.turtle.item.*;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.util.ExecutorUtils;
import io.xeres.common.util.SecureRandomUtils;
import org.bouncycastle.util.BigIntegers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import static io.xeres.app.xrs.common.SecurityKey.Flags.DISTRIBUTION_ADMIN;
import static io.xeres.app.xrs.common.SecurityKey.Flags.TYPE_PUBLIC_ONLY;
import static io.xeres.app.xrs.service.RsServiceType.GXS_TUNNEL;
import static io.xeres.app.xrs.service.RsServiceType.TURTLE;
import static io.xeres.app.xrs.service.gxstunnel.GxsTunnelStatus.*;
import static io.xeres.app.xrs.service.gxstunnel.TunnelDhInfo.Status.HALF_KEY_DONE;
import static io.xeres.app.xrs.service.gxstunnel.TunnelDhInfo.Status.UNINITIALIZED;

@Component
public class GxsTunnelRsService extends RsService implements RsServiceMaster<GxsTunnelRsClient>, TurtleRsClient
{
	private static final Logger log = LoggerFactory.getLogger(GxsTunnelRsService.class);

	private static final Duration TUNNEL_DELAY_BETWEEN_RESEND = Duration.ofSeconds(10);

	private static final Duration TUNNEL_KEEP_ALIVE_TIMEOUT = Duration.ofSeconds(6);

	private static final Duration TUNNEL_MANAGEMENT_DELAY = Duration.ofSeconds(2);

	private final AtomicLong counter = new AtomicLong();

	private final Map<Integer, GxsTunnelRsClient> clients = new HashMap<>();
	private final RsServiceRegistry rsServiceRegistry;
	private final DatabaseSessionManager databaseSessionManager;
	private final IdentityService identityService;

	private ScheduledExecutorService executorService;

	private final Map<Location, TunnelPeerInfo> contacts = new ConcurrentHashMap<>(); // _gxs_tunnel_contacts
	private final Map<Location, TunnelDhInfo> peers = new ConcurrentHashMap<>(); // gxs_tunnel_virtual_peer_ids

	private final ReentrantLock tunnelDataItemLock = new ReentrantLock();
	private final PriorityQueue<GxsTunnelDataItem> tunnelDataItems = new PriorityQueue<>();

	private GxsId ownGxsId;
	private TurtleRouter turtleRouter;

	public GxsTunnelRsService(RsServiceRegistry rsServiceRegistry, DatabaseSessionManager databaseSessionManager, IdentityService identityService)
	{
		super(rsServiceRegistry);
		this.rsServiceRegistry = rsServiceRegistry;
		this.databaseSessionManager = databaseSessionManager;
		this.identityService = identityService;
	}

	@Override
	public void initialize()
	{
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			ownGxsId = identityService.getOwnIdentity().getGxsId();
		}

		executorService = ExecutorUtils.createFixedRateExecutor(this::manageResending,
				getInitPriority().getMaxTime() + TUNNEL_DELAY_BETWEEN_RESEND.toSeconds(),
				TUNNEL_MANAGEMENT_DELAY.toSeconds());
	}

	@Override
	public void cleanup()
	{
		ExecutorUtils.cleanupExecutor(executorService);
	}

	private void manageResending()
	{
		// XXX: use a reentrant lock and utility functions to send items? only the encrypted item is important. rest can be sent directly
		var now = Instant.now();

		try
		{
			tunnelDataItemLock.lock();

			var item = tunnelDataItems.poll();
			if (item == null || Duration.between(item.getLastSendingAttempt(), now).compareTo(TUNNEL_DELAY_BETWEEN_RESEND) < 0)
			{
				return;
			}
			sendEncryptedTunnelData(item.getLocation(), item);
			item.updateLastSendingAttempt();

			tunnelDataItems.offer(item);
		}
		finally
		{
			tunnelDataItemLock.unlock();
		}

		// XXX: there should be a way to remove them?! I think it's only when the tunnel is removed, but I can't see where it's done in RS
	}

	// XXX: use this for sending encrypted data directly... it will retry them! used by getTunnelInfo() (reading)
	private void sendTunnelDataItem(Location destination, GxsTunnelDataItem item)
	{
		try
		{
			tunnelDataItemLock.lock();

			sendEncryptedTunnelData(destination, item);
			item.setForResending(destination);
			tunnelDataItems.offer(item);
		}
		finally
		{
			tunnelDataItemLock.unlock();
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
		var serviceId = client.onGxsTunnelInitialization(this);
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
		var peer = peers.get(virtualLocation);
		if (peer == null)
		{
			log.error("Cannot find peer for {}", virtualLocation);
			return;
		}

		PublicKey publicKey = null;

		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			publicKey = identityService.findByGxsId(item.getSignerPublicKey().getKeyId())
					.map(GxsGroupItem::getAdminPublicKey)
					.orElse(item.getSignerPublicKey() != null ? RSA.getPublicKey(item.getSignerPublicKey().getData()) : null);
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			log.debug("Error while trying to get public key for {}: {}", item.getSignerPublicKey().getKeyId(), e.getMessage());
		}

		if (publicKey == null)
		{
			log.warn("Cannot find public key for {}", item);
			return;
		}

		// XXX: check public key? see the algo and where it's used

		if (!item.getSignerPublicKey().getKeyId().equals(item.getSignature().getGxsId()))
		{
			log.warn("Signature does not match public key for {}", peer);
			return;
		}

		if (!RSA.verify(publicKey, item.getSignature().getData(), BigIntegers.asUnsignedByteArray(item.getPublicKey())))
		{
			log.error("Signature verification failed for {}", peer);
			return;
		}

		if (peer.getKeyPair() == null)
		{
			log.error("No information on peer {}", peer);
			return;
		}
		if (peer.getStatus() == TunnelDhInfo.Status.KEY_AVAILABLE)
		{
			restartDhSession(virtualLocation);
		}

		var tunnelId = VirtualLocation.fromGxsIds(ownGxsId, item.getSignerPublicKey().getKeyId());
		peer.setTunnelId(tunnelId);

		var commonSecret = DiffieHellman.generateCommonSecretKey(peer.getKeyPair().getPrivate(), publicKey); // XXX: catch IllegalArgumentException? I think so...
		peer.setStatus(TunnelDhInfo.Status.KEY_AVAILABLE);

		contacts.put(tunnelId, new TunnelPeerInfo(generateAesKey(commonSecret), virtualLocation, peer.getDirection(), item.getSignature().getGxsId()));

		sendEncryptedTunnelData(tunnelId, new GxsTunnelStatusItem(GxsTunnelStatusItem.Status.ACK_DISTANT_CONNECTION));
	}

	private byte[] generateAesKey(byte[] commonSecret)
	{
		var aesKey = new byte[16];
		var digest = new Sha1MessageDigest();
		digest.update(commonSecret);
		System.arraycopy(digest.getBytes(), 0, aesKey, 0, 16);
		return aesKey;
	}

	private void handleEncryptedData(Sha1Sum hash, Location virtualLocation, ByteBuffer buf)
	{
		if (buf.remaining() < 8 + Sha1Sum.LENGTH)
		{
			log.error("Encrypted data for hash {}, virtual location {} is too short", hash, virtualLocation);
			return;
		}

		var peer = peers.get(virtualLocation);
		if (peer == null)
		{
			log.error("Cannot find peer for hash {}, virtual location {}", hash, virtualLocation);
			return;
		}

		var tunnelPeerInfo = contacts.get(peer.getTunnelId());
		if (tunnelPeerInfo == null)
		{
			log.error("Cannot find tunnel peer {}, virtual location {}", peer, virtualLocation);
			return;
		}

		var iv = new byte[8];
		buf.get(iv);
		var hmac = new byte[Sha1Sum.LENGTH];
		buf.get(hmac);
		var encryptedItem = new byte[buf.remaining()];
		buf.get(encryptedItem);

		var hmacCheck = new Sha1HMac(new SecretKeySpec(tunnelPeerInfo.getAesKey(), "AES"));
		hmacCheck.update(encryptedItem);

		if (!Arrays.equals(hmac, hmacCheck.getBytes()))
		{
			log.error("HMAC check failed for peer {}, virtual location {}. Resetting DH session.", peer, virtualLocation);
			restartDhSession(virtualLocation);
			return;
		}

		byte[] decryptedItem;

		try
		{
			decryptedItem = AES.decrypt(tunnelPeerInfo.getAesKey(), iv, encryptedItem);
		}
		catch (IllegalArgumentException e)
		{
			log.error("Decryption failed for peer {}, virtual location {}. : {}. Resetting DH session.", peer, virtualLocation, e.getMessage());
			restartDhSession(virtualLocation);
			return;
		}

		tunnelPeerInfo.setStatus(CAN_TALK);
		tunnelPeerInfo.updateLastContact();

		var item = ItemUtils.deserializeItem(decryptedItem, rsServiceRegistry);

		if (item.getServiceType() == RsServiceType.NONE.getType())
		{
			log.error("Deserialization failed for peer {}, virtual location {}", peer, virtualLocation);
			return;
		}

		tunnelPeerInfo.addReceivedSize(decryptedItem.length);

		handleIncomingItem(peer.getTunnelId(), item);
	}

	private void handleIncomingItem(Location tunnelId, Item item)
	{
		switch (item)
		{
			case GxsTunnelDataItem gxsTunnelDataItem -> handleTunnelDataItem(tunnelId, gxsTunnelDataItem);
			case GxsTunnelDataAckItem gxsTunnelDataAckItem -> handleTunnelDataItemAck(tunnelId, gxsTunnelDataAckItem);
			case GxsTunnelStatusItem gxsTunnelStatusItem -> handleTunnelStatusItem(tunnelId, gxsTunnelStatusItem);
			default -> log.warn("Unknown packet type received: {}", item.getSubType());
		}
	}

	private void handleTunnelDataItem(Location tunnelId, GxsTunnelDataItem item)
	{
		// Acknowledge reception
		var ackItem = new GxsTunnelDataAckItem(item.getCounter());
		sendEncryptedTunnelData(tunnelId, ackItem);

		var client = clients.get(item.getServiceId());
		if (client == null)
		{
			log.warn("No registered service with ID {}, rejecting item", item.getServiceId());
			return;
		}

		var isClientSide = false;

		var tunnelPeerInfo = contacts.get(tunnelId);
		if (tunnelPeerInfo == null)
		{
			log.error("No contact found for {}", item.getServiceId());
			return;
		}

		tunnelPeerInfo.addService(item.getServiceId());
		isClientSide = tunnelPeerInfo.getDirection() == TunnelDirection.SERVER;

		// We check if we already received.
		if (tunnelPeerInfo.checkIfMessageAlreadyReceivedAndRecord(item.getCounter()))
		{
			log.warn("Tunnel peer already received a message for {}", item.getServiceId());
			return;
		}

		if (client.onGxsTunnelDataAuthorization(tunnelPeerInfo.getDestination(), tunnelId, isClientSide))
		{
			client.onGxsTunnelDataReceived(tunnelId, item.getTunnelData());
		}
	}

	private void handleTunnelDataItemAck(Location tunnelId, GxsTunnelDataAckItem item)
	{
		try
		{
			tunnelDataItemLock.lock();

			tunnelDataItems.removeIf(gxsTunnelDataItem -> gxsTunnelDataItem.getCounter() == item.getCounter());
		}
		finally
		{
			tunnelDataItemLock.unlock();
		}
	}

	private void handleTunnelStatusItem(Location tunnelId, GxsTunnelStatusItem item)
	{
		switch (item.getStatus())
		{
			case CLOSING_DISTANT_CONNECTION ->
			{
				var tunnelPeerInfo = contacts.get(tunnelId);
				if (tunnelPeerInfo == null)
				{
					log.error("Cannot mark tunnel connection as closed. No connected opened for tunnel id {}", tunnelId);
					return;
				}
				if (tunnelPeerInfo.getDirection() == TunnelDirection.CLIENT)
				{
					tunnelPeerInfo.setStatus(REMOTELY_CLOSED);
				}
				else
				{
					tunnelPeerInfo.setStatus(TUNNEL_DOWN);
				}
				log.debug("Remote tunnel for tunnel id {} closed", tunnelId);
				notifyClients(tunnelId, REMOTELY_CLOSED);
			}
			case KEEP_ALIVE -> log.debug("Received keep alive for tunnel {}", tunnelId); // Nothing to do, decryption method updated the activity for the tunnel
			case ACK_DISTANT_CONNECTION -> notifyClients(tunnelId, CAN_TALK);
			default -> log.warn("Unknown status received: {}", item.getStatus());
		}
	}

	private void notifyClients(Location tunnelId, GxsTunnelStatus status)
	{
		var tunnelPeerInfo = contacts.get(tunnelId);
		if (tunnelPeerInfo == null)
		{
			return;
		}

		tunnelPeerInfo.getClientServices().forEach(serviceId -> {
			var client = clients.get(serviceId);
			if (client != null)
			{
				client.onGxsTunnelStatusChanged(tunnelId, status);
			}
		});
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
		dhInfo.clear();
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
		var peer = peers.remove(virtualLocation);

		if (peer == null)
		{
			log.error("Cannot remove virtual peer {} because it's not found", virtualLocation);
			return;
		}

		var tunnelPeerInfo = contacts.get(peer.getTunnelId());
		if (tunnelPeerInfo == null)
		{
			log.error("Cannot find tunnel id {} in contact list", peer.getTunnelId());
			return;
		}

		// Notify all clients that this tunnel is down.
		if (peer.getTunnelId().equals(virtualLocation))
		{
			tunnelPeerInfo.setStatus(TUNNEL_DOWN);
			tunnelPeerInfo.clearLocation();

			tunnelPeerInfo.getClientServices().forEach(serviceId -> {
				var client = clients.get(serviceId);
				if (client != null)
				{
					client.onGxsTunnelStatusChanged(peer.getTunnelId(), TUNNEL_DOWN);
				}
			});
		}
	}

	private void restartDhSession(Location virtualLocation)
	{
		var dhInfo = peers.getOrDefault(virtualLocation, new TunnelDhInfo());
		dhInfo.setStatus(UNINITIALIZED);
		dhInfo.setKeyPair(DiffieHellman.generateKeys());
		dhInfo.setStatus(HALF_KEY_DONE);

		sendDhPublicKey(virtualLocation, dhInfo.getKeyPair());
	}

	private void sendDhPublicKey(Location virtualLocation, KeyPair keyPair)
	{
		assert keyPair != null;

		// Sign the public key
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			var ownIdentity = identityService.getOwnIdentity();
			var signerSecurityKey = new SecurityKey(ownIdentity.getGxsId(), EnumSet.of(DISTRIBUTION_ADMIN, TYPE_PUBLIC_ONLY), 0, 0, ownIdentity.getAdminPublicKey().getEncoded()); // XXX: validity, flags, ... ok?

			var publicKeyNum = ((DHPublicKey) keyPair.getPublic()).getY();

			var signature = new Signature(ownIdentity.getGxsId(), RSA.sign(BigIntegers.asUnsignedByteArray(publicKeyNum), ownIdentity.getAdminPrivateKey())); // XXX: no type... correct?

			var item = new GxsTunnelDhPublicKeyItem(publicKeyNum, signature, signerSecurityKey);
			var serializedItem = ItemUtils.serializeItem(item, this);

			// The preceding IV is made of zeroes as this is the only clear item that is sent.
			var data = new byte[serializedItem.length + 8];
			System.arraycopy(serializedItem, 0, data, 8, serializedItem.length);

			turtleRouter.sendTurtleData(virtualLocation, new TurtleGenericFastDataItem(data));
		}
	}

	private void sendEncryptedTunnelData(Location destination, GxsTunnelItem item)
	{
		var serializedItem = ItemUtils.serializeItem(item, this);

		var peer = contacts.get(destination);
		if (peer == null)
		{
			log.error("Cannot find peer for {}", destination);
			return;
		}

		if (peer.getStatus() != CAN_TALK)
		{
			log.error("Cannot talk to tunnel id {}, status is {}", destination, peer.getStatus());
			return;
		}

		peer.addSentSize(serializedItem.length);

		var iv = new byte[8];
		SecureRandomUtils.nextBytes(iv);

		var key = peer.getAesKey();

		var encryptedItem = AES.encrypt(key, iv, serializedItem);
		var turtleItem = new TurtleGenericFastDataItem(createTurtleData(key, iv, encryptedItem));
		turtleRouter.sendTurtleData(destination, turtleItem);
	}

	// XXX: missing public methods:
	// XXX: getTunnelInfo()

	public Location requestSecuredTunnel(GxsId from, GxsId to, int serviceId)
	{
		var hash = DestinationHash.createRandomHash(to);
		var tunnelId = VirtualLocation.fromGxsIds(from, to);

		if (contacts.get(tunnelId) != null)
		{
			log.error("Tunnel {} already exists", tunnelId);
			return null;
		}

		var peer = new TunnelPeerInfo(hash, to, serviceId);
		contacts.put(tunnelId, peer);

		turtleRouter.startMonitoringTunnels(hash, this, false);

		return tunnelId;
	}

	public void sendData(Location tunnelId, int serviceId, byte[] data)
	{
		var tunnelPeerInfo = contacts.get(tunnelId);
		if (tunnelPeerInfo == null)
		{
			log.error("Not tunnel with this id");
			return;
		}

		var client = clients.get(serviceId);
		if (client == null)
		{
			log.error("Cannot find client for {}", serviceId);
			return;
		}
		sendTunnelDataItem(tunnelId, new GxsTunnelDataItem(getUniquePacketCounter(), serviceId, data));
	}

	public void closeExistingTunnel(Location tunnelId, int serviceId)
	{
		var tunnelPeerInfo = contacts.get(tunnelId);
		if (tunnelPeerInfo == null)
		{
			log.error("Cannot close distant tunnel connection. No connection opened for tunnel id {}", tunnelId);
			return;
		}

		Sha1Sum hash;

		var tunnelDhInfo = peers.get(tunnelPeerInfo.getLocation());
		if (tunnelDhInfo != null)
		{
			hash = tunnelDhInfo.getHash();
		}
		else
		{
			hash = tunnelPeerInfo.getHash();
		}

		if (!tunnelPeerInfo.getClientServices().contains(serviceId))
		{
			log.error("Tunnel {} is not associated with service {}", tunnelId, serviceId);
			return;
		}

		tunnelPeerInfo.removeService(serviceId);

		if (tunnelPeerInfo.getClientServices().isEmpty())
		{
			// No clients, we can close the tunnel.

			sendEncryptedTunnelData(tunnelId, new GxsTunnelStatusItem(GxsTunnelStatusItem.Status.CLOSING_DISTANT_CONNECTION));

			if (tunnelPeerInfo.getDirection() == TunnelDirection.SERVER)
			{
				turtleRouter.stopMonitoringTunnels(hash);
			}

			contacts.remove(tunnelId);
		}
	}

	private long getUniquePacketCounter()
	{
		return counter.getAndIncrement();
	}

	private byte[] createTurtleData(byte[] aesKey, byte[] iv, byte[] encryptedItem)
	{
		var turtleData = new byte[iv.length + Sha1Sum.LENGTH + encryptedItem.length];

		System.arraycopy(iv, 0, turtleData, 0, iv.length);

		var hmac = new Sha1HMac(new SecretKeySpec(aesKey, "AES"));
		hmac.update(encryptedItem);

		System.arraycopy(hmac.getBytes(), 0, turtleData, iv.length, Sha1Sum.LENGTH);
		System.arraycopy(encryptedItem, 0, turtleData, iv.length + Sha1Sum.LENGTH, encryptedItem.length);
		return turtleData;
	}

	@Override
	public RsServiceType getMasterServiceType()
	{
		return TURTLE;
	}
}
