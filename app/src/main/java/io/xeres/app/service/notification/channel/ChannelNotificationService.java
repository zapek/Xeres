/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.service.notification.channel;

import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.UnHtmlService;
import io.xeres.app.service.notification.NotificationService;
import io.xeres.app.xrs.service.channel.ChannelRsService;
import io.xeres.app.xrs.service.channel.item.ChannelGroupItem;
import io.xeres.app.xrs.service.channel.item.ChannelMessageItem;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import io.xeres.common.rest.notification.channel.AddChannelMessages;
import io.xeres.common.rest.notification.channel.AddOrUpdateChannelGroups;
import io.xeres.common.rest.notification.channel.ChannelNotification;
import io.xeres.common.rest.notification.channel.MarkChannelMessagesAsRead;
import org.apache.commons.collections4.SetUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.xeres.app.database.model.channel.ChannelMapper.toChannelMessageDTOs;
import static io.xeres.app.database.model.channel.ChannelMapper.toDTOs;

@Service
public class ChannelNotificationService extends NotificationService
{
	private final ChannelRsService channelRsService;
	private final IdentityService identityService;
	private final UnHtmlService unHtmlService;

	public ChannelNotificationService(@Lazy ChannelRsService channelRsService, IdentityService identityService, UnHtmlService unHtmlService)
	{
		this.channelRsService = channelRsService;
		this.identityService = identityService;
		this.unHtmlService = unHtmlService;
	}

	public void addOrUpdateChannelGroups(List<ChannelGroupItem> channelGroups)
	{
		var action = new AddOrUpdateChannelGroups(toDTOs(channelGroups));
		sendNotification(new ChannelNotification(action.getClass().getSimpleName(), action));
	}

	public void addOrUpdateChannelMessages(List<ChannelMessageItem> channelMessages)
	{
		var action = new AddChannelMessages(toChannelMessageDTOs(unHtmlService, channelMessages,
				getAuthorsMapFromMessages(channelMessages),
				getMessagesMapFromMessages(channelMessages),
				false));

		sendNotification(new ChannelNotification(action.getClass().getSimpleName(), action));
	}

	public void markChannelMessagesAsRead(Map<Long, Boolean> messageMap)
	{
		var action = new MarkChannelMessagesAsRead(messageMap);
		sendNotification(new ChannelNotification(action.getClass().getSimpleName(), action));
	}

	private Map<GxsId, IdentityGroupItem> getAuthorsMapFromMessages(List<ChannelMessageItem> channelMessages)
	{
		var authors = channelMessages.stream()
				.map(ChannelMessageItem::getAuthorId)
				.collect(Collectors.toSet());

		return identityService.findAll(authors).stream()
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, Function.identity()));
	}

	private Map<MessageId, ChannelMessageItem> getMessagesMapFromMessages(List<ChannelMessageItem> channelMessages)
	{
		var messageIds = channelMessages.stream()
				.map(ChannelMessageItem::getMessageId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		var parentIds = channelMessages.stream()
				.map(ChannelMessageItem::getParentId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		return channelRsService.findAllMessages(SetUtils.union(messageIds, parentIds)).stream()
				.collect(Collectors.toMap(ChannelMessageItem::getMessageId, Function.identity()));
	}
}
