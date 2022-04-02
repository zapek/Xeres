/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

import io.xeres.app.xrs.service.chat.RoomFlags;
import io.xeres.app.xrs.service.gxsid.item.GxsIdGroupItem;
import org.apache.commons.lang3.EnumUtils;

import javax.persistence.*;
import java.util.Set;

@Table(name = "chatrooms")
@Entity
public class ChatRoom
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	private long roomId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "gxs_id_group_id")
	private GxsIdGroupItem gxsIdGroupItem;

	private String name;
	private String topic;
	private int flags;
	private boolean subscribed;
	private boolean joined;

	protected ChatRoom()
	{

	}

	protected ChatRoom(long roomId, GxsIdGroupItem gxsIdGroupItem, String name, String topic, int flags)
	{
		this.roomId = roomId;
		this.gxsIdGroupItem = gxsIdGroupItem;
		this.name = name;
		this.topic = topic;
		this.flags = flags;
	}

	public static ChatRoom createChatRoom(io.xeres.app.xrs.service.chat.ChatRoom serviceChatRoom, GxsIdGroupItem gxsIdGroupItem)
	{
		return new ChatRoom(serviceChatRoom.getId(),
				gxsIdGroupItem,
				serviceChatRoom.getName(),
				serviceChatRoom.getTopic(),
				(int) EnumUtils.generateBitVector(RoomFlags.class, serviceChatRoom.getRoomFlags()));
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public long getRoomId()
	{
		return roomId;
	}

	public void setRoomId(long roomId)
	{
		this.roomId = roomId;
	}

	public GxsIdGroupItem getGxsIdGroupItem()
	{
		return gxsIdGroupItem;
	}

	public void setGxsIdGroupItem(GxsIdGroupItem gxsIdGroupItem)
	{
		this.gxsIdGroupItem = gxsIdGroupItem;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getTopic()
	{
		return topic;
	}

	public void setTopic(String topic)
	{
		this.topic = topic;
	}

	public Set<RoomFlags> getFlags()
	{
		return EnumUtils.processBitVector(RoomFlags.class, flags);
	}

	public void setFlags(Set<RoomFlags> flags)
	{
		this.flags = (int) EnumUtils.generateBitVector(RoomFlags.class, flags);
	}

	public boolean isSubscribed()
	{
		return subscribed;
	}

	public void setSubscribed(boolean subscribed)
	{
		this.subscribed = subscribed;
	}

	public boolean isJoined()
	{
		return joined;
	}

	public void setJoined(boolean joined)
	{
		this.joined = joined;
	}
}
