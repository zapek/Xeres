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

package io.xeres.app.api.controller.contact;

import io.xeres.app.api.controller.AbstractControllerTest;
import io.xeres.app.service.ContactService;
import io.xeres.common.location.Availability;
import io.xeres.common.rest.contact.Contact;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static io.xeres.common.rest.PathConfig.CONTACT_PATH;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactController.class)
@AutoConfigureMockMvc(addFilters = false)
class ContactControllerTest extends AbstractControllerTest
{
	private static final String BASE_URL = CONTACT_PATH;

	@MockitoBean
	private ContactService contactService;

	@Test
	void GetContacts_Success() throws Exception
	{
		var contacts = List.of(
				new Contact("foo", 1L, 1L, Availability.AVAILABLE, true),
				new Contact("bar", 2L, 2L, Availability.BUSY, true)
		);

		when(contactService.getContacts()).thenReturn(contacts);

		mvc.perform(getJson(BASE_URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0].name").value("foo"));

		verify(contactService).getContacts();
	}
}