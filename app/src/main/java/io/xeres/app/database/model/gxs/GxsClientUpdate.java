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

package io.xeres.app.database.model.gxs;

import io.xeres.app.database.model.location.Location;
import io.xeres.common.id.GxsId;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Table(name = "gxs_client_updates")
@Entity
public class GxsClientUpdate
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@NotNull
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id", nullable = false)
	private Location location;

	@NotNull
	private int serviceType;

	private Instant lastSynced;

	@ElementCollection
	@Column(name = "updated")
	private Map<GxsId, Instant> messages = new HashMap<>();

	public GxsClientUpdate()
	{
		// Needed
	}

	public GxsClientUpdate(Location location, int serviceType, Instant lastSynced)
	{
		this.location = location;
		this.serviceType = serviceType;
		this.lastSynced = lastSynced;
	}

	public GxsClientUpdate(Location location, int serviceType, GxsId groupId, Instant lastSynced)
	{
		this.location = location;
		this.serviceType = serviceType;
		messages.put(groupId, lastSynced);
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
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

	public int getServiceType()
	{
		return serviceType;
	}

	public void setServiceType(int serviceType)
	{
		this.serviceType = serviceType;
	}

	public Instant getLastSynced()
	{
		return lastSynced;
	}

	public void setLastSynced(Instant lastSynced)
	{
		this.lastSynced = lastSynced;
	}

	public Instant getMessageUpdate(GxsId groupId)
	{
		return messages.get(groupId);
	}

	public void addMessageUpdate(GxsId groupId, Instant lastSynced)
	{
		messages.put(groupId, lastSynced);
	}

	public void removeMessageUpdate(GxsId groupId)
	{
		messages.remove(groupId);
	}

	@Override
	public String toString()
	{
		return "GxsClientUpdate{" +
				"location=" + location +
				", serviceType=" + serviceType +
				", lastSynced=" + lastSynced +
				'}';
	}
}
