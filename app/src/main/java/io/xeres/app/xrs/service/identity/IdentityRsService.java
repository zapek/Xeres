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

import io.netty.buffer.Unpooled;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.GxsExchangeService;
import io.xeres.app.service.IdentityService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.app.xrs.service.gxs.item.GxsTransferGroupItem;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.xeres.app.xrs.service.RsServiceType.GXSID;

@Component
public class IdentityRsService extends GxsRsService
{
	private static final Logger log = LoggerFactory.getLogger(IdentityRsService.class);

	private final IdentityService identityService;

	public IdentityRsService(Environment environment, PeerConnectionManager peerConnectionManager, GxsExchangeService gxsExchangeService, GxsTransactionManager gxsTransactionManager, IdentityService identityService)
	{
		super(environment, peerConnectionManager, gxsExchangeService, gxsTransactionManager);
		this.identityService = identityService;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return GXSID;
	}

	@Transactional
	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		super.handleItem(sender, item); // This is required for the @Transactional to work
	}

	@Override
	public List<? extends GxsGroupItem> getPendingGroups(PeerConnection recipient, Instant since)
	{
		return identityService.findAllPublishedSince(since);
	}

	@Override
	protected List<? extends GxsGroupItem> onGroupListRequest(Set<GxsId> ids)
	{
		return identityService.findAll(ids);
	}

	@Override
	protected void onGroupReceived(GxsTransferGroupItem item)
	{
		log.debug("Saving id {}", item.getGroupId());

		var buf = Unpooled.copiedBuffer(item.getMeta(), item.getGroup()); //XXX: use ctx().alloc()?
		var gxsIdGroupItem = new IdentityGroupItem();
		Serializer.deserializeGxsGroupItem(buf, gxsIdGroupItem);
		buf.release();

		identityService.transferIdentity(gxsIdGroupItem);
	}

	@Override
	protected Set<GxsId> onGroupListResponse(Map<GxsId, Instant> ids)
	{
		// From the received list, we keep:
		// 1) all identities that we don't already have
		// 2) all identities that have a more recent publishing date than what we have
		var existingMap = identityService.findAll(ids.keySet()).stream()
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, identityGroupItem -> identityGroupItem.getPublished().truncatedTo(ChronoUnit.SECONDS)));

		ids.entrySet().removeIf(gxsIdInstantEntry -> {
			var existing = existingMap.get(gxsIdInstantEntry.getKey());
			return existing != null && !gxsIdInstantEntry.getValue().isAfter(existing);
		});
		return ids.keySet();
	}
}
