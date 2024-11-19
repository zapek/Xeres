/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.app.api.controller.geoip;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.service.GeoIpService;
import io.xeres.common.rest.geoip.CountryResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.MediaType;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

import static io.xeres.common.rest.PathConfig.GEOIP_PATH;

@Tag(name = "GeoIP", description = "GeoIP lookups", externalDocs = @ExternalDocumentation(url = "https://xeres.io/docs/api/geoip", description = "GeoIP documentation"))
@RestController
@RequestMapping(value = GEOIP_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class GeoIpController
{
	private final GeoIpService geoIpService;

	public GeoIpController(GeoIpService geoIpService)
	{
		this.geoIpService = geoIpService;
	}

	@GetMapping("/{ip}")
	@Operation(summary = "Get the ISO country code of the IP address.")
	@ApiResponse(responseCode = "200", description = "Request successful")
	@ApiResponse(responseCode = "404", description = "No country found for IP address", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	public CountryResponse getIsoCountry(@PathVariable String ip)
	{
		var country = geoIpService.getCountry(ip);
		if (country == null)
		{
			throw new EntityNotFoundException();
		}
		return new CountryResponse(country.name().toLowerCase(Locale.ROOT));
	}
}
