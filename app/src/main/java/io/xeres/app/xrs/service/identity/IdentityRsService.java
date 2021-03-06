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

package io.xeres.app.xrs.service.identity;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.GxsExchangeService;
import io.xeres.app.service.IdentityService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.app.xrs.service.gxs.Transaction;
import io.xeres.app.xrs.service.gxs.item.GxsSyncGroupItem;
import io.xeres.app.xrs.service.gxs.item.GxsTransferGroupItem;
import io.xeres.app.xrs.service.gxs.item.SyncFlags;
import io.xeres.app.xrs.service.gxs.item.TransactionFlags;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static io.xeres.app.xrs.service.RsServiceType.GXSID;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Component
public class IdentityRsService extends GxsRsService
{
	private static final Logger log = LoggerFactory.getLogger(IdentityRsService.class);

	private final IdentityService identityService;
	private final GxsExchangeService gxsExchangeService;
	private final DatabaseSessionManager databaseSessionManager;

	public IdentityRsService(Environment environment, PeerConnectionManager peerConnectionManager, GxsExchangeService gxsExchangeService, GxsTransactionManager gxsTransactionManager, DatabaseSessionManager databaseSessionManager, IdentityService identityService)
	{
		super(environment, peerConnectionManager, gxsExchangeService, gxsTransactionManager, databaseSessionManager);
		this.identityService = identityService;
		this.databaseSessionManager = databaseSessionManager;
		this.gxsExchangeService = gxsExchangeService;
	}

