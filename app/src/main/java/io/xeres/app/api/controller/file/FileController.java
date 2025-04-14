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

package io.xeres.app.api.controller.file;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.xrs.service.filetransfer.FileTransferRsService;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.rest.file.FileDownloadRequest;
import io.xeres.common.rest.file.FileProgress;
import io.xeres.common.rest.file.FileSearchRequest;
import io.xeres.common.rest.file.FileSearchResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static io.xeres.common.rest.PathConfig.FILES_PATH;

@Tag(name = "File", description = "File service")
@RestController
@RequestMapping(value = FILES_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class FileController
{
	private final FileTransferRsService fileTransferRsService;

	public FileController(FileTransferRsService fileTransferRsService)
	{
		this.fileTransferRsService = fileTransferRsService;
	}

	@PostMapping("/search")
	@Operation(summary = "Searches for files")
	public FileSearchResponse search(@Valid @RequestBody FileSearchRequest fileSearchRequest)
	{
		return new FileSearchResponse(fileTransferRsService.turtleSearch(fileSearchRequest.name()));
	}

	@PostMapping("/download")
	@Operation(summary = "Downloads a file")
	@ApiResponse(responseCode = "200", description = "Download created successfully")
	@ApiResponse(responseCode = "400", description = "Invalid hash")
	public long download(@RequestBody FileDownloadRequest fileDownloadRequest)
	{
		var hash = Sha1Sum.fromString(fileDownloadRequest.hash());
		if (hash.isNullIdentifier())
		{
			throw new IllegalArgumentException("Invalid hash");
		}
		return fileTransferRsService.download(fileDownloadRequest.name(), hash, fileDownloadRequest.size(), fileDownloadRequest.locationIdentifier());
	}

	@GetMapping("/downloads")
	@Operation(summary = "Shows the current downloads")
	public List<FileProgress> getDownloads()
	{
		return fileTransferRsService.getDownloadStatistics();
	}

	@GetMapping("/uploads")
	@Operation(summary = "Shows the current uploads")
	public List<FileProgress> getUploads()
	{
		return fileTransferRsService.getUploadStatistics();
	}

	@DeleteMapping("/downloads/{id}")
	@Operation(summary = "Removes/cancels a download")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeDownload(@PathVariable long id)
	{
		fileTransferRsService.removeDownload(id);
	}
}
