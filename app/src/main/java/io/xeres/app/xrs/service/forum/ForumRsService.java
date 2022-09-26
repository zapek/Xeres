/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.GxsExchangeService;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.forum.item.ForumGroupItem;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.app.xrs.service.gxs.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

import static io.xeres.app.xrs.service.RsServiceType.FORUMS;

@Component
public class ForumRsService extends GxsRsService
{
	private static final Logger log = LoggerFactory.getLogger(ForumRsService.class);

	public ForumRsService(Environment environment, PeerConnectionManager peerConnectionManager, GxsExchangeService gxsExchangeService, GxsTransactionManager gxsTransactionManager, DatabaseSessionManager databaseSessionManager)
	{
		super(environment, peerConnectionManager, gxsExchangeService, gxsTransactionManager, databaseSessionManager);
	}

	@Override
	public Class<? extends GxsGroupItem> getGroupClass()
	{
		return ForumGroupItem.class;
	}

	@Override
	public Class<? extends GxsMessageItem> getMessageClass()
	{
		return null; // XXX: we DO use messages
	}

	@Override
	public RsServiceType getServiceType()
	{
		return FORUMS;
	}

	@Override
	public List<GxsGroupItem> getPendingGroups(PeerConnection recipient, Instant since)
	{
		return null;
	}

	@Override
	public void processItems(PeerConnection peerConnection, Transaction<?> transaction)
	{

	}
}
