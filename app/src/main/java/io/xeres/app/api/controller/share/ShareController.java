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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.service.file.FileService;
import io.xeres.common.dto.share.ShareDTO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
