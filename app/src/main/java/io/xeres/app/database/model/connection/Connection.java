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

package io.xeres.app.database.model.connection;

import io.xeres.app.database.converter.PeerAddressTypeConverter;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.protocol.PeerAddress;

import javax.persistence.*;
import java.time.Instant;
import java.util.Objects;

import static io.xeres.app.net.protocol.PeerAddress.Type.HOSTNAME;
import static io.xeres.app.net.protocol.PeerAddress.Type.IPV4;

@Table(name = "connections")
@Entity
public class Connection
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id", nullable = false)
	private Location location;

	@Convert(converter = PeerAddressTypeConverter.class)
	private PeerAddress.Type type;

	private String address;

	private Instant lastConnected;

	private boolean external;

	protected Connection()
	{
	}

	public static Connection from(PeerAddress peerAddress)
	{
		return new Connection(peerAddress);
	}

	private Connection(PeerAddress peerAddress)
	{
		type = peerAddress.getType();
		address = peerAddress.getAddress().orElseThrow();
		external = peerAddress.isExternal();
	}

	long getId()
	{
		return id;
	}

	void setId(long id)
	{
		this.id = id;
	}

	public Location getLocation()
	{
		return location;
	}

	public void setLocation(Location location)
	{
		this.location = location;
	}

	public PeerAddress.Type getType()
	{
		return type;
	}

	public void setType(PeerAddress.Type type)
	{
		this.type = type;
	}

	public String getAddress()
	{
		return address;
	}

	public void setAddress(String address)
	{
		this.address = address;
	}

	public Instant getLastConnected()
	{
		return lastConnected;
	}

	public void setLastConnected(Instant lastConnected)
	{
		this.lastConnected = lastConnected;
	}

	public boolean isExternal()
	{
		return external;
	}

	public void setExternal(boolean external)
	{
		this.external = external;
	}

	public int getPort()
	{
		if (!(type == IPV4 || type == HOSTNAME))
		{
			throw new IllegalArgumentException("Trying to get port from a non ipv4 address");
		}
		var tokens = address.split(":");
		return Integer.parseInt(tokens[1]);
	}

	public String getIp()
	{
		if (type != IPV4)
		{
			throw new IllegalArgumentException("Trying to get ip from a non ipv4 address");
		}
		var tokens = address.split(":");
		return tokens[0];
	}

	public String getHostname()
	{
		if (type != HOSTNAME)
		{
			throw new IllegalArgumentException("Trying to get a hostname from a non hostname address");
		}
		var tokens = address.split(":");
		return tokens[0];
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		var that = (Connection) o;
		return external == that.external && type == that.type && address.equals(that.address);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(type, address, external);
	}

	@Override
	public String toString()
	{
		return "Connection{" +
				"type=" + type +
				", address='" + address + '\'' +
				", external=" + external +
				'}';
	}
}
