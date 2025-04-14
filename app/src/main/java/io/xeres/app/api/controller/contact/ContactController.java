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

package io.xeres.app.api.controller.contact;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.xeres.app.service.ContactService;
import io.xeres.common.rest.contact.Contact;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static io.xeres.common.rest.PathConfig.CONTACT_PATH;

@Tag(name = "Contact", description = "Contacts")
@RestController
@RequestMapping(value = CONTACT_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class ContactController
{
	private final ContactService contactService;

	public ContactController(ContactService contactService)
	{
		this.contactService = contactService;
	}

	@GetMapping("")
	@Operation(summary = "Gets all the contacts")
	public List<Contact> getContacts()
	{
		return contactService.getContacts();
	}
}
