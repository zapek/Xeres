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

import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.xrs.service.identity.IdentityServiceStorage;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.location.Availability;
import io.xeres.common.rest.contact.Contact;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContactService
{
	private final ProfileService profileService;
	private final IdentityService identityService;

	public ContactService(@Lazy ProfileService profileService, IdentityService identityService)
	{
		this.profileService = profileService;
		this.identityService = identityService;
	}

	@Transactional(readOnly = true)
	public List<Contact> getContacts()
	{
		// Send identities and profiles but make sure we don't send
		// an empty profile if it has already identities linked to it.
		var profiles = profileService.getAllProfiles().stream()
				.collect(Collectors.toMap(Profile::getId, profile -> profile));
		var identities = identityService.getAll();
		var profilesIdsToRemove = identities.stream()
				.filter(identity -> identity.getProfile() != null)
				.map(identity -> identity.getProfile().getId())
				.collect(Collectors.toSet());

		profiles.entrySet().removeIf(entry -> profilesIdsToRemove.contains(entry.getKey()));

		List<Contact> contacts = new ArrayList<>(profiles.size() + identities.size());
		profiles.forEach((key, value) -> contacts.add(new Contact(value.getName(), key, 0L, getAvailability(value))));
		identities.forEach(identity -> contacts.add(new Contact(identity.getName(), identity.getProfile() != null ? identity.getProfile().getId() : 0L, identity.getId(), getAvailability(identity.getProfile()))));
		return contacts;
	}

	private Availability getAvailability(Profile profile)
	{
		if (profile != null && profile.isConnected())
		{
			return Availability.AVAILABLE;
		}
		return Availability.OFFLINE;
	}

	public List<Contact> toContacts(List<IdentityGroupItem> identities)
	{
		List<Contact> contacts = new ArrayList<>(identities.size());
		identities.forEach(identity -> contacts.add(new Contact(identity.getName(), identity.getProfile() != null ? identity.getProfile().getId() : new IdentityServiceStorage(identity.getServiceString()).getPgpIdentifier(), identity.getId(), getAvailability(identity.getProfile()))));
		return contacts;
	}

	public Contact toContact(Profile profile)
	{
		return new Contact(profile.getName(), profile.getId(), 0L, getAvailability(profile));
	}
}
