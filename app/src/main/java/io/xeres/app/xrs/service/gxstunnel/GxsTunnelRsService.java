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
import java.io.IOException;
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

/**
 * Generic tunnel service.
 * <p>
 * Services wanting to use it just need to implement {@link GxsTunnelRsClient}.
 * They can then request a tunnel from their identity to another identity and get a
 * handle (<i>tunnel id</i>). They can send data using that <i>tunnel id</i>. Several services can
 * use the same tunnel if the destination is the same. A <i>service id</i> is used to differentiate
 * between services.
 */
@Component
public class GxsTunnelRsService extends RsService implements RsServiceMaster<GxsTunnelRsClient>, TurtleRsClient
{
	private static final Logger log = LoggerFactory.getLogger(GxsTunnelRsService.class);

	private static final Duration TUNNEL_DELAY_BETWEEN_RESEND = Duration.ofSeconds(10);

	private static final Duration TUNNEL_KEEP_ALIVE_TIMEOUT = Duration.ofSeconds(6);

	private static final Duration TUNNEL_MANAGEMENT_DELAY = Duration.ofSeconds(2);

	private static final Duration TUNNEL_MESSAGES_DUPLICATE_DELAY = Duration.ofMinutes(10);

	private final AtomicLong counter = new AtomicLong();

	private final Map<Integer, GxsTunnelRsClient> clients = new HashMap<>();
	private final RsServiceRegistry rsServiceRegistry;
	private final DatabaseSessionManager databaseSessionManager;
	private final IdentityService identityService;

	private ScheduledExecutorService executorService;

	/**
	 * Current peers we can talk to. Key is a tunnel id.
	 */
	private final Map<Location, TunnelPeerInfo> contacts = new ConcurrentHashMap<>();

	/**
	 * Current virtual peers. Key is a turtle virtual peer.
	 */
	private final Map<Location, TunnelDhInfo> dhPeers = new ConcurrentHashMap<>();

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

