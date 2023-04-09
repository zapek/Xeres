/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

import io.xeres.app.database.repository.GxsForumGroupRepository;
import io.xeres.app.database.repository.GxsForumMessageRepository;
import io.xeres.app.xrs.service.forum.item.ForumGroupItem;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ForumService
{
	private static final Logger log = LoggerFactory.getLogger(ForumService.class);

	private final GxsForumGroupRepository gxsForumGroupRepository;
	private final GxsForumMessageRepository gxsForumMessageRepository;

	public ForumService(GxsForumGroupRepository gxsForumGroupRepository, GxsForumMessageRepository gxsForumMessageRepository)
	{
		this.gxsForumGroupRepository = gxsForumGroupRepository;
		this.gxsForumMessageRepository = gxsForumMessageRepository;
	}

	public List<ForumGroupItem> findAllGroups()
	{
		return gxsForumGroupRepository.findAll();
	}

	public List<ForumGroupItem> findAllGroups(Set<GxsId> gxsIds)
	{
		return gxsForumGroupRepository.findAllByGxsIdIn(gxsIds);
	}

	public List<ForumGroupItem> findAllGroupsSubscribedAndPublishedSince(Instant since)
	{
		return gxsForumGroupRepository.findAllBySubscribedIsTrueAndPublishedAfter(since);
	}

	@Transactional
	public void save(ForumGroupItem forumGroupItem)
	{
		forumGroupItem.setId(gxsForumGroupRepository.findByGxsId(forumGroupItem.getGxsId()).orElse(forumGroupItem).getId());
		gxsForumGroupRepository.save(forumGroupItem);
		// XXX: setLastServiceUpdate() ! (though, it seems to work already?) and I also should do it for messages
	}

	public List<ForumMessageItem> findAllMessagesInGroupSince(GxsId groupId, Instant since)
	{
		return gxsForumMessageRepository.findAllByGxsIdAndPublishedAfter(groupId, since);
	}

	public List<ForumMessageItem> findAllMessages(GxsId groupId, Set<MessageId> messageIds)
	{
		return gxsForumMessageRepository.findAllByGxsIdAndMessageIdIn(groupId, messageIds);
	}

	@Transactional
	public void save(ForumMessageItem forumMessageItem)
	{
		forumMessageItem.setId(gxsForumMessageRepository.findByGxsIdAndMessageId(forumMessageItem.getGxsId(), forumMessageItem.getMessageId()).orElse(forumMessageItem).getId()); // XXX: not sure we should be able to overwrite a message. in which case is it correct? maybe throw?
		gxsForumMessageRepository.save(forumMessageItem);
		// XXX: setLastServiceUpdate() ? I think so actually!
	}
}
