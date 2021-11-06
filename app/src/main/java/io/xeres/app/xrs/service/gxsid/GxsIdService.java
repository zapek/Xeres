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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.database.model.identity.Identity;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.GxsExchangeService;
import io.xeres.app.service.IdentityService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxs.GxsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.app.xrs.service.gxs.item.GxsExchange;
import io.xeres.app.xrs.service.gxs.item.GxsSyncGroupItem;
import io.xeres.app.xrs.service.gxs.item.GxsTransferGroupItem;
import io.xeres.app.xrs.service.gxs.item.SyncFlags;
import io.xeres.app.xrs.service.gxsid.item.GxsIdGroupItem;
import io.xeres.common.id.GxsId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static io.xeres.app.xrs.service.RsServiceType.GXSID;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

@Component
public class GxsIdService extends GxsService
{
	private static final Logger log = LoggerFactory.getLogger(GxsIdService.class);

	private final IdentityService identityService;
	private final DatabaseSessionManager databaseSessionManager;

	public GxsIdService(Environment environment, PeerConnectionManager peerConnectionManager, GxsExchangeService gxsExchangeService, GxsTransactionManager gxsTransactionManager, DatabaseSessionManager databaseSessionManager, IdentityService identityService)
	{
		super(environment, peerConnectionManager, gxsExchangeService, gxsTransactionManager, databaseSessionManager);
		this.identityService = identityService;
		this.databaseSessionManager = databaseSessionManager;
	}

	@Override
	public Class<? extends GxsGroupItem> getGroupClass()
	{
		return GxsIdGroupItem.class;
	}

	@Override
	public Class<? extends GxsMessageItem> getMessageClass()
	{
		return null; // We don't use messages
	}

	@Override
	public RsServiceType getServiceType()
	{
		return GXSID;
	}

