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

package io.xeres.app.api.controller.statistics;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.xrs.service.turtle.TurtleRsService;
import io.xeres.common.rest.statistics.TurtleStatisticsResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static io.xeres.app.api.controller.statistics.StatisticsMapper.toDTO;
import static io.xeres.common.rest.PathConfig.STATISTICS_PATH;

@Tag(name = "Statistics", description = "Statistics service", externalDocs = @ExternalDocumentation(url = "https://xeres.io/docs/api/statistics", description = "Statistics documentation"))
@RestController
@RequestMapping(value = STATISTICS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class StatisticsController
{
	private final TurtleRsService turtleRsService;

	public StatisticsController(TurtleRsService turtleRsService)
	{
		this.turtleRsService = turtleRsService;
	}

	@GetMapping("/turtle")
	@Operation(summary = "Get turtle statistics")
	@ApiResponse(responseCode = "200", description = "Request successful")
	public TurtleStatisticsResponse getTurtleStatistics()
	{
		return toDTO(turtleRsService.getStatistics());
	}
}
