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

package io.xeres.app.api.controller.share;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.file.FileFakes;
import io.xeres.app.database.model.share.Share;
import io.xeres.app.service.file.FileService;
import io.xeres.common.dto.share.ShareDTO;
import io.xeres.common.pgp.Trust;
import io.xeres.common.rest.share.TemporaryShareRequest;
import io.xeres.common.rest.share.UpdateShareRequest;
import io.xeres.testutils.Sha1SumFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static io.xeres.common.rest.PathConfig.SHARES_PATH;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShareController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShareControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = SHARES_PATH;

	@MockitoBean
	private FileService fileService;

	@Autowired
	private MockMvc mvc;

	@Test
	void GetShares_Success() throws Exception
	{
		var share = Share.createShare("foo", FileFakes.createFile("test"), true, Trust.FULL);
		share.setId(1L);

		when(fileService.getShares()).thenReturn(List.of(share));
		when(fileService.getFilesMapFromShares(any())).thenReturn(Map.of(share.getId(), "foo/bar"));

		mvc.perform(getJson(BASE_URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id", is(1)))
				.andExpect(jsonPath("$[0].path", is("foo/bar")));

		verify(fileService).getShares();
		verify(fileService).getFilesMapFromShares(any());
	}

	@Test
	void CreateAndUpdateShares_Success() throws Exception
	{
		var shareDTO = new ShareDTO(1L, "foo", "foo/bar", true, Trust.FULL, Instant.now());
		var updateRequest = new UpdateShareRequest(List.of(shareDTO));

		mvc.perform(postJson(BASE_URL, updateRequest))
				.andExpect(status().isCreated());

		verify(fileService).synchronize(any());
	}

	@Test
	void ShareTemporarily_Success() throws Exception
	{
		var filePath = "/tmp/test.txt";
		var temporaryShareRequest = new TemporaryShareRequest(filePath);
		var path = Path.of(filePath);
		var hash = Sha1SumFakes.createSha1Sum();

		when(fileService.calculateTemporaryFileHash(path)).thenReturn(hash);

		mvc.perform(postJson(BASE_URL + "/temporary", temporaryShareRequest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hash", is(hash.toString())));

		verify(fileService).calculateTemporaryFileHash(path);
	}

	@Test
	void ShareTemporarily_HashCalculationFails() throws Exception
	{
		var filePath = "/tmp/test.txt";
		var temporaryShareRequest = new TemporaryShareRequest(filePath);
		var path = Path.of(filePath);

		when(fileService.calculateTemporaryFileHash(path)).thenReturn(null);

		mvc.perform(postJson(BASE_URL + "/temporary", temporaryShareRequest))
				.andExpect(status().isInternalServerError());

		verify(fileService).calculateTemporaryFileHash(path);
	}
}