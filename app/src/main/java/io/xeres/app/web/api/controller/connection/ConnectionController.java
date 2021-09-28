/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.web.api.controller.connection;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.service.LocationService;
import io.xeres.common.dto.profile.ProfileDTO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static io.xeres.app.database.model.profile.ProfileMapper.toDeepDTOs;
import static io.xeres.common.rest.PathConfig.CONNECTIONS_PATH;
import static java.util.function.Predicate.not;

@Tag(name = "Connection", description = "Connected peers", externalDocs = @ExternalDocumentation(url = "https://xeres.io/docs/api/connection", description = "Connection documentation"))
@RestController
@RequestMapping(value = CONNECTIONS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ConnectionController
{
	private final LocationService locationService;

	public ConnectionController(LocationService locationService)
	{
		this.locationService = locationService;
	}

	@GetMapping("/profiles")
	@Operation(summary = "Get all connected profiles")
	@ApiResponse(responseCode = "200", description = "Request completed successfully")
	public List<ProfileDTO> getConnectedProfiles()
	{
		return toDeepDTOs(locationService.getConnectedLocations().stream()
				.filter(not(Location::isOwn))
				.map(Location::getProfile)
				.toList());
	}
}
