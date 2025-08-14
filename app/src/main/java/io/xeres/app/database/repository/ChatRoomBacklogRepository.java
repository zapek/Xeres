/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

import io.xeres.app.database.model.chat.ChatRoom;
import io.xeres.app.database.model.chat.ChatRoomBacklog;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Transactional(readOnly = true)
public interface ChatRoomBacklogRepository extends JpaRepository<ChatRoomBacklog, Long>
{
	List<ChatRoomBacklog> findAllByRoomAndCreatedAfterOrderByCreatedDesc(ChatRoom chatRoom, Instant from, Limit limit);

	void deleteAllByCreatedBefore(Instant before);

	void deleteAllByRoom(ChatRoom chatRoom);
}
