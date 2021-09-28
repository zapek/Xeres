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

package io.xeres.app.xrs.service.chat;

import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.xrs.service.chat.item.VisibleChatRoomInfo;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Id;
import io.xeres.common.id.LocationId;
import io.xeres.common.message.chat.RoomInfo;
import io.xeres.common.message.chat.RoomType;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom
{
	private final long id;
	private final String name;
	private final String topic;
	private final Set<PeerConnection> participatingPeers = ConcurrentHashMap.newKeySet();
	private GxsId gxsId; // signing entity
	private final Map<GxsId, Long> gxsIds = new ConcurrentHashMap<>(); // non direct friends who are participating
	private final int userCount;
	private Instant lastActivity;
	private final RoomType type;
	private final boolean signed;

	private final MessageCache messageCache = new MessageCache();
	private LocationId virtualPeerId; // XXX: check if we need that...
	private int connectionChallengeCount;
	private Instant lastConnectionChallenge = Instant.EPOCH;
	private boolean joinedRoomPacketSent;
	private Instant lastKeepAlivePacket = Instant.EPOCH;
	private final Set<LocationId> previouslyKnownLocations = ConcurrentHashMap.newKeySet();

	public ChatRoom(long id, String name, String topic, RoomType type, int userCount, boolean isSigned)
	{
		this.id = id;
		this.name = name;
		this.topic = topic;
		this.type = type;
		this.userCount = userCount; // XXX: use that if available, other gxsId.size() which is more precise
		this.signed = isSigned;
	}

	/**
	 * Get as a RoomInfo structure, used for displaying in the UI
	 *
	 * @return a RoomInfo
	 */
	public RoomInfo getAsRoomInfo()
	{
		return new RoomInfo(
				id,
				name,
				type,
				topic,
				userCount, // XXX: use a getUserCount() which choses what to get
				signed
		);
	}

	/**
	 * Get as a VisibleChatRoomInfo, used for serialization as RS protocol
	 *
	 * @return a VisibleChatRoomInfo
	 */
	public VisibleChatRoomInfo getAsVisibleChatRoomInfo()
	{
		return new VisibleChatRoomInfo(
				id,
				name,
				topic,
				userCount, // XXX: ditto
				getRoomFlags()
		);
	}

	public long getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public String getTopic()
	{
		return topic;
	}

	public Set<PeerConnection> getParticipatingPeers()
	{
		return participatingPeers;
	}

	public void addParticipatingPeer(PeerConnection peerConnection)
	{
		participatingPeers.add(peerConnection);
	}

	public void removeParticipatingPeer(PeerConnection peerConnection)
	{
		participatingPeers.remove(peerConnection);
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	public Map<GxsId, Long> getGxsIds()
	{
		return gxsIds;
	}

	public Instant getLastActivity()
	{
		return lastActivity;
	}

	public void updateActivity()
	{
		lastActivity = Instant.now();
	}

	public LocationId getVirtualPeerId()
	{
		return virtualPeerId;
	}

	public int getConnectionChallengeCount()
	{
		return connectionChallengeCount;
	}

	public int getConnectionChallengeCountAndIncrease()
	{
		return connectionChallengeCount++;
	}

	public void resetConnectionChallengeCount()
	{
		connectionChallengeCount = 0;
		this.lastConnectionChallenge = Instant.now();
	}

	public Instant getLastConnectionChallenge()
	{
		return lastConnectionChallenge;
	}

	// XXX: what is that used for?
	public boolean isJoinedRoomPacketSent()
	{
		return joinedRoomPacketSent;
	}

	public void setLastKeepAlivePacket(Instant lastKeepAlivePacket)
	{
		this.lastKeepAlivePacket = lastKeepAlivePacket;
	}

	public Instant getLastKeepAlivePacket()
	{
		return lastKeepAlivePacket;
	}

	public Set<LocationId> getPreviouslyKnownLocations()
	{
		return previouslyKnownLocations;
	}

	public boolean isPublic()
	{
		return type == RoomType.PUBLIC;
	}

	public boolean isSigned()
	{
		return signed;
	}

	public long getNewMessageId()
	{
		return messageCache.getNewMessageId();
	}

	public void incrementConnectionChallengeCount()
	{
		connectionChallengeCount++;
	}

	public MessageCache getMessageCache()
	{
		return messageCache;
	}

	public Set<RoomFlags> getRoomFlags()
	{
		var roomFlags = EnumSet.noneOf(RoomFlags.class);
		if (type == RoomType.PUBLIC)
		{
			roomFlags.add(RoomFlags.PUBLIC);
		}
		if (signed)
		{
			roomFlags.add(RoomFlags.PGP_SIGNED);
		}
		return roomFlags;
	}

	@Override
	public String toString()
	{
		return "ChatRoom{" +
				"id=" + Id.toStringLowerCase(id) +
				", name='" + name + '\'' +
				'}';
	}
}
