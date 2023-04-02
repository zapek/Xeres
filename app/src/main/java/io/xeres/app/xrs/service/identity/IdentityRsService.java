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

import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.GxsExchangeService;
import io.xeres.app.service.IdentityService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.xeres.app.xrs.service.RsServiceType.GXSID;

@Component
public class IdentityRsService extends GxsRsService<IdentityGroupItem, GxsMessageItem>
{
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
	protected List<IdentityGroupItem> onPendingGroupListRequest(PeerConnection recipient, Instant since)
	{
		return identityService.findAllSubscribedAndPublishedSince(since);
	}

	@Override
	protected Set<GxsId> onGroupListResponse(Map<GxsId, Instant> ids)
	{
		// From the received list, we keep all identities that have a more recent publishing date than those
		// we already have. If it's a new identity, we don't want it.
		var existingMap = identityService.findAll(ids.keySet()).stream()
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, identityGroupItem -> identityGroupItem.getPublished().truncatedTo(ChronoUnit.SECONDS)));

		ids.entrySet().removeIf(gxsIdInstantEntry -> {
			var existing = existingMap.get(gxsIdInstantEntry.getKey());
			return existing == null || !gxsIdInstantEntry.getValue().isAfter(existing);
		});
		return ids.keySet();
	}

	@Override
	protected List<IdentityGroupItem> onGroupListRequest(Set<GxsId> ids)
	{
		return identityService.findAll(ids);
	}

	@Override
	protected void onGroupReceived(IdentityGroupItem item)
	{
		log.debug("Saving id {}", item.getGxsId());
		identityService.transferIdentity(item);
	}

	@Override
	protected List<GxsMessageItem> onPendingMessageListRequest(PeerConnection recipient, GxsId groupId, Instant since)
	{
		return Collections.emptyList(); // unused
	}
}
