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

package io.xeres.app.database.repository;

import io.xeres.app.xrs.service.board.item.BoardMessageItem;
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
public interface GxsBoardMessageRepository extends JpaRepository<BoardMessageItem, Long>
{
	Optional<BoardMessageItem> findByGxsIdAndMessageId(GxsId groupId, MessageId messageId);

	Page<BoardMessageItem> findAllByGxsId(GxsId groupId, Pageable pageable);

	List<BoardMessageItem> findAllByGxsIdAndPublishedAfter(GxsId groupId, Instant since);

	List<BoardMessageItem> findAllByGxsIdAndMessageIdIn(GxsId groupId, Set<MessageId> messageIds);

	List<BoardMessageItem> findAllByMessageIdIn(Set<MessageId> messageIds);

	@Query("SELECT COUNT(m.id) FROM board_message m WHERE m.gxsId = :gxsId AND m.read = false")
	int countUnreadMessages(GxsId gxsId);

	@Modifying
	@Transactional
	@Query("UPDATE board_message m SET m.read = :read WHERE m.gxsId = :gxsId AND m.read != :read")
	int markAllMessagesAsRead(GxsId gxsId, boolean read);
}
