/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.app.database.model.chat;

import io.xeres.common.id.GxsId;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
public class ChatRoomBacklog
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id", nullable = false)
	private ChatRoom room;

	@CreationTimestamp
	private Instant created;

	@Embedded
	@AttributeOverride(name = "identifier", column = @Column(name = "gxs_id"))
	private GxsId gxsId;

	private String nickname;

	private String message;

	protected ChatRoomBacklog()
	{

	}

	public ChatRoomBacklog(ChatRoom room, GxsId gxsId, String nickname, String message)
	{
		this.room = room;
		this.gxsId = gxsId;
		this.nickname = nickname;
		this.message = message;
	}

	public ChatRoomBacklog(ChatRoom room, String nickname, String message)
	{
		this.room = room;
		this.nickname = nickname;
		this.message = message;
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public ChatRoom getRoom()
	{
		return room;
	}

	public void setRoom(ChatRoom room)
	{
		this.room = room;
	}

	public Instant getCreated()
	{
		return created;
	}

	public void setCreated(Instant timestamp)
	{
		this.created = timestamp;
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	public void setGxsId(GxsId gxsId)
	{
		this.gxsId = gxsId;
	}

	public String getNickname()
	{
		return nickname;
	}

	public void setNickname(String nickname)
	{
		this.nickname = nickname;
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}
}
