/*
 * Copyright (c) 2023-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.service;

import io.xeres.app.database.model.forum.ForumMessageItemSummary;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.xrs.service.forum.ForumRsService;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MsgId;
import org.apache.commons.collections4.SetUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Message helper service because they're hard to retrieve otherwise.
 */
@Service
public class ForumMessageService
{
	private final ForumRsService forumRsService;
	private final IdentityService identityService;

	public ForumMessageService(@Lazy ForumRsService forumRsService, IdentityService identityService)
	{
		this.forumRsService = forumRsService;
		this.identityService = identityService;
	}

	public Map<GxsId, IdentityGroupItem> getAuthorsMapFromSummaries(Page<ForumMessageItemSummary> forumMessages)
	{
		var authors = forumMessages.stream()
				.map(ForumMessageItemSummary::getAuthorGxsId)
				.collect(Collectors.toSet());

		return identityService.findAll(authors).stream()
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, Function.identity()));
	}

	public Map<GxsId, IdentityGroupItem> getAuthorsMapFromMessages(List<ForumMessageItem> forumMessages)
	{
		var authors = forumMessages.stream()
				.map(ForumMessageItem::getAuthorGxsId)
				.collect(Collectors.toSet());

		return identityService.findAll(authors).stream()
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, Function.identity()));
	}

	public Map<MsgId, ForumMessageItem> getMessagesMapFromSummaries(long groupId, Page<ForumMessageItemSummary> forumMessages)
	{
		var msgIds = forumMessages.stream()
				.map(ForumMessageItemSummary::getMsgId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		var parentMsgIds = forumMessages.stream()
				.map(ForumMessageItemSummary::getParentMsgId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		return forumRsService.findAllMessages(groupId, SetUtils.union(msgIds, parentMsgIds)).stream()
				.collect(Collectors.toMap(ForumMessageItem::getMsgId, Function.identity()));
	}

	public Map<MsgId, ForumMessageItem> getMessagesMapFromMessages(long groupId, List<ForumMessageItem> forumMessages)
	{
		var msgIds = forumMessages.stream()
				.map(ForumMessageItem::getMsgId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		var parentMsgIds = forumMessages.stream()
				.map(ForumMessageItem::getParentMsgId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		// XXX: update? why isn't this used?

		return forumRsService.findAllMessages(groupId, SetUtils.union(msgIds, parentMsgIds)).stream()
				.collect(Collectors.toMap(ForumMessageItem::getMsgId, Function.identity()));
	}

	public Map<MsgId, ForumMessageItem> getMessagesMapFromMessages(List<ForumMessageItem> forumMessages)
	{
		var msgIds = forumMessages.stream()
				.map(ForumMessageItem::getMsgId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		var parentMsgIds = forumMessages.stream()
				.map(ForumMessageItem::getParentMsgId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		var originalMsgIds = forumMessages.stream()
				.map(ForumMessageItem::getOriginalMsgId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		var map = forumRsService.findAllMessages(SetUtils.union(msgIds, parentMsgIds)).stream()
				.collect(Collectors.toMap(ForumMessageItem::getMsgId, Function.identity()));

		if (!originalMsgIds.isEmpty())
		{
			forumRsService.findAllOldMessages(originalMsgIds).forEach(forumMessageItem -> map.put(forumMessageItem.getMsgId(), forumMessageItem));
		}

		return map;
	}
}
