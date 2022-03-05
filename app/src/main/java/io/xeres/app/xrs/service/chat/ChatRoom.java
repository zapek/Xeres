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

import io.xeres.app.database.model.location.Location;
import io.xeres.app.xrs.service.chat.item.VisibleChatRoomInfo;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Id;
import io.xeres.common.id.LocationId;
import io.xeres.common.message.chat.ChatRoomInfo;
import io.xeres.common.message.chat.RoomType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoom
{
	private static final Logger log = LoggerFactory.getLogger(ChatRoom.class);

	private static final long USER_INACTIVITY_TIMEOUT = Duration.ofMinutes(3).toSeconds();

	private final long id;
	private final String name;
	private final String topic;
	private final Set<Location> participatingLocations = ConcurrentHashMap.newKeySet();
	private GxsId ownGxsId;
	private final Map<GxsId, Long> users = new ConcurrentHashMap<>();
	private final int userCount;
	private Instant lastActivity;
	private Instant lastSeen = Instant.now();
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
	public ChatRoomInfo getAsRoomInfo()
	{
		return new ChatRoomInfo(
				id,
				name,
				type,
				topic,
				getUserCount(),
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
				getUserCount(),
				getRoomFlags()
		);
	}

	private int getUserCount()
	{
		return users.isEmpty() ? userCount : users.size();
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

	public Set<Location> getParticipatingLocations()
	{
		return participatingLocations;
	}

	public boolean addParticipatingLocation(Location location)
	{
		return participatingLocations.add(location);
	}

	public void removeParticipatingLocation(Location location)
	{
		participatingLocations.remove(location);
	}

	public void recordPreviouslyKnownLocation(Location location)
	{
		previouslyKnownLocations.add(location.getLocationId());
	}

	public boolean isPreviouslyKnownLocation(Location location)
	{
		return previouslyKnownLocations.contains(location.getLocationId());
	}

	public void setOwnGxsId(GxsId gxsId)
	{
		this.ownGxsId = gxsId;
	}

	public void addUser(GxsId user)
	{
		users.put(user, Instant.now().getEpochSecond());
	}

	public void userActivity(GxsId user)
	{
		users.replace(user, Instant.now().getEpochSecond());
	}

	public void removeUser(GxsId user)
	{
		users.remove(user);
	}

	public Set<GxsId> getExpiredUsers()
	{
		var now = Instant.now().getEpochSecond();

		Set<GxsId> expiredUsers = new HashSet<>();
		users.forEach((user, timestamp) -> {
			if (timestamp + USER_INACTIVITY_TIMEOUT < now)
			{
				if (!user.equals(ownGxsId)) // We never expire ourself
				{
					expiredUsers.add(user);
				}
			}
		});
		return expiredUsers;
	}

	public void clearUsers()
	{
		users.clear();
	}

	public Instant getLastActivity()
	{
		return lastActivity;
	}

	public void updateActivity()
	{
		lastActivity = Instant.now();
	}

	public Instant getLastSeen()
	{
		return lastSeen;
	}

	public void updateLastSeen()
	{
		lastSeen = Instant.now();
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

	public boolean isJoinedRoomPacketSent()
	{
		return joinedRoomPacketSent;
	}

	public void setJoinedRoomPacketSent(boolean joinedRoomPacketSent)
	{
		this.joinedRoomPacketSent = joinedRoomPacketSent;
	}

	public void setLastKeepAlivePacket(Instant lastKeepAlivePacket)
	{
		this.lastKeepAlivePacket = lastKeepAlivePacket;
	}

	public Instant getLastKeepAlivePacket()
	{
		return lastKeepAlivePacket;
	}

	public boolean isPublic()
	{
		return type == RoomType.PUBLIC;
	}

	public boolean isPrivate()
	{
		return type == RoomType.PRIVATE;
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
