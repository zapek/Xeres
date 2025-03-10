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

package io.xeres.app.database.repository;

import io.xeres.app.database.model.chat.ChatRoom;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long>
{
	Optional<ChatRoom> findByRoomIdAndIdentityGroupItem(long roomId, IdentityGroupItem identityGroupItem);

	Optional<ChatRoom> findByRoomId(long roomId); // Should eventually go away when we have multiple identities but... not sure yet

	List<ChatRoom> findAllBySubscribedTrueAndJoinedFalse();

	@Modifying
	@Transactional
	@Query("UPDATE ChatRoom c SET c.joined = false WHERE c.joined = true")
	void putAllJoinedToFalse();
}
