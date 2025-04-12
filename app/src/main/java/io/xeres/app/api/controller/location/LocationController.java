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

package io.xeres.app.api.controller.location;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.QrCodeService;
import io.xeres.common.dto.location.LocationDTO;
import io.xeres.common.rest.location.RSIdResponse;
import io.xeres.common.rsid.Type;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.awt.image.BufferedImage;

import static io.xeres.app.database.model.location.LocationMapper.toDTO;
import static io.xeres.common.rest.PathConfig.LOCATIONS_PATH;

@Tag(name = "Location", description = "Local instance")
@RestController
@RequestMapping(value = LOCATIONS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class LocationController
{
	private final LocationService locationService;

	private final QrCodeService qrCodeService;

	public LocationController(LocationService locationService, QrCodeService qrCodeService)
	{
		this.locationService = locationService;
		this.qrCodeService = qrCodeService;
	}

	@GetMapping("/{id}")
	@Operation(summary = "Returns a location")
	@ApiResponse(responseCode = "200", description = "Location found")
	@ApiResponse(responseCode = "404", description = "Location not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public LocationDTO findLocationById(@PathVariable long id)
	{
		return toDTO(locationService.findLocationById(id).orElseThrow());
	}

	@GetMapping("/{id}/rs-id")
	@Operation(summary = "Returns a location's RSId")
	@ApiResponse(responseCode = "200", description = "Location found")
	@ApiResponse(responseCode = "404", description = "Profile not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public RSIdResponse getRSIdOfLocation(@PathVariable long id, @RequestParam(value = "type", required = false) Type type)
	{
		var location = locationService.findLocationById(id).orElseThrow();

		return new RSIdResponse(location.getProfile().getName(), location.getSafeName(), location.getRsId(type == null ? Type.ANY : type).getArmored());
	}

	@GetMapping(value = "/{id}/rs-id/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
	@Operation(summary = "Returns a location's RSId as a QR code")
	@ApiResponse(responseCode = "200", description = "Location found")
	@ApiResponse(responseCode = "404", description = "Profile not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public ResponseEntity<BufferedImage> getRSIdOfLocationAsQrCode(@PathVariable long id)
	{
		var location = locationService.findLocationById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)); // Bypass the global controller advice because it only knows about application/json mimetype

		return ResponseEntity.ok(qrCodeService.generateQrCode(location.getRsId(Type.SHORT_INVITE).getArmored()));
	}
}
