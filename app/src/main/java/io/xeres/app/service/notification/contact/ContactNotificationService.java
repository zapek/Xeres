/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.service.notification.contact;

import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.service.ContactService;
import io.xeres.app.service.notification.NotificationService;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.rest.contact.Contact;
import io.xeres.common.rest.notification.contact.ContactNotification;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.xeres.common.rest.notification.contact.ContactOperation.ADD_OR_UPDATE;
import static io.xeres.common.rest.notification.contact.ContactOperation.REMOVE;

@Service
public class ContactNotificationService extends NotificationService
{
	private final ContactService contactService;

	public ContactNotificationService(ContactService contactService)
	{
		this.contactService = contactService;
	}

	public void addOrUpdateIdentities(List<IdentityGroupItem> identities)
	{
		addOrUpdateContacts(contactService.toContacts(identities));
	}

	public void removeIdentities(List<IdentityGroupItem> identities)
	{
		removeContacts(contactService.toContacts(identities));
	}

	public void addOrUpdateProfile(Profile profile)
	{
		addOrUpdateContacts(List.of(contactService.toContact(profile)));
	}

	public void removeProfile(Profile profile)
	{
		removeContacts(List.of(contactService.toContact(profile)));
	}

	private void addOrUpdateContacts(List<Contact> contacts)
	{
		sendNotification(new ContactNotification(ADD_OR_UPDATE, contacts));
	}

	private void removeContacts(List<Contact> contacts)
	{
		sendNotification(new ContactNotification(REMOVE, contacts));
	}
}
