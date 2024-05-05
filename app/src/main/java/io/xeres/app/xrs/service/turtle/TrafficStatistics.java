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

import java.util.List;

/**
 * Everything is in bytes per seconds.
 */
class TrafficStatistics
{
	private double unknownTotal;

	private double dataUpload;
	private double dataDownload;

	private double tunnelRequestsUpload;
	private double tunnelRequestsDownload;

	private double totalUpload;
	private double totalDownload;

	private List<Double> forwardProbabilities;

	public void reset()
	{
		unknownTotal = 0.0;

		dataUpload = 0.0;
		dataDownload = 0.0;

		tunnelRequestsUpload = 0.0;
		tunnelRequestsDownload = 0.0;

		totalUpload = 0.0;
		totalDownload = 0.0;

		// XXX: no reset of forwardProbabilities? check what RS does with it...
	}

	public TrafficStatistics multiply(double number)
	{
		unknownTotal *= number;

		dataUpload *= number;
		dataDownload *= number;

		tunnelRequestsUpload *= number;
		tunnelRequestsDownload *= number;

		totalUpload *= number;
		totalDownload *= number;

		return this;
	}

	// XXX: remove if not needed...
	public TrafficStatistics add(double number)
	{
		unknownTotal += number;

		dataUpload += number;
		dataDownload += number;

		tunnelRequestsUpload += number;
		tunnelRequestsDownload += number;

		totalUpload += number;
		totalDownload += number;

		return this;
	}

	public TrafficStatistics add(TrafficStatistics other)
	{
		unknownTotal += other.getUnknownTotal();

		dataUpload += other.getDataUpload();
		dataDownload += other.getDataDownload();

		tunnelRequestsUpload += other.getTunnelRequestsUpload();
		tunnelRequestsDownload += other.getTunnelRequestsDownload();

		totalUpload += other.getTotalUpload();
		totalDownload += other.getTotalDownload();

		return this;
	}

	public void addToTunnelRequestsDownload(int size)
	{
		tunnelRequestsDownload += size;
	}

	public void addToTunnelRequestsUpload(int size)
	{
		tunnelRequestsUpload += size;
	}

	public void addToUnknownTotal(int size)
	{
		unknownTotal += size;
	}

	public void addToDataDownload(int size)
	{
		dataDownload += size;
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

	public double getTotalUpload()
	{
		return totalUpload;
	}

	public double getTotalDownload()
	{
		return totalDownload;
	}
}
