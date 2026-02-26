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

package io.xeres.app.database.repository;

import io.xeres.app.database.model.forum.ForumMessageItemSummary;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Transactional(readOnly = true)
public interface GxsForumMessageRepository extends JpaRepository<ForumMessageItem, Long>
{
	Optional<ForumMessageItem> findByGxsIdAndMessageId(GxsId groupId, MessageId messageId);

	List<ForumMessageItem> findAllByGxsIdAndPublishedAfterAndHiddenFalse(GxsId groupId, Instant since);

	List<ForumMessageItem> findAllByGxsIdAndMessageIdInAndHiddenFalse(GxsId groupId, Set<MessageId> messageIds);

	Page<ForumMessageItemSummary> findSummaryAllByGxsIdAndHiddenFalse(GxsId groupId, Pageable pageable);

	List<ForumMessageItem> findAllByMessageIdInAndHiddenFalse(Set<MessageId> messageIds);

	List<ForumMessageItem> findAllByMessageIdInAndHiddenTrue(Set<MessageId> messageIds);

	@Query("SELECT COUNT(m.id) FROM forum_message m WHERE m.gxsId = :gxsId AND m.read = false AND m.hidden = false")
	int countUnreadMessages(GxsId gxsId);

	@Modifying
	@Transactional
	@Query("UPDATE forum_message m SET m.read = :read WHERE m.gxsId = :gxsId AND m.read != :read")
	int markAllMessagesAsRead(GxsId gxsId, boolean read);
}
