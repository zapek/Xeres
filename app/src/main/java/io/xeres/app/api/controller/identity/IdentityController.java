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

package io.xeres.app.api.controller.identity;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.api.error.Error;
import io.xeres.app.service.IdentityService;
import io.xeres.common.dto.identity.IdentityDTO;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Id;
import io.xeres.common.identity.Type;
import io.xeres.common.util.ImageDetectionUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static io.xeres.app.database.model.identity.IdentityMapper.toDTO;
import static io.xeres.app.database.model.identity.IdentityMapper.toGxsIdDTOs;
import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Tag(name = "Identities", description = "Identities", externalDocs = @ExternalDocumentation(url = "https://xeres.io/docs/api/identities", description = "Identities documentation"))
@RestController
@RequestMapping(value = IDENTITIES_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class IdentityController
{
	private final IdentityService identityService;

	public IdentityController(IdentityService identityService)
	{
		this.identityService = identityService;
	}

	@GetMapping("/{id}")
	@Operation(summary = "Return an identity")
	@ApiResponse(responseCode = "200", description = "Identity found")
	@ApiResponse(responseCode = "404", description = "Identity not found", content = @Content(schema = @Schema(implementation = Error.class)))
	public IdentityDTO findIdentityById(@PathVariable long id)
	{
		return toDTO(identityService.findById(id).orElseThrow());
	}

	@GetMapping("/{id}/image")
	@Operation(summary = "Return an identity's avatar image")
	@ApiResponse(responseCode = "200", description = "Identity's avatar image found")
	@ApiResponse(responseCode = "204", description = "Identity's avatar image is empty")
	@ApiResponse(responseCode = "404", description = "Identity not found", content = @Content(schema = @Schema(implementation = Error.class)))
	public ResponseEntity<InputStreamResource> downloadIdentityImage(@PathVariable long id)
	{
		var identity = identityService.findById(id).orElseThrow();
		var imageType = ImageDetectionUtils.getImageMimeType(identity.getImage());
		if (imageType == null)
		{
			return ResponseEntity.noContent()
					.build();
		}
		return ResponseEntity.ok()
				.contentLength(identity.getImage().length)
				.contentType(imageType)
				.body(new InputStreamResource(new ByteArrayInputStream(identity.getImage())));
	}

	@PostMapping("/{id}/image")
	@Operation(summary = "Change an identity's avatar image")
	@ApiResponse(responseCode = "201", description = "Identity's avatar image created")
	@ApiResponse(responseCode = "404", description = "Identity not found", content = @Content(schema = @Schema(implementation = Error.class)))
	@ApiResponse(responseCode = "415", description = "Image's media type unsupported", content = @Content(schema = @Schema(implementation = Error.class)))
	@ApiResponse(responseCode = "422", description = "Image unprocessable", content = @Content(schema = @Schema(implementation = Error.class)))
	public ResponseEntity<Void> uploadIdentityImage(@PathVariable long id, @RequestBody MultipartFile file) throws IOException
	{
		identityService.saveIdentityImage(id, file);

		var location = ServletUriComponentsBuilder.fromCurrentRequest().replacePath(IDENTITIES_PATH + "/{id}/image").buildAndExpand(id).toUri();
		return ResponseEntity.created(location).build();
	}

	@DeleteMapping("/{id}/image")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteIdentityImage(@PathVariable long id)
	{
		identityService.deleteIdentityImage(id);
	}

	@GetMapping
	@Operation(summary = "Search all identities", description = "If no search parameters are provided, return all identities")
	@ApiResponse(responseCode = "200", description = "All matched identities")
	public List<IdentityDTO> findIdentities(
			@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "gxsId", required = false) String gxsId,
			@RequestParam(value = "type", required = false) Type type)
	{
		if (isNotBlank(name))
		{
			return toGxsIdDTOs(identityService.findAllByName(name));
		}
		else if (isNotBlank(gxsId))
		{
			var identity = identityService.findByGxsId(new GxsId(Id.toBytes(gxsId)));
			return identity.map(id -> List.of(toDTO(id))).orElse(Collections.emptyList());
		}
		else if (type != null)
		{
			return toGxsIdDTOs(identityService.findAllByType(type));
		}
		return toGxsIdDTOs(identityService.getAll());
	}
}
