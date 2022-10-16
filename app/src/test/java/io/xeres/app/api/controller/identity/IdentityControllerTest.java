package io.xeres.app.api.controller.identity;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.gxs.IdentityGroupItemFakes;
import io.xeres.app.service.IdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IdentityController.class)
class IdentityControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = IDENTITIES_PATH;

	@MockBean
	private IdentityService identityService;

	@Autowired
	public MockMvc mvc;

	@Test
	void IdentityController_FindIdentityById_OK() throws Exception
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
	void IdentityController_FindIdentityById_NotFound() throws Exception
	{
		var id = 1L;

		when(identityService.findById(id)).thenThrow(new NoSuchElementException());

		mvc.perform(getJson(BASE_URL + "/" + id))
				.andExpect(status().isNotFound());

		verify(identityService).findById(id);
	}

	@Test
	void IdentityController_DownloadIdentityImage_Empty() throws Exception
	{
		var id = 1L;
		var identity = IdentityGroupItemFakes.createIdentityGroupItem();

		when(identityService.findById(id)).thenReturn(Optional.of(identity));

		mvc.perform(getJson(BASE_URL + "/" + id + "/image"))
				.andExpect(status().isNoContent());

		verify(identityService).findById(id);
	}

	@Test
	void IdentityController_DownloadIdentityImage_OK() throws Exception
	{
		var id = 1L;
		var identity = IdentityGroupItemFakes.createIdentityGroupItem();
		identity.setImage(Objects.requireNonNull(getClass().getResourceAsStream("/image/leguman.jpg")).readAllBytes());

		when(identityService.findById(id)).thenReturn(Optional.of(identity));

		mvc.perform(getJson(BASE_URL + "/" + id + "/image"))
				.andExpect(status().isOk())
				.andExpect(header().string(CONTENT_TYPE, "image/jpeg"));

		verify(identityService).findById(id);
	}

	@Test
	void IdentityController_UploadIdentityImage_OK() throws Exception
	{
		var id = 1L;

		mvc.perform(postJson(BASE_URL + "/" + id + "/image", null))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + IDENTITIES_PATH + "/" + id + "/image"));

		verify(identityService).saveIdentityImage(eq(id), any());
	}

	@Test
	void IdentityController_DeleteIdentityImage_OK() throws Exception
	{
		var id = 1L;

		mvc.perform(delete(BASE_URL + "/" + id + "/image"))
				.andExpect(status().isNoContent());

		verify(identityService).deleteIdentityImage(id);
	}

	@Test
	void IdentityController_FindIdentities_ByName() throws Exception
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
	void IdentityController_FindIdentities_ByGxsId() throws Exception
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
	void IdentityController_FindIdentities_ByType() throws Exception
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
	void IdentityController_FindIdentities_All() throws Exception
	{
		var identity = IdentityGroupItemFakes.createIdentityGroupItem();
		identity.setId(1L);

		when(identityService.getAll()).thenReturn(List.of(identity));

		mvc.perform(getJson(BASE_URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0].id").value(is(identity.getId()), Long.class));

		verify(identityService).getAll();
	}
}