	// XXX: for now only GxsService handles the items... GxsIdGroupItem is not used I think (it has the same subtype as GxsSyncGroupItem which means they clash). also disabled handleItem() below
	@Override
	public Map<Class<? extends Item>, Integer> getSupportedItems()
	{
		return super.getSupportedItems();
//		return Stream.concat(Map.of(
//				GxsIdGroupItem.class, 2
//		).entrySet().stream(), super.getSupportedItems().entrySet().stream())
//				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public List<? extends GxsGroupItem> getGroups(PeerConnection peerConnection, Instant since)
	{
		// XXX: use identityService to return the identities we need. for now we just return ours
		return List.of(identityService.getOwnIdentity().getGxsIdGroupItem());
	}

	@Override
	public void handleItem(PeerConnection peerConnection, Item item)
	{
//		if (item instanceof GxsIdGroupItem gxsIdGroupItem)
//		{
//			handleGxsIdGroupItem(peerConnection, gxsIdGroupItem);
//		}
		super.handleItem(peerConnection, item);
	}

	private void handleGxsIdGroupItem(PeerConnection peerConnection, GxsIdGroupItem item)
	{
		log.debug("got item: {}", item);
		// XXX: I think those only exist when doing a transfer between distant peers
	}

	@Override
	public void processItems(PeerConnection peerConnection, List<? extends GxsExchange> items)
	{
		if (isEmpty(items))
		{
			throw new IllegalArgumentException("Empty transaction items");
		}

		if (items.get(0) instanceof GxsSyncGroupItem)
		{
			@SuppressWarnings("unchecked")
			var gxsIds = ((List<GxsSyncGroupItem>) items).stream().map(GxsSyncGroupItem::getGroupId).toList();
			log.debug("Peer wants the following gxs ids: {}", gxsIds);
			// XXX: for now we just send back our own identity. we should ideally just send back the identities that we have that match the request (there can be more than 1 of course)
			try (var session = new DatabaseSession(databaseSessionManager))
			{
				Identity ownIdentity = identityService.getOwnIdentity();
				if (gxsIds.size() == 1 && gxsIds.get(0).equals(ownIdentity.getGxsIdGroupItem().getGxsId()))
				{
					sendGxsGroups(peerConnection, List.of(ownIdentity.getGxsIdGroupItem()));
				}
				else
				{
					// Requested an ID that we don't have
				}
			}
		}
		else if (items.get(0) instanceof GxsTransferGroupItem)
		{
			@SuppressWarnings("unchecked")
			var transferItems = (List<GxsTransferGroupItem>) items;
			transferItems.forEach(item -> {
				log.debug("Saving id {}", item.getGroupId());

				var buf = Unpooled.copiedBuffer(item.getMeta(), item.getGroup()); //XXX: use ctx().alloc()?
				var gxsIdGroupItem = new GxsIdGroupItem();
				((RsSerializable) gxsIdGroupItem).readObject(buf, EnumSet.noneOf(SerializationFlags.class)); // XXX: should we add some helper method into Serializer()?
				buf.release();

				var identity = Identity.createIdentity(gxsIdGroupItem); // XXX: find out if it's a friend. how?
				identityService.saveIdentity(identity);
			});
		}
	}

	// XXX: maybe this should be in GxsService. also other methods I think (though there are gxsIds... hmm... what a mess)
	public void requestGxsGroups(PeerConnection peerConnection, List<GxsId> ids) // XXX: maybe use a future to know when the group arrived? it's possible by keeping a list of transactionIds then answering once the answer comes back
	{
		int transactionId = getTransactionId(peerConnection);
		List<GxsExchange> items = new ArrayList<>();

		ids.forEach(gxsId -> items.add(new GxsSyncGroupItem(EnumSet.of(SyncFlags.REQUEST), gxsId, transactionId)));

		gxsTransactionManager.startOutgoingTransactionRequest(peerConnection, items, transactionId, this);
	}

	public void sendGxsGroups(PeerConnection peerConnection, List<GxsGroupItem> gxsGroupItems)
	{
		int transactionId = getTransactionId(peerConnection);
		List<GxsExchange> items = new ArrayList<>();
		gxsGroupItems.forEach(gxsGroupItem -> {
			signGroup(gxsGroupItem);
			var groupBuf = Unpooled.buffer(); // XXX: size... well, it autogrows
			log.debug("Writing group buf");
			gxsGroupItem.writeObject(groupBuf, EnumSet.of(SerializationFlags.SUBCLASS_ONLY));
			var metaBuf = Unpooled.buffer(); // XXX: size... autogrows as well
			log.debug("Writing meta buf");
			gxsGroupItem.writeObject(metaBuf, EnumSet.of(SerializationFlags.SUPERCLASS_ONLY));
			var gxsTransferGroupItem = new GxsTransferGroupItem(gxsGroupItem.getGxsId(), getArray(groupBuf), getArray(metaBuf), transactionId);
			gxsTransferGroupItem.setService(this); // XXX: maybe move that on the constructor? since it's kinda needed
			items.add(gxsTransferGroupItem);
		});

		gxsTransactionManager.startOutgoingTransaction(
				peerConnection,
				items,
				Instant.now(), // XXX: not sure about that one... recheck
				transactionId,
				this
		);
	}

	private static byte[] getArray(ByteBuf buf)
	{
		var out = new byte[buf.writerIndex()];
		buf.readBytes(out);
		return out;
	}

	private static void signGroup(GxsGroupItem gxsGroupItem)
	{
		var data = serializeItemForSignature(gxsGroupItem);
		byte[] signature;
		try
		{
			signature = RSA.sign(data, RSA.getPrivateKey(gxsGroupItem.getAdminPrivateKeyData()));
		}
		catch (NoSuchAlgorithmException | InvalidKeySpecException e)
		{
			throw new IllegalArgumentException("Error in private key: " + e.getMessage());
		}
		gxsGroupItem.setSignature(signature);
	}

	private static byte[] serializeItemForSignature(Item item)
	{
		item.setOutgoing(Unpooled.buffer().alloc(), 2, RsServiceType.GXSID, 1); // XXX: not very nice to have those arguments hardcoded here
		var buf = item.serializeItem(EnumSet.of(SerializationFlags.SIGNATURE)).getBuffer();
		var data = new byte[buf.writerIndex()];
		buf.getBytes(0, data);
		buf.release();
		return data;
	}
}
