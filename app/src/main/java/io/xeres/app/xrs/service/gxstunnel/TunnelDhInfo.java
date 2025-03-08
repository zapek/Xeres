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
import io.xeres.common.id.Sha1Sum;

import java.security.KeyPair;

/**
 * Used to keep track of a Diffie-Hellman session.
 */
class TunnelDhInfo
{
	public enum Status
	{
		UNINITIALIZED,
		HALF_KEY_DONE,
		KEY_AVAILABLE
	}

	private Status status;
	private Sha1Sum hash;
	private TunnelDirection direction;
	private KeyPair keyPair;
	private Location tunnelId;

	public Status getStatus()
	{
		return status;
	}

	public void setStatus(Status status)
	{
		this.status = status;
	}

	public Sha1Sum getHash()
	{
		return hash;
	}

	public void setHash(Sha1Sum hash)
	{
		this.hash = hash;
	}

	public TunnelDirection getDirection()
	{
		return direction;
	}

	public void setDirection(TunnelDirection direction)
	{
		this.direction = direction;
	}

	public KeyPair getKeyPair()
	{
		return keyPair;
	}

	public void setKeyPair(KeyPair keyPair)
	{
		this.keyPair = keyPair;
	}

	public Location getTunnelId()
	{
		return tunnelId;
	}

	public void setTunnelId(Location tunnelId)
	{
		this.tunnelId = tunnelId;
	}

	public void clear()
	{
		status = Status.UNINITIALIZED;
		keyPair = null;
		tunnelId = null;
	}

	@Override
	public String toString()
	{
		return "TunnelDhInfo{" +
				"status=" + status +
				", direction=" + direction +
				", tunnelId=" + tunnelId +
				'}';
	}
}
