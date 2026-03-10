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

import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Transactional(readOnly = true)
public interface GxsMessageItemRepository extends JpaRepository<GxsMessageItem, Long>
{
	Optional<GxsMessageItem> findByGxsIdAndMessageId(GxsId gxsId, MessageId messageId);

	int countByGxsId(GxsId gxsId);

	/**
	 * If messages are received out of order, it's possible that we receive a message that replace another (so nothing is done), then we receive that message afterwards.
	 * We have to check for that our of order message and mark it as hidden.
	 *
	 * @param gxsId the message group
	 * @param since since when to consider the messages
	 */
	@Modifying
	@Transactional
	@Query("UPDATE gxs_message m SET m.hidden = true WHERE m.gxsId = :gxsId AND m.hidden = false AND m.published >= :since AND EXISTS (SELECT 1 FROM gxs_message m2 WHERE m2.gxsId = :gxsId AND m2.messageId != m.messageId AND m2.originalMessageId = m.messageId)")
	void fixIntervalDuplicates(GxsId gxsId, Instant since);

	/**
	 * Retroshare can branch from a message that is not the latest. We check if there exists another message with the same originalMessageId but with a
	 * later published timestamp, if so, mark it as hidden because it's not the latest.
	 *
	 * @param gxsId the message group
	 * @param since since when to consider the messages
	 */
	@Modifying
	@Transactional
	@Query("UPDATE gxs_message m SET m.hidden = true WHERE m.gxsId = :gxsId AND m.hidden = false AND m.published >= :since AND m.originalMessageId IS NOT NULL AND EXISTS (SELECT 1 FROM gxs_message m2 WHERE m2.gxsId = :gxsId AND m2.messageId != m.messageId AND m2.originalMessageId = m.originalMessageId AND m2.published > m.published)")
	void hideOldDuplicates(GxsId gxsId, Instant since);
}
