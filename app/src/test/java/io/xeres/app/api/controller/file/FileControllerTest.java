/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.xrs.service.filetransfer.FileTransferRsService;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.rest.file.FileDownloadRequest;
import io.xeres.common.rest.file.FileProgress;
import io.xeres.common.rest.file.FileSearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static io.xeres.common.rest.PathConfig.FILES_PATH;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FileController.class)
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = FILES_PATH;

	@MockitoBean
	private FileTransferRsService fileTransferRsService;

	@Test
	void Search_Success() throws Exception
	{
		var searchName = "cool stuff";
		var searchRequest = new FileSearchRequest(searchName);

		when(fileTransferRsService.turtleSearch(searchName)).thenReturn(1);

		mvc.perform(postJson(BASE_URL + "/search", searchRequest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(1)));

		verify(fileTransferRsService).turtleSearch(searchName);
	}

	@Test
	void Download_Success() throws Exception
	{
		var downloadId = 123L;
		var request = new FileDownloadRequest("test.txt", "0123456789abcdef0123456789abcdef01234567", 1024L, LocationFakes.createLocation().getLocationIdentifier());

		when(fileTransferRsService.download(eq(request.name()), any(Sha1Sum.class), eq(request.size()), eq(request.locationIdentifier()))).thenReturn(downloadId);

		mvc.perform(postJson(BASE_URL + "/download", request))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", is((int) downloadId)));
	}

	@Test
	void Download_InvalidHash_Failure() throws Exception
	{
		var request = new FileDownloadRequest("test.txt", "invalid_hash", 1024L, LocationFakes.createLocation().getLocationIdentifier());

		mvc.perform(postJson(BASE_URL + "/download", request))
				.andExpect(status().isBadRequest());
	}

	@Test
	void GetDownloads_Success() throws Exception
	{
		var progress = new FileProgress(1L, "test.txt", 2048L, 8192L, "0123456789abcdef0123456789abcdef01234567", false);
		when(fileTransferRsService.getDownloadStatistics()).thenReturn(List.of(progress));

		mvc.perform(getJson(BASE_URL + "/downloads"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is(1)))
				.andExpect(jsonPath("$[0].name", is("test.txt")))
				.andExpect(jsonPath("$[0].currentSize", is(2048)))
				.andExpect(jsonPath("$[0].totalSize", is(8192)))
				.andExpect(jsonPath("$[0].hash", is("0123456789abcdef0123456789abcdef01234567")))
				.andExpect(jsonPath("$[0].completed", is(false)));

		verify(fileTransferRsService).getDownloadStatistics();
	}

	@Test
	void GetUploads_Success() throws Exception
	{
		var progress = new FileProgress(1L, "test.txt", 2048L, 8192L, "0123456789abcdef0123456789abcdef01234567", false);
		when(fileTransferRsService.getUploadStatistics()).thenReturn(List.of(progress));

		mvc.perform(getJson(BASE_URL + "/uploads"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is(1)))
				.andExpect(jsonPath("$[0].name", is("test.txt")))
				.andExpect(jsonPath("$[0].currentSize", is(2048)))
				.andExpect(jsonPath("$[0].totalSize", is(8192)))
				.andExpect(jsonPath("$[0].hash", is("0123456789abcdef0123456789abcdef01234567")))
				.andExpect(jsonPath("$[0].completed", is(false)));

		verify(fileTransferRsService).getUploadStatistics();
	}

	@Test
	void RemoveDownload_Success() throws Exception
	{
		var downloadId = 123L;

		mvc.perform(delete(BASE_URL + "/downloads/" + downloadId))
				.andExpect(status().isNoContent());

		verify(fileTransferRsService).removeDownload(downloadId);
	}
}