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
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.ForumService;
import io.xeres.app.service.GxsExchangeService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.forum.item.ForumGroupItem;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.app.xrs.service.gxs.GxsRsService;
import io.xeres.app.xrs.service.gxs.GxsTransactionManager;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.xeres.app.xrs.service.RsServiceType.FORUMS;

@Component
public class ForumRsService extends GxsRsService<ForumGroupItem, ForumMessageItem>
{
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
	protected List<ForumGroupItem> onAvailableGroupListRequest(PeerConnection recipient, Instant since)
	{
		return forumService.findAllGroupsSubscribedAndPublishedSince(since);
	}

	@Override
	protected List<ForumGroupItem> onGroupListRequest(Set<GxsId> ids)
	{
		return forumService.findAllGroups(ids);
	}

	@Override
	protected Set<GxsId> onAvailableGroupListResponse(Map<GxsId, Instant> ids)
	{
		// We want new forums as well as updated ones
		var existingMap = forumService.findAllGroups(ids.keySet()).stream()
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, forumGroupItem -> forumGroupItem.getPublished().truncatedTo(ChronoUnit.SECONDS)));

		ids.entrySet().removeIf(gxsIdInstantEntry -> {
			var existing = existingMap.get(gxsIdInstantEntry.getKey());
			return existing != null && !gxsIdInstantEntry.getValue().isAfter(existing);
		});
		return ids.keySet();
	}

	@Override
	protected void onGroupReceived(ForumGroupItem item)
	{
		log.debug("Received group {}, saving/updating...", item);
		forumService.save(item);
	}

	@Override
	protected List<ForumMessageItem> onPendingMessageListRequest(PeerConnection recipient, GxsId groupId, Instant since)
	{
		return forumService.findAllMessagesInGroupSince(groupId, since);
	}

	@Override
	protected List<ForumMessageItem> onMessageListRequest(GxsId groupId, Set<MessageId> messageIds)
	{
		return forumService.findAllMessages(groupId, messageIds);
	}

	@Override
	protected List<MessageId> onMessageListResponse(GxsId groupId, Set<MessageId> messageIds)
	{
		var existing = forumService.findAllMessages(groupId, messageIds).stream()
						.map(GxsMessageItem::getMessageId)
						.collect(Collectors.toSet());

		messageIds.removeIf(existing::contains);

		return messageIds.stream().toList();
	}

	@Override
	protected void onMessageReceived(ForumMessageItem item)
	{
		log.debug("Received message {}, saving...", item);
		forumService.save(item);
	}

	@Transactional
	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		super.handleItem(sender, item); // This is required for the @Transactional to work
	}
}
