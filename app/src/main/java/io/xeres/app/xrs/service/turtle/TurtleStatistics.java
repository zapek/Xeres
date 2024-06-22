/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.turtle;

/**
 * Everything is in bytes per seconds.
 */
public class TurtleStatistics
{
	private float forwardTotal;

	private float dataUpload;
	private float dataDownload;

	private float tunnelRequestsUpload;
	private float tunnelRequestsDownload;

	private float searchRequestsUpload;
	private float searchRequestsDownload;

	private float totalUpload;
	private float totalDownload;

	public TurtleStatistics()
	{
	}

	private TurtleStatistics(TurtleStatistics from)
	{
		forwardTotal = from.forwardTotal;

		dataUpload = from.dataUpload;
		dataDownload = from.dataDownload;

		tunnelRequestsUpload = from.tunnelRequestsUpload;
		tunnelRequestsDownload = from.tunnelRequestsDownload;

		searchRequestsUpload = from.searchRequestsUpload;
		searchRequestsDownload = from.searchRequestsDownload;

		totalUpload = from.totalUpload;
		totalDownload = from.totalDownload;
	}

	public synchronized void reset()
	{
		forwardTotal = 0.0f;

		dataUpload = 0.0f;
		dataDownload = 0.0f;

		tunnelRequestsUpload = 0.0f;
		tunnelRequestsDownload = 0.0f;

		searchRequestsUpload = 0.0f;
		searchRequestsDownload = 0.0f;

		totalUpload = 0.0f;
		totalDownload = 0.0f;
	}

	public synchronized TurtleStatistics multiply(float number)
	{
		var result = new TurtleStatistics(this);

		result.forwardTotal *= number;

		result.dataUpload *= number;
		result.dataDownload *= number;

		result.tunnelRequestsUpload *= number;
		result.tunnelRequestsDownload *= number;

		result.searchRequestsUpload *= number;
		result.searchRequestsDownload *= number;

		result.totalUpload *= number;
		result.totalDownload *= number;

		return result;
	}

	public synchronized TurtleStatistics add(float number)
	{
		var result = new TurtleStatistics(this);

		result.forwardTotal += number;

		result.dataUpload += number;
		result.dataDownload += number;

		result.tunnelRequestsUpload += number;
		result.tunnelRequestsDownload += number;

		result.searchRequestsUpload += number;
		result.searchRequestsDownload += number;

		result.totalUpload += number;
		result.totalDownload += number;

		return result;
	}

	public synchronized TurtleStatistics add(TurtleStatistics other)
	{
		var result = new TurtleStatistics(this);

		result.forwardTotal += other.forwardTotal;

		result.dataUpload += other.dataUpload;
		result.dataDownload += other.dataDownload;

		result.tunnelRequestsUpload += other.tunnelRequestsUpload;
		result.tunnelRequestsDownload += other.tunnelRequestsDownload;

		result.searchRequestsUpload += other.searchRequestsUpload;
		result.searchRequestsDownload += other.searchRequestsDownload;

		result.totalUpload += other.totalUpload;
		result.totalDownload += other.totalDownload;

		return result;
	}

	public synchronized void addToTunnelRequestsDownload(int size)
	{
		tunnelRequestsDownload += size;
	}

	public synchronized void addToTunnelRequestsUpload(int size)
	{
		tunnelRequestsUpload += size;
	}

	public synchronized void addToSearchRequestsDownload(int size)
	{
		searchRequestsDownload += size;
	}

	public synchronized void addToSearchRequestsUpload(int size)
	{
		searchRequestsUpload += size;
	}

	public synchronized void addToForwardTotal(int size)
	{
		forwardTotal += size;
	}

	public synchronized void addToDataDownload(int size)
	{
		dataDownload += size;
	}

	public synchronized void addToDataUpload(int size)
	{
		dataUpload += size;
	}

	public float getForwardTotal()
	{
		return forwardTotal;
	}

	public float getDataUpload()
	{
		return dataUpload;
	}

	public float getDataDownload()
	{
		return dataDownload;
	}

	public float getTunnelRequestsUpload()
	{
		return tunnelRequestsUpload;
	}

	public float getTunnelRequestsDownload()
	{
		return tunnelRequestsDownload;
	}

	public float getSearchRequestsUpload()
	{
		return searchRequestsUpload;
	}

	public float getSearchRequestsDownload()
	{
		return searchRequestsDownload;
	}

	public float getTotalUpload()
	{
		return totalUpload;
	}

	public float getTotalDownload()
	{
		return totalDownload;
	}

	public TurtleStatistics getStatistics()
	{
		return new TurtleStatistics(this);
	}

	@Override
	public synchronized String toString()
	{
		return "TrafficStatistics [forwardTotal=" + forwardTotal +
				", dataUpload=" + dataUpload +
				", dataDownload=" + dataDownload +
				", tunnelRequestsUpload=" + tunnelRequestsUpload +
				", tunnelRequestsDownload=" + tunnelRequestsDownload +
				", searchRequestsUpload=" + searchRequestsUpload +
				", searchRequestsDownload=" + searchRequestsDownload +
				", totalUpload=" + totalUpload +
				", totalDownload=" + totalDownload + "]";
	}
}
