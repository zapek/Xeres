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

package io.xeres.app.api.controller.share;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.service.file.FileService;
import io.xeres.common.dto.share.ShareDTO;
import io.xeres.common.rest.Error;
import io.xeres.common.rest.share.UpdateShareRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static io.xeres.app.database.model.share.ShareMapper.fromDTOs;
import static io.xeres.app.database.model.share.ShareMapper.toDTOs;
import static io.xeres.common.rest.PathConfig.SHARES_PATH;

@Tag(name = "Share", description = "File shares", externalDocs = @ExternalDocumentation(url = "https://xeres.io/docs/api/share", description = "Shares documentation"))
@RestController
@RequestMapping(value = SHARES_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ShareController
{
	private final FileService fileService;

	public ShareController(FileService fileService)
	{
		this.fileService = fileService;
	}

	@GetMapping
	@Operation(summary = "Return all shares", description = "Return all configured shares")
	@ApiResponse(responseCode = "200", description = "All shares")
	public List<ShareDTO> getShares()
	{
		var shares = fileService.getShares();
		return toDTOs(shares, fileService.getFilesMapFromShares(shares));
	}

	@PostMapping
	@Operation(summary = "Add/Update shares")
	@ApiResponse(responseCode = "201", description = "Shares created/updated successfully")
	@ApiResponse(responseCode = "422", description = "Shares cannot be processed", content = @Content(schema = @Schema(implementation = Error.class)))
	@ApiResponse(responseCode = "500", description = "Serious error", content = @Content(schema = @Schema(implementation = Error.class)))
	public ResponseEntity<Void> createAndUpdateShares(@Valid @RequestBody UpdateShareRequest updateSharesRequest)
	{
		fileService.synchronize(fromDTOs(updateSharesRequest.shares()));
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@PostMapping("/temporary")
	@Operation(summary = "Add a file to share temporarily")
	@ApiResponse(responseCode = "200", description = "File added to temporary share successfully")
	public String shareTemporarily(@Valid @RequestBody String filePath) throws IOException
	{
		var path = Paths.get(filePath);
		var hash = fileService.calculateTemporaryFileHash(path);

		if (hash == null)
		{
			throw new IOException("Cannot compute hash of file");
		}
		return hash.toString();
	}
}
