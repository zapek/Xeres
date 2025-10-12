/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.model.location;

import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.location.Availability;
import io.xeres.ui.model.connection.Connection;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Location
{
	private long id;
	private String name;
	private LocationIdentifier locationIdentifier;
	private String hostname;
	private final List<Connection> connections = new ArrayList<>();
	private boolean connected;
	private Instant lastConnected;
	private Availability availability;
	private String version;

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public LocationIdentifier getLocationIdentifier()
	{
		return locationIdentifier;
	}

	public void setLocationIdentifier(LocationIdentifier locationIdentifier)
	{
		this.locationIdentifier = locationIdentifier;
	}

	public String getHostname()
	{
		return hostname;
	}

	public void setHostname(String hostname)
	{
		this.hostname = hostname;
	}

	public List<Connection> getConnections()
	{
		return Collections.unmodifiableList(connections);
	}

	public void addConnections(List<Connection> connections)
	{
		this.connections.addAll(connections);
	}

	public boolean isConnected()
	{
		return connected;
	}

	public void setConnected(boolean connected)
	{
		this.connected = connected;
	}

	public Instant getLastConnected()
	{
		return lastConnected;
	}

	public void setLastConnected(Instant lastConnected)
	{
		this.lastConnected = lastConnected;
	}

	/**
	 * Returns the availability state. Always make sure to check {@link #isConnected()} first because
	 * this location has no concept of offline presence.
	 *
	 * @return the availability
	 */
	public Availability getAvailability()
	{
		return availability;
	}

	public void setAvailability(Availability availability)
	{
		this.availability = availability;
	}

	public String getVersion()
	{
		return version;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	public boolean hasVersion()
	{
		return StringUtils.isNotBlank(version);
	}
}
