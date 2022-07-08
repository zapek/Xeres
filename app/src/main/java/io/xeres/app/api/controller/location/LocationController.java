/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.app.api.controller.location;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.api.error.Error;
import io.xeres.app.service.LocationService;
import io.xeres.common.dto.location.LocationDTO;
import io.xeres.common.rest.location.RSIdResponse;
import io.xeres.common.rsid.Type;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static io.xeres.app.database.model.location.LocationMapper.toDTO;
import static io.xeres.common.rest.PathConfig.LOCATIONS_PATH;

@Tag(name = "Location", description = "Local instance", externalDocs = @ExternalDocumentation(url = "https://xeres.io/docs/api/location", description = "Location documentation"))
@RestController
@RequestMapping(value = LOCATIONS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class LocationController
{
	private final LocationService locationService;

	public LocationController(LocationService locationService)
	{
		this.locationService = locationService;
	}

	@GetMapping("/{id}")
	@Operation(summary = "Return a location")
	@ApiResponse(responseCode = "200", description = "Location found")
	@ApiResponse(responseCode = "404", description = "Location not found", content = @Content(schema = @Schema(implementation = Error.class)))
	public LocationDTO findLocationById(@PathVariable long id)
	{
		return toDTO(locationService.findLocationById(id).orElseThrow());
	}

	@GetMapping("/{id}/rsId")
	@Operation(summary = "Return a location's RSId")
	@ApiResponse(responseCode = "200", description = "Location found")
	@ApiResponse(responseCode = "404", description = "Profile not found", content = @Content(schema = @Schema(implementation = Error.class)))
	public RSIdResponse getRSIdOfLocation(@PathVariable long id, @RequestParam(value = "type", required = false) Type type)
	{
		var location = locationService.findLocationById(id).orElseThrow();

		return new RSIdResponse(location.getProfile().getName(), location.getName(), location.getRsId(type == null ? Type.ANY : type).getArmored());
	}
}