	@Override
	public Class<? extends GxsGroupItem> getGroupClass()
	{
		return IdentityGroupItem.class;
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
	public List<GxsGroupItem> getPendingGroups(PeerConnection recipient, Instant since)
	{
		// XXX: use identityService to return the identities we need. for now, we just return ours
		return List.of(identityService.getOwnIdentity());
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
//		if (item instanceof GxsIdGroupItem gxsIdGroupItem)
//		{
//			handleGxsIdGroupItem(peerConnection, gxsIdGroupItem);
//		}
		super.handleItem(sender, item);
	}

	private void handleGxsIdGroupItem(PeerConnection peerConnection, IdentityGroupItem item)
	{
		log.debug("got item: {}", item);
		// XXX: I think those only exist when doing a transfer between distant peers
	}

	@Override
	public void processItems(PeerConnection peerConnection, Transaction<?> transaction)
	{
		if (isEmpty(transaction.getItems()))
		{
			throw new IllegalArgumentException("Empty transaction items");
		}

		if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_GROUP_LIST_REQUEST))
		{
			@SuppressWarnings("unchecked")
			var gxsIds = ((List<GxsSyncGroupItem>) transaction.getItems()).stream().map(GxsSyncGroupItem::getGroupId).collect(toSet());
			log.debug("Peer wants the following gxs ids (total: {}): {} ...", gxsIds.size(), gxsIds.stream().limit(10).toList());
			try (var ignored = new DatabaseSession(databaseSessionManager))
			{
				sendGxsGroups(peerConnection, identityService.findAll(gxsIds));
			}
		}
		else if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_GROUP_LIST_RESPONSE))
		{
			@SuppressWarnings("unchecked")
			var gxsIdsMap = ((List<GxsSyncGroupItem>) transaction.getItems()).stream()
					.collect(toMap(GxsSyncGroupItem::getGroupId, gxsSyncGroupItem -> Instant.ofEpochSecond(gxsSyncGroupItem.getPublishTimestamp())));
			log.debug("Peer has the following gxsIds (new or updates) for us (total: {}): {} ...", gxsIdsMap.keySet().size(), gxsIdsMap.keySet().stream().limit(10).toList());
			try (var ignored = new DatabaseSession(databaseSessionManager))
			{
				// From the received list, we keep:
				// 1) all identities that we don't already have
				// 2) all identities that have a more recent publishing date than what we have
				var existingMap = identityService.findAll(gxsIdsMap.keySet()).stream()
						.collect(Collectors.toMap(GxsGroupItem::getGxsId, identityGroupItem -> identityGroupItem.getPublished().truncatedTo(ChronoUnit.SECONDS)));

				gxsIdsMap.entrySet().removeIf(gxsIdInstantEntry -> {
					var existing = existingMap.get(gxsIdInstantEntry.getKey());
					return existing != null && !gxsIdInstantEntry.getValue().isAfter(existing);
				});
			}
			requestGxsGroups(peerConnection, gxsIdsMap.keySet());
		}
		else if (transaction.getTransactionFlags().contains(TransactionFlags.TYPE_GROUPS))
		{
			@SuppressWarnings("unchecked")
			var transferItems = (List<GxsTransferGroupItem>) transaction.getItems();
			transferItems.forEach(item -> {
				log.debug("Saving id {}", item.getGroupId());

				var buf = Unpooled.copiedBuffer(item.getMeta(), item.getGroup()); //XXX: use ctx().alloc()?
				var gxsIdGroupItem = new IdentityGroupItem();
				((RsSerializable) gxsIdGroupItem).readObject(buf, EnumSet.noneOf(SerializationFlags.class)); // XXX: should we add some helper method into Serializer()?
				buf.release();

				identityService.transferIdentity(gxsIdGroupItem);
			});
			gxsExchangeService.setLastServiceUpdate(RsServiceType.GXSID, Instant.now());
		}
	}

	// XXX: maybe this should be in GxsService. also other methods I think (though there are gxsIds... hmm... what a mess)
	void requestGxsGroups(PeerConnection peerConnection, Collection<GxsId> ids) // XXX: maybe use a future to know when the group arrived? it's possible by keeping a list of transactionIds then answering once the answer comes back
	{
		if (isEmpty(ids))
		{
			return;
		}
		var transactionId = getTransactionId(peerConnection);
		List<GxsSyncGroupItem> items = new ArrayList<>();

		ids.forEach(gxsId -> items.add(new GxsSyncGroupItem(EnumSet.of(SyncFlags.REQUEST), gxsId, transactionId)));

		gxsTransactionManager.startOutgoingTransactionForGroupIdRequest(peerConnection, items, transactionId, this);
	}

	public void sendGxsGroups(PeerConnection peerConnection, List<? extends GxsGroupItem> gxsGroupItems)
	{
		var transactionId = getTransactionId(peerConnection);
		List<GxsTransferGroupItem> items = new ArrayList<>();
		gxsGroupItems.forEach(gxsGroupItem -> {
			signGroupIfNeeded(gxsGroupItem);
			var groupBuf = Unpooled.buffer(); // XXX: size... well, it autogrows
			gxsGroupItem.writeObject(groupBuf, EnumSet.of(SerializationFlags.SUBCLASS_ONLY));
			var metaBuf = Unpooled.buffer(); // XXX: size... autogrows as well
			gxsGroupItem.writeObject(metaBuf, EnumSet.of(SerializationFlags.SUPERCLASS_ONLY));
			var gxsTransferGroupItem = new GxsTransferGroupItem(gxsGroupItem.getGxsId(), getArray(groupBuf), getArray(metaBuf), transactionId, this);
			groupBuf.release();
			metaBuf.release();
			items.add(gxsTransferGroupItem);
		});

		if (isNotEmpty(items))
		{
			gxsTransactionManager.startOutgoingTransactionForGroupTransfer(
					peerConnection,
					items,
					Instant.now(), // XXX: not sure about that one... recheck. I think it has to be when our group last changed
					transactionId,
					this
			);
		}
	}

	private static byte[] getArray(ByteBuf buf)
	{
		var out = new byte[buf.writerIndex()];
		buf.readBytes(out);
		return out;
	}

	// XXX: GXS messages will need publish/identity support here, not just admin
	private static void signGroupIfNeeded(GxsGroupItem gxsGroupItem)
	{
		if (gxsGroupItem.getAdminPrivateKey() != null)
		{
			var data = serializeItemForSignature(gxsGroupItem);
			var signature = RSA.sign(data, gxsGroupItem.getAdminPrivateKey());
			gxsGroupItem.setSignature(signature);
		}
	}

	private static byte[] serializeItemForSignature(Item item)
	{
		item.setSerialization(Unpooled.buffer().alloc(), 2, RsServiceType.GXSID, 1); // XXX: not very nice to have those arguments hardcoded here
		var buf = item.serializeItem(EnumSet.of(SerializationFlags.SIGNATURE)).getBuffer();
		var data = new byte[buf.writerIndex()];
		buf.getBytes(0, data);
		buf.release();
		return data;
	}
}
