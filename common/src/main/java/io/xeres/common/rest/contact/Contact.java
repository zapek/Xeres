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

package io.xeres.common.rest.contact;

import io.xeres.common.location.Availability;

public record Contact(String name, long profileId, long identityId, Availability availability, boolean accepted)
{
	public static final Contact EMPTY = new Contact(null, 0L, 0L, Availability.OFFLINE, false);

	public static Contact withAvailability(Contact contact, Availability availability)
	{
		return new Contact(contact.name(), contact.profileId(), contact.identityId(), availability, contact.accepted());
	}

	public static Contact withIdentityId(Contact contact, long identityId)
	{
		return new Contact(contact.name(), contact.profileId(), identityId, contact.availability(), contact.accepted());
	}
}
