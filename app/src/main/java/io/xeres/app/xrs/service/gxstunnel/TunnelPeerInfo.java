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

import java.time.Instant;
import java.util.Map;
import java.util.Set;

class TunnelPeerInfo
{
	public enum Status
	{
		UNKNOWN,
		TUNNEL_DOWN,
		CAN_TALK,
		REMOTELY_CLOSED
	}

	private Instant lastContact;
	private Instant lastKeepAliveSent;
	private byte[] aesKey;
	private Sha1Sum hash;
	private Status status;
	private Location location;
	private GxsId destination;
	private TunnelDirection direction;
	private Set<Integer> clientServices;
	private Map<Long, Instant> receivedMessages;
	private long totalSent;
	private long totalReceived;

	public TunnelPeerInfo(byte[] aesKey, Status status, Location location, TunnelDirection direction, GxsId destination)
	{
		var now = Instant.now();

		lastContact = now;
		lastKeepAliveSent = now;
		this.aesKey = aesKey;
		this.status = status;
		this.location = location;
		this.direction = direction;
		this.destination = destination;
	}

	public Sha1Sum getHash()
	{
		return hash;
	}

	public Status getStatus()
	{
		return status;
	}


	public void addSentSize(int size)
	{
		totalSent += size;
	}
}
