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

package io.xeres.app.api.controller.identity;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.gxs.IdentityGroupItemFakes;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.identicon.IdenticonService;
import io.xeres.app.service.notification.contact.ContactNotificationService;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IdentityController.class)
@AutoConfigureMockMvc(addFilters = false)
class IdentityControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = IDENTITIES_PATH;

	@MockitoBean
	private IdentityService identityService;

	@MockitoBean
	private IdentityRsService identityRsService;

	@MockitoBean
	private ContactNotificationService contactNotificationService;

	@MockitoBean
	private IdenticonService identiconService;

	@Test
	void FindIdentityById_Success() throws Exception
	{
		var identity = IdentityGroupItemFakes.createIdentityGroupItem();
		identity.setId(1L);

		when(identityService.findById(identity.getId())).thenReturn(Optional.of(identity));

		mvc.perform(getJson(BASE_URL + "/" + identity.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(is(identity.getId()), Long.class));

		verify(identityService).findById(identity.getId());
	}

	@Test
	void FindIdentityById_NotFound_Failure() throws Exception
	{
		var id = 1L;

		when(identityService.findById(id)).thenThrow(new NoSuchElementException());

		mvc.perform(getJson(BASE_URL + "/" + id))
				.andExpect(status().isNotFound());

		verify(identityService).findById(id);
	}

	@Test
	void DownloadIdentityImage_Empty_Success() throws Exception
	{
		var id = 1L;
		var identity = IdentityGroupItemFakes.createIdentityGroupItem();

		when(identityService.findById(id)).thenReturn(Optional.of(identity));
		when(identiconService.getIdenticon(any())).thenReturn(Objects.requireNonNull(IdentityControllerTest.class.getResourceAsStream("/image/leguman.jpg")).readAllBytes());

		mvc.perform(get(BASE_URL + "/" + id + "/image", MediaType.IMAGE_JPEG))
				.andExpect(status().isOk())
				.andExpect(header().string(CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE));

		verify(identityService).findById(id);
	}

	@Test
	void DownloadIdentityImage_Success() throws Exception
	{
		var id = 1L;
		var identity = IdentityGroupItemFakes.createIdentityGroupItem();
		identity.setImage(Objects.requireNonNull(IdentityControllerTest.class.getResourceAsStream("/image/leguman.jpg")).readAllBytes());

		when(identityService.findById(id)).thenReturn(Optional.of(identity));

		mvc.perform(get(BASE_URL + "/" + id + "/image", MediaType.IMAGE_JPEG))
				.andExpect(status().isOk())
				.andExpect(header().string(CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE));

		verify(identityService).findById(id);
	}

	@Test
	void UploadIdentityImage_Success() throws Exception
	{
		var identity = IdentityGroupItemFakes.createIdentityGroupItem();
		identity.setId(1L);

		when(identityRsService.saveOwnIdentityImage(eq(identity.getId()), any())).thenReturn(identity);

		mvc.perform(post(BASE_URL + "/" + identity.getId() + "/image")
						.contentType(MediaType.MULTIPART_FORM_DATA)
						.accept(MediaType.APPLICATION_JSON)
						.content(""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + IDENTITIES_PATH + "/" + identity.getId() + "/image"));

		verify(identityRsService).saveOwnIdentityImage(eq(identity.getId()), any());
	}

	@Test
	void DeleteIdentityImage_Success() throws Exception
	{
		var identity = IdentityGroupItemFakes.createIdentityGroupItem();
		identity.setId(1L);

		when(identityRsService.deleteOwnIdentityImage(identity.getId())).thenReturn(identity);

		mvc.perform(delete(BASE_URL + "/" + identity.getId() + "/image"))
				.andExpect(status().isNoContent());

		verify(identityRsService).deleteOwnIdentityImage(identity.getId());
	}

	@Test
	void FindIdentities_ByName_Success() throws Exception
	{
		var identity = IdentityGroupItemFakes.createIdentityGroupItem();
		identity.setId(1L);

		when(identityService.findAllByName(identity.getName())).thenReturn(List.of(identity));

		mvc.perform(getJson(BASE_URL + "?name=" + identity.getName()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0].id").value(is(identity.getId()), Long.class));

		verify(identityService).findAllByName(identity.getName());
	}

	@Test
	void FindIdentities_ByGxsId_Success() throws Exception
	{
		var identity = IdentityGroupItemFakes.createIdentityGroupItem();
		identity.setId(1L);

		when(identityService.findByGxsId(identity.getGxsId())).thenReturn(Optional.of(identity));

		mvc.perform(getJson(BASE_URL + "?gxsId=" + identity.getGxsId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0].id").value(is(identity.getId()), Long.class));

		verify(identityService).findByGxsId(identity.getGxsId());
	}

	@Test
	void FindIdentities_ByType_Success() throws Exception
	{
		var identity = IdentityGroupItemFakes.createIdentityGroupItem();
		identity.setId(1L);

		when(identityService.findAllByType(identity.getType())).thenReturn(List.of(identity));

		mvc.perform(getJson(BASE_URL + "?type=" + identity.getType()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0].id").value(is(identity.getId()), Long.class));

		verify(identityService).findAllByType(identity.getType());
	}

	@Test
	void FindIdentities_All_Success() throws Exception
	{
		var identity = IdentityGroupItemFakes.createIdentityGroupItem();
		identity.setId(1L);

		when(identityService.getAll()).thenReturn(List.of(identity));

		mvc.perform(getJson(BASE_URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0].id").value(is(identity.getId()), Long.class));

		verify(identityService).getAll();
	}

	@Test
	void DownloadImageByGxsId_Found_Success() throws Exception
	{
		var identity = IdentityGroupItemFakes.createIdentityGroupItem();
		identity.setId(1L);
		identity.setImage(Objects.requireNonNull(IdentityControllerTest.class.getResourceAsStream("/image/leguman.jpg")).readAllBytes());

		when(identityService.findByGxsId(identity.getGxsId())).thenReturn(Optional.of(identity));

		mvc.perform(get(BASE_URL + "/image", MediaType.IMAGE_JPEG)
						.param("gxsId", identity.getGxsId().toString())
						.param("find", "true"))
				.andExpect(status().isOk())
				.andExpect(header().string(CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE));

		verify(identityService).findByGxsId(identity.getGxsId());
	}

	@Test
	void DownloadImageByGxsId_Identicon_Success() throws Exception
	{
		var identity = IdentityGroupItemFakes.createIdentityGroupItem();
		identity.setId(1L);

		when(identiconService.getIdenticon(any())).thenReturn(Objects.requireNonNull(IdentityControllerTest.class.getResourceAsStream("/image/leguman.jpg")).readAllBytes());

		mvc.perform(get(BASE_URL + "/image", MediaType.IMAGE_JPEG)
						.param("gxsId", identity.getGxsId().toString()))
				.andExpect(status().isOk())
				.andExpect(header().string(CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE));

		verify(identiconService).getIdenticon(any());
	}
}
