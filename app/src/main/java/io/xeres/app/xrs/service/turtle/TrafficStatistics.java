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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Everything is in bytes per seconds.
 */
class TrafficStatistics
{
	private static final Logger log = LoggerFactory.getLogger(TrafficStatistics.class);
	private double unknownTotal;

	private double dataUpload;
	private double dataDownload;

	private double tunnelRequestsUpload;
	private double tunnelRequestsDownload;

	private double searchRequestsUpload;
	private double searchRequestsDownload;

	private double totalUpload;
	private double totalDownload;

	public TrafficStatistics()
	{
	}

	private TrafficStatistics(TrafficStatistics from)
	{
		unknownTotal = from.unknownTotal;

		dataUpload = from.dataUpload;
		dataDownload = from.dataDownload;

		tunnelRequestsUpload = from.tunnelRequestsUpload;
		tunnelRequestsDownload = from.tunnelRequestsDownload;

		searchRequestsUpload = from.searchRequestsUpload;
		searchRequestsDownload = from.searchRequestsDownload;

		totalUpload = from.totalUpload;
		totalDownload = from.totalDownload;
	}

	public void reset()
	{
		unknownTotal = 0.0;

		dataUpload = 0.0;
		dataDownload = 0.0;

		tunnelRequestsUpload = 0.0;
		tunnelRequestsDownload = 0.0;

		searchRequestsUpload = 0.0;
		searchRequestsDownload = 0.0;

		totalUpload = 0.0;
		totalDownload = 0.0;
	}

	public TrafficStatistics multiply(double number)
	{
		var result = new TrafficStatistics(this);

		result.unknownTotal *= number;

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

	public TrafficStatistics add(double number)
	{
		var result = new TrafficStatistics(this);

		result.unknownTotal += number;

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

	public TrafficStatistics add(TrafficStatistics other)
	{
		var result = new TrafficStatistics(this);

		result.unknownTotal += other.unknownTotal;

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

	public void addToTunnelRequestsDownload(int size)
	{
		tunnelRequestsDownload += size;
	}

	public void addToTunnelRequestsUpload(int size)
	{
		tunnelRequestsUpload += size;
	}

	public void addToSearchRequestsDownload(int size)
	{
		searchRequestsDownload += size;
	}

	public void addToSearchRequestsUpload(int size)
	{
		searchRequestsUpload += size;
	}

	public void addToUnknownTotal(int size)
	{
		unknownTotal += size;
	}

	public void addToDataDownload(int size)
	{
		dataDownload += size;
	}

	public void addToDataUpload(int size)
	{
		dataUpload += size;
	}

	public double getUnknownTotal()
	{
		return unknownTotal;
	}

	public double getDataUpload()
	{
		return dataUpload;
	}

	public double getDataDownload()
	{
		return dataDownload;
	}

	public double getTunnelRequestsUpload()
	{
		return tunnelRequestsUpload;
	}

	public double getTunnelRequestsDownload()
	{
		return tunnelRequestsDownload;
	}

	public double getSearchRequestsUpload()
	{
		return searchRequestsUpload;
	}

	public double getSearchRequestsDownload()
	{
		return searchRequestsDownload;
	}

	public double getTotalUpload()
	{
		return totalUpload;
	}

	public double getTotalDownload()
	{
		return totalDownload;
	}

	@Override
	public String toString()
	{
		return "TrafficStatistics [unknownTotal=" + unknownTotal +
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
