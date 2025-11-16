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

package io.xeres.app.xrs.service.board;

import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.board.item.BoardGroupItem;
import io.xeres.app.xrs.service.board.item.BoardMessageItem;
import io.xeres.app.xrs.service.gxs.AuthenticationRequirements;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.app.xrs.service.gxs.GxsUpdateService;
import io.xeres.app.xrs.service.identity.IdentityManager;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.xeres.app.xrs.service.RsServiceType.POSTED;
import static io.xeres.app.xrs.service.gxs.AuthenticationRequirements.Flags.*;

@Component
public class BoardRsService extends GxsRsService<BoardGroupItem, BoardMessageItem>
{
	public BoardRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, GxsTransactionManager gxsTransactionManager, DatabaseSessionManager databaseSessionManager, IdentityManager identityManager, GxsUpdateService<BoardGroupItem, BoardMessageItem> gxsUpdateService)
	{
		super(rsServiceRegistry, peerConnectionManager, gxsTransactionManager, databaseSessionManager, identityManager, gxsUpdateService);
	}

	@Override
	public RsServiceType getServiceType()
	{
		return POSTED;
	}

	@Override
	protected AuthenticationRequirements getAuthenticationRequirements()
	{
		return new AuthenticationRequirements.Builder()
				.withPublic(EnumSet.of(ROOT_AUTHOR, CHILD_AUTHOR))
				.withRestricted(EnumSet.of(ROOT_PUBLISH, CHILD_PUBLISH))
				.withPrivate(EnumSet.of(ROOT_PUBLISH, CHILD_PUBLISH))
				.build();
	}

	// XXX: add initialize()...

	@Override
	protected void syncMessages(PeerConnection recipient)
	{

	}

	@Override
	protected List<BoardGroupItem> onAvailableGroupListRequest(PeerConnection recipient, Instant since)
	{
		return List.of();
	}

	@Override
	protected Set<GxsId> onAvailableGroupListResponse(Map<GxsId, Instant> ids)
	{
		return Set.of();
	}

	@Override
	protected List<BoardGroupItem> onGroupListRequest(Set<GxsId> ids)
	{
		return List.of();
	}

	@Override
	protected boolean onGroupReceived(BoardGroupItem item)
	{
		return false;
	}

	@Override
	protected void onGroupsSaved(List<BoardGroupItem> items)
	{

	}

	@Override
	protected List<BoardMessageItem> onPendingMessageListRequest(PeerConnection recipient, GxsId groupId, Instant since)
	{
		return List.of();
	}

	@Override
	protected List<BoardMessageItem> onMessageListRequest(GxsId groupId, Set<MessageId> messageIds)
	{
		return List.of();
	}

	@Override
	protected List<MessageId> onMessageListResponse(GxsId groupId, Set<MessageId> messageIds)
	{
		return List.of();
	}

	@Override
	protected boolean onMessageReceived(BoardMessageItem item)
	{
		return false;
	}

	@Override
	protected void onMessagesSaved(List<BoardMessageItem> items)
	{

	}
}
