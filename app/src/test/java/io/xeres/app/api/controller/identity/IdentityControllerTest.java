package io.xeres.app.api.controller.identity;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.gxs.GxsIdGroupItemFakes;
import io.xeres.app.database.model.identity.GxsIdFakes;
import io.xeres.app.service.IdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
		var id = 1L;
		var identity = GxsIdGroupItemFakes.createGxsIdGroupItem();

		when(identityService.findById(id)).thenReturn(Optional.of(identity));

		mvc.perform(getJson(BASE_URL + "/" + id))
				.andExpect(status().isOk());

		verify(identityService).findById(id);
	}
}
