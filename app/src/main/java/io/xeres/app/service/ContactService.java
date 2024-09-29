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

package io.xeres.app.service;

import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.common.rest.contact.Contact;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ContactService
{
	private final ProfileService profileService;
	private final LocationService locationService;
	private final IdentityRsService identityRsService;

	public ContactService(ProfileService profileService, LocationService locationService, IdentityRsService identityRsService)
	{
		this.profileService = profileService;
		this.locationService = locationService;
		this.identityRsService = identityRsService;
	}

	@Transactional(readOnly = true)
	public List<Contact> getContacts()
	{
		var profiles = profileService.getAllProfiles();
		var identities = identityRsService.getAll();

		// XXX: for now return all of them, in the future it would be possible to merge the identity to the profile (if the name is the same)
		List<Contact> contacts = new ArrayList<>(profiles.size() + identities.size());
		profiles.forEach(profile -> contacts.add(new Contact(profile.getName(), profile.getId(), 0L)));
		identities.forEach(identity -> contacts.add(new Contact(identity.getName(), 0L, identity.getId()))); // XXX: put the profile too
		return contacts;
	}
}