		executorService = ExecutorUtils.createFixedRateExecutor(this::manageTunnels,
				getInitPriority().getMaxTime() + TUNNEL_DELAY_BETWEEN_RESEND.toSeconds(),
				TUNNEL_MANAGEMENT_DELAY.toSeconds());
	}

	@Override
	public void cleanup()
	{
		ExecutorUtils.cleanupExecutor(executorService);
	}

	private void manageTunnels()
	{
		var now = Instant.now();

		manageResending(now);
		manageDiggingAndCleanup(now);
	}

	private void manageResending(Instant now)
	{
		tunnelDataItemLock.lock();
		try
		{
			var item = tunnelDataItems.peek();
			if (item == null || Duration.between(item.getLastSendingAttempt(), now).compareTo(TUNNEL_DELAY_BETWEEN_RESEND) < 0)
			{
				return;
			}

			tunnelDataItems.poll(); // We need to remove it so that the priority is changed
			log.debug("Resending tunnel data item for tunnel {}", item.getLocation());
			sendEncryptedTunnelData(item.getLocation(), item);
			item.updateLastSendingAttempt();
			tunnelDataItems.offer(item); // Insert back with the proper priority
		}
		finally
		{
			tunnelDataItemLock.unlock();
		}
		// XXX: there should be a way to remove them?! I think it's only when the tunnel is removed, but I can't see where it's done in RS
	}

	private void manageDiggingAndCleanup(Instant now)
	{
		var it = contacts.entrySet().iterator();
		while (it.hasNext())
		{
			var tunnelPeerInfoEntry = it.next();

			// Remove tunnels that were remotely closed as we
			// cannot use them anymore.
			if (tunnelPeerInfoEntry.getValue().getStatus() == REMOTELY_CLOSED && tunnelPeerInfoEntry.getValue().getLastContact().plusSeconds(20).isBefore(now))
			{
				log.debug("Removing tunnel {}", tunnelPeerInfoEntry.getKey());
				it.remove();
				continue;
			}

			// Re-digg tunnels that have died out of inaction
			if (tunnelPeerInfoEntry.getValue().getStatus() == CAN_TALK && tunnelPeerInfoEntry.getValue().getLastContact().plusSeconds(20).plus(TUNNEL_KEEP_ALIVE_TIMEOUT).isBefore(now))
			{
				log.debug("Connection interrupted with tunnelPeerInfo");
				tunnelPeerInfoEntry.getValue().setStatus(TUNNEL_DOWN);
				notifyClients(tunnelPeerInfoEntry.getKey(), tunnelPeerInfoEntry.getValue().getStatus()); // XXX: OK?!
				tunnelPeerInfoEntry.getValue().clearLocation();

				// Reset the turtle router monitoring. Avoids having to wait 60 seconds for the tunnel to die.
				if (tunnelPeerInfoEntry.getValue().getDirection() == TunnelDirection.SERVER)
				{
					log.debug("Forcing new tunnel");
					turtleRouter.forceReDiggTunnel(DestinationHash.createRandomHash(tunnelPeerInfoEntry.getValue().getDestination()));
				}
			}

			// Send keep alive to active tunnels.
			if (tunnelPeerInfoEntry.getValue().getStatus() == CAN_TALK && tunnelPeerInfoEntry.getValue().getLastKeepAliveSent().plus(TUNNEL_KEEP_ALIVE_TIMEOUT).isBefore(now))
			{
				log.debug("Sending keep alive to tunnel {}", tunnelPeerInfoEntry.getKey());
				sendEncryptedTunnelData(tunnelPeerInfoEntry.getKey(), new GxsTunnelStatusItem(GxsTunnelStatusItem.Status.KEEP_ALIVE));
				tunnelPeerInfoEntry.getValue().updateLastKeepAlive();
			}

			// Clean old received messages
			tunnelPeerInfoEntry.getValue().cleanupReceivedMessagesOlderThan(TUNNEL_MESSAGES_DUPLICATE_DELAY);
		}
	}

	private void sendTunnelDataItem(Location destination, GxsTunnelDataItem item)
	{
		tunnelDataItemLock.lock();
		try
		{
			log.debug("Sending tunnel data item {} to tunnel {}", item, destination);
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
		var isForUs = ownGxsId.equals(destination);
		if (isForUs)
		{
			log.trace("Tunnel request from {} is for us", sender);
		}
		return isForUs;
	}

	@Override
	public void receiveTurtleData(TurtleGenericTunnelItem item, Sha1Sum hash, Location virtualLocation, TunnelDirection tunnelDirection)
	{
		log.debug("Received tunnel data item {} from {} (direction: {})", item, virtualLocation, tunnelDirection);
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
					// Skip IV placeholder
					buf.position(8);
					buf.compact();
					buf.position(0);
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
			default -> log.warn("Unknown packet subtype received from turtle data: {}", item.getSubType());
		}
	}

	private boolean hasNoIv(ByteBuffer buf)
	{
		buf.mark();
		var result = buf.getLong(0) == 0L;
		buf.reset();
		return result;
	}

	private void handleRecvDhPublicKeyItem(Location virtualLocation, GxsTunnelDhPublicKeyItem item)
	{
		log.debug("Received DH public key from {}", virtualLocation);

		var tunnelDhInfo = dhPeers.get(virtualLocation);
		if (tunnelDhInfo == null)
		{
			log.error("DH: Cannot find tunnelDhInfo for {}", virtualLocation);
			return;
		}

		PublicKey signerPublicKey = null;

		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			signerPublicKey = identityService.findByGxsId(item.getSignature().getGxsId())
					.map(GxsGroupItem::getAdminPublicKey)
					.orElse(getPublicKeySecurely(item.getSignerPublicKey()));
		}

		if (signerPublicKey == null)
		{
			log.error("DH: Cannot find/process signer public key for {}", tunnelDhInfo);
			return;
		}

		if (!item.getSignerPublicKey().getKeyId().equals(item.getSignature().getGxsId()))
		{
			log.error("DH: Signature does not match public key for {}", tunnelDhInfo);
			return;
		}

		if (!RSA.verify(signerPublicKey, item.getSignature().getData(), BigIntegers.asUnsignedByteArray(item.getPublicKey())))
		{
			log.error("DH: Signature verification failed for {}", tunnelDhInfo);
			return;
		}

		if (tunnelDhInfo.getKeyPair() == null)
		{
			log.error("DH: No information on tunnelDhInfo {}", tunnelDhInfo);
			return;
		}
		if (tunnelDhInfo.getStatus() == TunnelDhInfo.Status.KEY_AVAILABLE)
		{
			log.debug("Key already available for {}, restarting DH session...", tunnelDhInfo);
			restartDhSession(virtualLocation);
		}

		var tunnelId = VirtualLocation.fromGxsIds(ownGxsId, item.getSignerPublicKey().getKeyId());
		tunnelDhInfo.setTunnelId(tunnelId);

		var publicKey = DiffieHellman.getPublicKey(item.getPublicKey());
		byte[] commonSecret;
		try
		{
			commonSecret = DiffieHellman.generateCommonSecretKey(tunnelDhInfo.getKeyPair().getPrivate(), publicKey);
		}
		catch (IllegalArgumentException e)
		{
			log.error("DH: Cannot generate common secret key for {}", tunnelDhInfo);
			return;
		}
		tunnelDhInfo.setStatus(TunnelDhInfo.Status.KEY_AVAILABLE);

		var tunnelPeerInfo = contacts.computeIfAbsent(tunnelId, location -> new TunnelPeerInfo());
		tunnelPeerInfo.activate(generateAesKey(commonSecret), virtualLocation, tunnelDhInfo.getDirection(), item.getSignature().getGxsId());

		log.debug("Sending distant connection ack for tunnel {}", tunnelId);
		sendEncryptedTunnelData(tunnelId, new GxsTunnelStatusItem(GxsTunnelStatusItem.Status.ACK_DISTANT_CONNECTION));
	}

	private static PublicKey getPublicKeySecurely(SecurityKey securityKey)
	{
		if (!securityKey.getFlags().contains(TYPE_PUBLIC_ONLY))
		{
			log.warn("Public key misses public flag");
			return null;
		}

		try
		{
			RSA.getPrivateKey(securityKey.getData());
			log.warn("Public key is in fact a private key, rejecting.");
			return null;
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			// All good
		}

		PublicKey publicKey;

		try
		{
			publicKey = RSA.getPublicKeyFromPkcs1(securityKey.getData());
		}
		catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			log.warn("Couldn't decode public key: {}", e.getMessage());
			return null;
		}

		var gxsId = RSA.getGxsId(publicKey);

		if (!securityKey.getKeyId().equals(gxsId))
		{
			// RS used to generate those keys. They're still accepted, but they
			// will be removed one day.

			//noinspection deprecation
			if (!securityKey.getKeyId().equals(RSA.getGxsIdInsecure(publicKey)))
			{
				log.warn("Old style key has wrong fingerprint, rejecting.");
				return null;
			}
			log.warn("Using old style key. The peer should generate a new identity though.");
		}
		return publicKey;
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

		var tunnelDhInfo = dhPeers.get(virtualLocation);
		if (tunnelDhInfo == null)
		{
			log.error("Incoming item not coming out of a registered tunnel for hash {}, virtual location {}. This is unexpected.", hash, virtualLocation);
			return;
		}

		if (tunnelDhInfo.getTunnelId() == null)
		{
			log.error("No tunnel id for tunnelDhInfo for virtual location {}, this shouldn't happen", virtualLocation);
			return;
		}

		var tunnelPeerInfo = contacts.get(tunnelDhInfo.getTunnelId());
		if (tunnelPeerInfo == null)
		{
			log.error("Cannot find tunnel tunnelDhInfo {}, virtual location {}", tunnelDhInfo, virtualLocation);
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
			log.error("HMAC check failed for tunnelDhInfo {}, virtual location {}. Resetting DH session.", tunnelDhInfo, virtualLocation);
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
			log.error("Decryption failed for tunnelDhInfo {}, virtual location {}. : {}. Resetting DH session.", tunnelDhInfo, virtualLocation, e.getMessage());
			restartDhSession(virtualLocation);
			return;
		}

		tunnelPeerInfo.setStatus(CAN_TALK);
		tunnelPeerInfo.updateLastContact();

		var item = ItemUtils.deserializeItem(decryptedItem, rsServiceRegistry);

		if (item.getServiceType() == RsServiceType.NONE.getType())
		{
			log.error("Deserialization failed for tunnelDhInfo {}, virtual location {}", tunnelDhInfo, virtualLocation);
			return;
		}

		tunnelPeerInfo.addReceivedSize(decryptedItem.length);

		handleIncomingItem(tunnelDhInfo.getTunnelId(), item);
	}

	private void handleIncomingItem(Location tunnelId, Item item)
	{
		switch (item)
		{
			case GxsTunnelDataItem gxsTunnelDataItem -> handleTunnelDataItem(tunnelId, gxsTunnelDataItem);
			case GxsTunnelDataAckItem gxsTunnelDataAckItem -> handleTunnelDataItemAck(gxsTunnelDataAckItem);
			case GxsTunnelStatusItem gxsTunnelStatusItem -> handleTunnelStatusItem(tunnelId, gxsTunnelStatusItem);
			default -> log.warn("Unknown packet subtype received from encrypted data: {}", item.getSubType());
		}
	}

	private void handleTunnelDataItem(Location tunnelId, GxsTunnelDataItem item)
	{
		// Acknowledge reception
		var ackItem = new GxsTunnelDataAckItem(item.getCounter());
		log.debug("Sending ack for tunnel {}", tunnelId);
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

	private void handleTunnelDataItemAck(GxsTunnelDataAckItem item)
	{
		tunnelDataItemLock.lock();
		try
		{
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
				client.onGxsTunnelStatusChanged(tunnelId, tunnelPeerInfo.getDestination(), status);
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

		var tunnelDhInfo = dhPeers.computeIfAbsent(virtualLocation, location -> new TunnelDhInfo());
		tunnelDhInfo.clear();
		tunnelDhInfo.setDirection(direction);
		tunnelDhInfo.setHash(hash);
		tunnelDhInfo.setStatus(UNINITIALIZED);

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
		var tunnelDhInfo = dhPeers.remove(virtualLocation);

		if (tunnelDhInfo == null)
		{
			log.error("Cannot remove virtual peer {} because it's not found", virtualLocation);
			return;
		}

		var tunnelPeerInfo = contacts.get(tunnelDhInfo.getTunnelId());
		if (tunnelPeerInfo == null)
		{
			log.error("Cannot find tunnel id {} in contact list", tunnelDhInfo.getTunnelId());
			return;
		}

		// Notify all clients that this tunnel is down.
		if (tunnelDhInfo.getTunnelId().equals(virtualLocation))
		{
			tunnelPeerInfo.setStatus(TUNNEL_DOWN);
			tunnelPeerInfo.clearLocation();

			tunnelPeerInfo.getClientServices().forEach(serviceId -> {
				var client = clients.get(serviceId);
				if (client != null)
				{
					client.onGxsTunnelStatusChanged(tunnelDhInfo.getTunnelId(), tunnelPeerInfo.getDestination(), TUNNEL_DOWN);
				}
			});
		}
	}

	private void restartDhSession(Location virtualLocation)
	{
		var tunnelDhInfo = dhPeers.computeIfAbsent(virtualLocation, location -> new TunnelDhInfo());
		tunnelDhInfo.setStatus(UNINITIALIZED);
		tunnelDhInfo.setKeyPair(DiffieHellman.generateKeys());
		tunnelDhInfo.setStatus(HALF_KEY_DONE);

		sendDhPublicKey(virtualLocation, tunnelDhInfo.getKeyPair());
	}

	private void sendDhPublicKey(Location virtualLocation, KeyPair keyPair)
	{
		assert keyPair != null;

		log.debug("Sending DH public key to {}", virtualLocation);

		// Sign the public key
		try (var ignored = new DatabaseSession(databaseSessionManager))
		{
			var ownIdentity = identityService.getOwnIdentity();
			var signerSecurityKey = new SecurityKey(ownIdentity.getGxsId(), EnumSet.of(DISTRIBUTION_ADMIN, TYPE_PUBLIC_ONLY), ownIdentity.getPublished(), null, RSA.getPublicKeyAsPkcs1(ownIdentity.getAdminPublicKey()));

			var publicKeyNum = ((DHPublicKey) keyPair.getPublic()).getY();

			var signature = new Signature(ownIdentity.getGxsId(), RSA.sign(BigIntegers.asUnsignedByteArray(publicKeyNum), ownIdentity.getAdminPrivateKey()));

			var item = new GxsTunnelDhPublicKeyItem(publicKeyNum, signature, signerSecurityKey);
			var serializedItem = ItemUtils.serializeItem(item, this);

			// The preceding IV is made of zeroes as this is the only clear item that is sent.
			var data = new byte[serializedItem.length + 8];
			System.arraycopy(serializedItem, 0, data, 8, serializedItem.length);

			turtleRouter.sendTurtleData(virtualLocation, new TurtleGenericFastDataItem(data));
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException("Cannot read public key from database: " + e.getMessage(), e);
		}
	}

	private void sendEncryptedTunnelData(Location destination, GxsTunnelItem item)
	{
		var serializedItem = ItemUtils.serializeItem(item, this);

		var tunnelPeerInfo = contacts.get(destination);
		if (tunnelPeerInfo == null)
		{
			log.error("Cannot find tunnelPeerInfo for {} when trying to send encrypted data", destination);
			return;
		}

		if (tunnelPeerInfo.getStatus() != CAN_TALK)
		{
			log.error("Cannot talk to tunnel id {}, status is {}", destination, tunnelPeerInfo.getStatus());
			return;
		}

		tunnelPeerInfo.addSentSize(serializedItem.length);

		var iv = new byte[8];
		SecureRandomUtils.nextBytes(iv);

		var key = tunnelPeerInfo.getAesKey();

		var encryptedItem = AES.encrypt(key, iv, serializedItem);
		var turtleItem = new TurtleGenericFastDataItem(createTurtleData(key, iv, encryptedItem));
		turtleRouter.sendTurtleData(tunnelPeerInfo.getLocation(), turtleItem);
	}

	/**
	 * Asks for a tunnel. The service will request it to the turtle router, and exchange an AES key using DH.
	 * When the tunnel is established, a {@link GxsTunnelRsClient#onGxsTunnelStatusChanged(Location, GxsId, GxsTunnelStatus)}  method will be received.
	 * Data can then be sent and received in the tunnel. A same tunnel can be used by several clients, hence they're differentiated
	 * by the serviceId parameter.
	 *
	 * @param from the originating identity
	 * @param to the destination identity
	 * @param serviceId the service id
	 * @return a tunnel id
	 */
	public Location requestSecuredTunnel(GxsId from, GxsId to, int serviceId)
	{
		var hash = DestinationHash.createRandomHash(to);
		var tunnelId = VirtualLocation.fromGxsIds(from, to);

		log.debug("Requesting secured tunnel for gxs id {}, resulting tunnel id: {}", to, tunnelId);

		if (contacts.putIfAbsent(tunnelId, new TunnelPeerInfo(hash, to, serviceId)) != null)
		{
			log.error("Tunnel {} already exists", tunnelId);
			return null;
		}

		turtleRouter.startMonitoringTunnels(hash, this, false);

		return tunnelId;
	}

	/**
	 * Gets the destination GxS identity from a tunnel.
	 *
	 * @param tunnelId the tunnel id
	 * @return the identity
	 */
	public GxsId getGxsFromTunnel(Location tunnelId)
	{
		var tunnelPeerInfo = contacts.get(tunnelId);
		if (tunnelPeerInfo == null)
		{
			return null;
		}
		return tunnelPeerInfo.getDestination();
	}

	/**
	 * Sends data through the tunnel. If a tunnel is present, retries are performed automatically until the reception is acknowledged by the other end.
	 *
	 * @param tunnelId  the tunnel id
	 * @param serviceId the service id
	 * @param data      the data
	 * @return true if successful, false if the tunnel doesn't exist
	 */
	public boolean sendData(Location tunnelId, int serviceId, byte[] data)
	{
		var tunnelPeerInfo = contacts.get(tunnelId);
		if (tunnelPeerInfo == null)
		{
			log.error("No tunnel peer info found for {}", tunnelId);
			return false;
		}

		var client = clients.get(serviceId);
		if (client == null)
		{
			log.error("Cannot find client for {}", serviceId);
			return false;
		}
		sendTunnelDataItem(tunnelId, new GxsTunnelDataItem(getUniquePacketCounter(), serviceId, data));
		return true;
	}

	/**
	 * Closes and established tunnel. All further data will be refused but the tunnel will be kept alive for a little
	 * while until all pending data is delivered. Clients will receive a {@link GxsTunnelRsClient#onGxsTunnelStatusChanged(Location, GxsId, GxsTunnelStatus)} method
	 * once the tunnel gets closed.
	 *
	 * @param tunnelId the tunnel id
	 * @param serviceId the service id
	 */
	public void closeExistingTunnel(Location tunnelId, int serviceId)
	{
		var tunnelPeerInfo = contacts.get(tunnelId);
		if (tunnelPeerInfo == null)
		{
			log.error("Cannot close distant tunnel connection. No connection opened for tunnel id {}", tunnelId);
			return;
		}

		if (tunnelPeerInfo.getLocation() == null)
		{
			log.warn("Connection already closed for tunnel id {}", tunnelId);
			return;
		}

		Sha1Sum hash;

		var tunnelDhInfo = dhPeers.get(tunnelPeerInfo.getLocation());
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
			log.debug("Sending close tunnel status to tunnel id {}", tunnelId);
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
