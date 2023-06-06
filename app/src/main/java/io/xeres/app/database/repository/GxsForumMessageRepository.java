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

package io.xeres.app.database.repository;

import io.xeres.app.database.model.forum.ForumMessageItemSummary;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Transactional
public interface GxsForumMessageRepository extends JpaRepository<ForumMessageItem, Long>
{
	Optional<ForumMessageItem> findByGxsIdAndMessageId(GxsId groupId, MessageId messageId);

	List<ForumMessageItem> findAllByGxsIdAndPublishedAfter(GxsId groupId, Instant since);

	List<ForumMessageItem> findAllByGxsIdAndMessageIdIn(GxsId groupId, Set<MessageId> messageIds);

	List<ForumMessageItemSummary> findSummaryAllByGxsId(GxsId groupId);
}
