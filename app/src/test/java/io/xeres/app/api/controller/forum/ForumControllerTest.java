/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.app.api.controller.forum;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.database.model.gxs.ForumGroupItemFakes;
import io.xeres.app.service.ForumMessageService;
import io.xeres.app.xrs.service.forum.ForumRsService;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.common.rest.forum.UpdateForumMessagesReadRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static io.xeres.common.rest.PathConfig.FORUMS_PATH;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ForumController.class)
@AutoConfigureMockMvc(addFilters = false)
class ForumControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = FORUMS_PATH;

	@MockBean
	private ForumRsService forumRsService;

	@MockBean
	private IdentityRsService identityRsService;

	@MockBean
	private ForumMessageService forumMessageService;

	@Autowired
	public MockMvc mvc;

	@Test
	void ForumController_GetForumsGroups() throws Exception
	{
		var forumGroups = List.of(ForumGroupItemFakes.createForumGroupItem(), ForumGroupItemFakes.createForumGroupItem());

		when(forumRsService.findAllGroups()).thenReturn(forumGroups);

		mvc.perform(getJson(BASE_URL + "/groups"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0].id").value(is(forumGroups.get(0).getId()), Long.class))
				.andExpect(jsonPath("$.[0].name", is(forumGroups.get(0).getName())))
				.andExpect(jsonPath("$.[1].id").value(is(forumGroups.get(1).getId()), Long.class));

		verify(forumRsService).findAllGroups();
	}

	@Test
	void ForumController_UpdateMessagesReadFlag_OK() throws Exception
	{
		var ids = Map.of(1L, true, 2L, true, 3L, false);
		var request = new UpdateForumMessagesReadRequest(ids);

		mvc.perform(patchJson(BASE_URL + "/messages", request))
				.andExpect(status().isOk());

		verify(forumRsService).setForumMessagesAsRead(ids);
	}
}
