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

package io.xeres.app.service.notification.contact;

import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.service.ContactService;
import io.xeres.app.service.notification.NotificationService;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.rest.contact.Contact;
import io.xeres.common.rest.notification.Notification;
import io.xeres.common.rest.notification.contact.AddContacts;
import io.xeres.common.rest.notification.contact.ContactNotification;
import io.xeres.common.rest.notification.contact.RemoveContacts;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactNotificationService extends NotificationService
{
	private final ContactService contactService;

	public ContactNotificationService(ContactService contactService)
	{
		this.contactService = contactService;
	}

	public void addIdentities(List<IdentityGroupItem> identities)
	{
		addContacts(contactService.toContacts(identities));
	}

	public void removeIdentities(List<IdentityGroupItem> identities)
	{
		removeContacts(contactService.toContacts(identities));
	}

	public void addProfile(Profile profile)
	{
		addContacts(List.of(contactService.toContact(profile)));
	}

	public void removeProfile(Profile profile)
	{
		removeContacts(List.of(contactService.toContact(profile)));
	}

	private void addContacts(List<Contact> contacts)
	{
		var action = new AddContacts(contacts);
		sendNotificationAlways(new ContactNotification(action.getClass().getSimpleName(), action));
	}

	private void removeContacts(List<Contact> contacts)
	{
		var action = new RemoveContacts(contacts);
		sendNotificationAlways(new ContactNotification(action.getClass().getSimpleName(), action));
	}

	@Override
	protected Notification createNotification()
	{
		return null;
	}
}
