/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.gxstunnel;

import io.xeres.app.database.model.location.Location;
import io.xeres.app.xrs.service.turtle.item.TunnelDirection;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Sha1Sum;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class TunnelPeerInfo
{
	private Instant lastContact;
	private Instant lastKeepAliveSent;
	private byte[] aesKey;
	private Sha1Sum hash;
	private GxsTunnelStatus status;
	private Location location;
	private final GxsId destination;
	private final TunnelDirection direction;
	private final Set<Integer> clientServices = new HashSet<>();
	private final Map<Long, Instant> receivedMessages = new ConcurrentHashMap<>();
	private long totalSent;
	private long totalReceived;

	public TunnelPeerInfo(Sha1Sum hash, GxsId destination, int serviceId)
	{
		var now = Instant.now();

		lastContact = now;
		lastKeepAliveSent = now;
		status = GxsTunnelStatus.TUNNEL_DOWN;
		direction = TunnelDirection.SERVER;
		this.hash = hash;
		this.destination = destination;
		clientServices.add(serviceId);
	}

	public TunnelPeerInfo(byte[] aesKey, Location location, TunnelDirection direction, GxsId destination)
	{
		var now = Instant.now();

		lastContact = now;
		lastKeepAliveSent = now;
		status = GxsTunnelStatus.CAN_TALK;
		this.aesKey = aesKey;
		this.location = location;
		this.direction = direction;
		this.destination = destination;
	}

	public Sha1Sum getHash()
	{
		return hash;
	}

	public GxsTunnelStatus getStatus()
	{
		return status;
	}

	public void setStatus(GxsTunnelStatus status)
	{
		this.status = status;
	}

	public void clearLocation()
	{
		location = null;
	}

	public Location getLocation()
	{
		return location;
	}

	public byte[] getAesKey()
	{
		return aesKey;
	}

	public TunnelDirection getDirection()
	{
		return direction;
	}

	public GxsId getDestination()
	{
		return destination;
	}

	public Set<Integer> getClientServices()
	{
		return clientServices;
	}

	public Instant getLastContact()
	{
		return lastContact;
	}

	public Instant getLastKeepAliveSent()
	{
		return lastKeepAliveSent;
	}

	public void updateLastKeepAlive()
	{
		lastKeepAliveSent = Instant.now();
	}

	public void addSentSize(int size)
	{
		totalSent += size;
	}

	public void addReceivedSize(int size)
	{
		totalReceived += size;
	}

	public void updateLastContact()
	{
		lastContact = Instant.now();
	}

	public void addService(int serviceId)
	{
		clientServices.add(serviceId);
	}

	public void removeService(int serviceId)
	{
		clientServices.remove(serviceId);
	}

	public boolean checkIfMessageAlreadyReceivedAndRecord(long messageId)
	{
		var message = receivedMessages.get(messageId);
		if (message != null)
		{
			return true;
		}
		receivedMessages.put(messageId, Instant.now());
		return false;
	}

	public void cleanupReceivedMessagesOlderThan(Duration delay)
	{
		var now = Instant.now();

		receivedMessages.entrySet().removeIf(entry -> entry.getValue().plus(delay).isAfter(now));
	}
}
