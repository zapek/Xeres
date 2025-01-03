/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.api.controller.statistics;

import io.xeres.app.xrs.service.turtle.TurtleStatistics;
import io.xeres.common.rest.statistics.TurtleStatisticsResponse;

final class StatisticsMapper
{
	private StatisticsMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static TurtleStatisticsResponse toDTO(TurtleStatistics turtleStatistics)
	{
		if (turtleStatistics == null)
		{
			return null;
		}
		return new TurtleStatisticsResponse(
				turtleStatistics.getForwardTotal(),
				turtleStatistics.getDataUpload(),
				turtleStatistics.getDataDownload(),
				turtleStatistics.getTunnelRequestsUpload(),
				turtleStatistics.getTunnelRequestsDownload(),
				turtleStatistics.getSearchRequestsUpload(),
				turtleStatistics.getSearchRequestsDownload(),
				turtleStatistics.getTotalUpload(),
				turtleStatistics.getTotalDownload()
		);
	}
}
