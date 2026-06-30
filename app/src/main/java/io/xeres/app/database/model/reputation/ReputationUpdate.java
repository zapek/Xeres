/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.app.database.model.reputation;

import io.xeres.app.database.model.location.Location;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Entity
public class ReputationUpdate
{
	public static final ReputationUpdate DEFAULT_REPUTATION_UPDATE = new ReputationUpdate();

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@NotNull
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id", nullable = false)
	private Location location;

	@NotNull
	private Instant lastUpdated = Instant.EPOCH;

	@SuppressWarnings("unused")
	public ReputationUpdate()
	{
	}

	public ReputationUpdate(Location location, Instant lastUpdated)
	{
		this.location = location;
		this.lastUpdated = lastUpdated;
	}

	public long getId()
	{
		return id;
	}

	public void setId(long id)
	{
		this.id = id;
	}

	public Instant getLastUpdated()
	{
		return lastUpdated;
	}

	public void setLastUpdated(Instant lastUpdated)
	{
		this.lastUpdated = lastUpdated;
	}

	public Location getLocation()
	{
		return location;
	}
}
