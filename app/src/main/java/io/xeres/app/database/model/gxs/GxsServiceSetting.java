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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.Instant;

@Entity
public class GxsServiceSetting
{
	@Id
	private int id;

	private Instant lastUpdated;

	public GxsServiceSetting()
	{
		// Needed
	}

	public GxsServiceSetting(int id, Instant lastUpdated)
	{
		this.id = id;
		this.lastUpdated = lastUpdated;
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
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

	@Override
	public String toString()
	{
		return "GxsServiceSetting{" +
				"lastUpdated=" + lastUpdated +
				'}';
	}
}
