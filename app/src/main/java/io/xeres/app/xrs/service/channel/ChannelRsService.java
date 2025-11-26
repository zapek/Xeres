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

package io.xeres.app.xrs.service.channel;

import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.common.CommentMessageItem;
import io.xeres.app.xrs.common.VoteMessageItem;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.channel.item.ChannelGroupItem;
import io.xeres.app.xrs.service.channel.item.ChannelMessageItem;
import io.xeres.app.xrs.service.gxs.AuthenticationRequirements;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.app.xrs.service.gxs.GxsUpdateService;
import io.xeres.app.xrs.service.identity.IdentityManager;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.xeres.app.xrs.service.RsServiceType.CHANNELS;
import static io.xeres.app.xrs.service.gxs.AuthenticationRequirements.Flags.*;

@Component
public class ChannelRsService extends GxsRsService<ChannelGroupItem, ChannelMessageItem>
{
	private static final int IMAGE_GROUP_WIDTH = 64; // XXX: to verify..
	private static final int IMAGE_GROUP_HEIGHT = 64;

	private static final Duration SYNCHRONIZATION_INITIAL_DELAY = Duration.ofSeconds(90);
	private static final Duration SYNCHRONIZATION_DELAY = Duration.ofMinutes(1);

	public ChannelRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, GxsTransactionManager gxsTransactionManager, DatabaseSessionManager databaseSessionManager, IdentityManager identityManager, GxsUpdateService<ChannelGroupItem, ChannelMessageItem> gxsUpdateService)
	{
		super(rsServiceRegistry, peerConnectionManager, gxsTransactionManager, databaseSessionManager, identityManager, gxsUpdateService);
		// XXX
	}


	// XXX: don't forget about the comments and votes!

	// XXX: other users cannot write messages on a channel we own

	@Override
	public RsServiceType getServiceType()
	{
		return CHANNELS;
	}

	@Override
	protected AuthenticationRequirements getAuthenticationRequirements()
	{
		return new AuthenticationRequirements.Builder()
				.withPublic(EnumSet.of(ROOT_PUBLISH, CHILD_AUTHOR))
				.withRestricted(EnumSet.of(ROOT_PUBLISH, CHILD_AUTHOR, CHILD_PUBLISH))
				.withPrivate(EnumSet.of(ROOT_PUBLISH, CHILD_AUTHOR, CHILD_PUBLISH))
				.build();
	}

	@Override
	protected void syncMessages(PeerConnection recipient)
	{

	}

	@Override
	protected List<ChannelGroupItem> onAvailableGroupListRequest(PeerConnection recipient, Instant since)
	{
		return List.of();
	}

	@Override
	protected Set<GxsId> onAvailableGroupListResponse(Map<GxsId, Instant> ids)
	{
		return Set.of();
	}

	@Override
	protected List<ChannelGroupItem> onGroupListRequest(Set<GxsId> ids)
	{
		return List.of();
	}

	@Override
	protected boolean onGroupReceived(ChannelGroupItem item)
	{
		return false;
	}

	@Override
	protected void onGroupsSaved(List<ChannelGroupItem> items)
	{

	}

	@Override
	protected List<ChannelMessageItem> onPendingMessageListRequest(PeerConnection recipient, GxsId groupId, Instant since)
	{
		return List.of();
	}

	@Override
	protected List<ChannelMessageItem> onMessageListRequest(GxsId groupId, Set<MessageId> messageIds)
	{
		return List.of();
	}

	@Override
	protected List<MessageId> onMessageListResponse(GxsId groupId, Set<MessageId> messageIds)
	{
		return List.of();
	}

	@Override
	protected boolean onMessageReceived(ChannelMessageItem item)
	{
		return false;
	}

	@Override
	protected void onMessagesSaved(List<ChannelMessageItem> items)
	{

	}

	@Override
	protected boolean onCommentReceived(CommentMessageItem item)
	{
		return false;
	}

	@Override
	protected void onCommentsSaved(List<CommentMessageItem> items)
	{

	}

	@Override
	protected boolean onVoteReceived(VoteMessageItem item)
	{
		return false;
	}

	@Override
	protected void onVotesSaved(List<VoteMessageItem> items)
	{

	}

}
