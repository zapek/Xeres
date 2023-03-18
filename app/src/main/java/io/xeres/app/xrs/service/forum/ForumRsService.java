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

package io.xeres.app.xrs.service.forum;

import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.ForumService;
import io.xeres.app.service.GxsExchangeService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.app.xrs.service.gxs.item.GxsTransferGroupItem;
import io.xeres.common.id.GxsId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.xeres.app.xrs.service.RsServiceType.FORUMS;

@Component
public class ForumRsService extends GxsRsService
{
	private static final Logger log = LoggerFactory.getLogger(ForumRsService.class);

	private final ForumService forumService;

	public ForumRsService(Environment environment, PeerConnectionManager peerConnectionManager, GxsExchangeService gxsExchangeService, GxsTransactionManager gxsTransactionManager, ForumService forumService)
	{
		super(environment, peerConnectionManager, gxsExchangeService, gxsTransactionManager);
		this.forumService = forumService;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return FORUMS;
	}

	@Override
	public List<? extends GxsGroupItem> getPendingGroups(PeerConnection recipient, Instant since)
	{
		return forumService.findAllSubscribedAndPublishedSince(since);
	}

	@Override
	protected List<? extends GxsGroupItem> onGroupListRequest(Set<GxsId> ids)
	{
		return forumService.findAll(ids);
	}

	@Override
	protected Set<GxsId> onGroupListResponse(Map<GxsId, Instant> ids)
	{
		// XXX: return the groups that we are subscribed and that are more recent
		log.debug("Received list response: {}", ids);
		return Collections.emptySet();
	}

	@Override
	protected void onGroupReceived(GxsTransferGroupItem item)
	{
		// XXX: save/update in database
		log.debug("Received group {}", item);
	}

	@Transactional
	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		super.handleItem(sender, item); // This is required for the @Transactional to work
	}
}
