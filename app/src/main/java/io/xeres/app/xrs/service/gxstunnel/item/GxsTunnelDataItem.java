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

package io.xeres.app.xrs.service.gxstunnel.item;

import io.xeres.app.database.model.location.Location;
import io.xeres.app.xrs.serialization.RsSerialized;

import java.time.Instant;

public class GxsTunnelDataItem extends GxsTunnelItem implements Comparable<GxsTunnelDataItem>
{
	@RsSerialized
	private long counter;

	@RsSerialized
	private int flags; // Not used

	@RsSerialized
	private int serviceId;

	@RsSerialized
	private byte[] tunnelData;

	// Used for resending
	private Instant lastSendingAttempt = Instant.EPOCH;
	private Location location;

	public GxsTunnelDataItem()
	{
		// Needed
	}

	public GxsTunnelDataItem(long counter, int serviceId, byte[] tunnelData)
	{
		this.counter = counter;
		this.serviceId = serviceId;
		this.tunnelData = tunnelData;
	}

	@Override
	public int getSubType()
	{
		return 1;
	}

	public long getCounter()
	{
		return counter;
	}

	public int getServiceId()
	{
		return serviceId;
	}

	public byte[] getTunnelData()
	{
		return tunnelData;
	}

	public void updateLastSendingAttempt()
	{
		lastSendingAttempt = Instant.now();
	}

	public Instant getLastSendingAttempt()
	{
		return lastSendingAttempt;
	}

	public void setForResending(Location location)
	{
		this.location = location;
		updateLastSendingAttempt();
	}

	public Location getLocation()
	{
		return location;
	}

	@Override
	public int compareTo(GxsTunnelDataItem o)
	{
		return lastSendingAttempt.compareTo(o.lastSendingAttempt);
	}

	@Override
	public String toString()
	{
		return "GxsTunnelDataItem{" +
				"serviceId=" + serviceId +
				'}';
	}
}
