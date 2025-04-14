/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

package io.xeres.app.api.controller.connection;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.job.PeerConnectionJob;
import io.xeres.app.service.LocationService;
import io.xeres.common.dto.profile.ProfileDTO;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.rest.connection.ConnectionRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static io.xeres.app.database.model.profile.ProfileMapper.toDeepDTOs;
import static io.xeres.common.rest.PathConfig.CONNECTIONS_PATH;
import static java.util.function.Predicate.not;

@Tag(name = "Connection", description = "Connected peers")
@RestController
@RequestMapping(value = CONNECTIONS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ConnectionController
{
	private final LocationService locationService;
	private final PeerConnectionJob peerConnectionJob;

	public ConnectionController(LocationService locationService, PeerConnectionJob peerConnectionJob)
	{
		this.locationService = locationService;
		this.peerConnectionJob = peerConnectionJob;
	}

	@GetMapping("/profiles")
	@Operation(summary = "Gets all currently connected profiles")
	public List<ProfileDTO> getConnectedProfiles()
	{
		return toDeepDTOs(locationService.getConnectedLocations().stream()
				.filter(not(Location::isOwn))
				.map(Location::getProfile)
				.toList());
	}

	@PutMapping("/connect")
	@Operation(summary = "Attempts to connect to a location")
	@ApiResponse(responseCode = "200", description = "Request completed successfully")
	@ApiResponse(responseCode = "404", description = "No location found for given identifier", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public ResponseEntity<Void> connect(@Valid @RequestBody ConnectionRequest connectionRequest)
	{
		var location = locationService.findLocationByLocationIdentifier(LocationIdentifier.fromString(connectionRequest.locationIdentifier())).orElseThrow();
		peerConnectionJob.connectImmediately(location, connectionRequest.connectionIndex());
		return ResponseEntity.ok().build();
	}
}
