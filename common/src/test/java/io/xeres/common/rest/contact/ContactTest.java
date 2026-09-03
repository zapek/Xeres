/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContactTest
{
	@Test
	void Empty_HasDefaultValues()
	{
		var empty = Contact.EMPTY;
		assertNull(empty.name());
		assertEquals(0L, empty.profileId());
		assertEquals(0L, empty.identityId());
		assertEquals(Availability.OFFLINE, empty.availability());
		assertFalse(empty.accepted());
		assertFalse(empty.banned());
	}

	@Test
	void Own_HasOwnValues()
	{
		var own = Contact.OWN;
		assertNull(own.name());
		assertEquals(1L, own.profileId());
		assertEquals(1L, own.identityId());
		assertEquals(Availability.OFFLINE, own.availability());
		assertTrue(own.accepted());
		assertFalse(own.banned());
	}

	@Test
	void WithName_ChangesName()
	{
		var contact = new Contact("old", 1L, 2L, Availability.AVAILABLE, true, false);
		var updated = Contact.withName(contact, "new");
		assertEquals("new", updated.name());
		assertEquals(1L, updated.profileId());
		assertEquals(2L, updated.identityId());
		assertEquals(Availability.AVAILABLE, updated.availability());
		assertTrue(updated.accepted());
		assertFalse(updated.banned());
	}

	@Test
	void WithAvailability_ChangesAvailability()
	{
		var contact = new Contact("test", 1L, 2L, Availability.OFFLINE, true, false);
		var updated = Contact.withAvailability(contact, Availability.AWAY);
		assertEquals("test", updated.name());
		assertEquals(Availability.AWAY, updated.availability());
	}

	@Test
	void WithIdentityId_ChangesIdentityId()
	{
		var contact = new Contact("test", 1L, 2L, Availability.OFFLINE, true, false);
		var updated = Contact.withIdentityId(contact, 99L);
		assertEquals("test", updated.name());
		assertEquals(99L, updated.identityId());
	}

	@Test
	void Record_Equality()
	{
		var c1 = new Contact("test", 1L, 2L, Availability.AVAILABLE, true, false);
		var c2 = new Contact("test", 1L, 2L, Availability.AVAILABLE, true, false);
		assertEquals(c1, c2);
		assertEquals(c1.hashCode(), c2.hashCode());
	}

	@Test
	void Record_Inequality()
	{
		var c1 = new Contact("test", 1L, 2L, Availability.AVAILABLE, true, false);
		var c2 = new Contact("other", 1L, 2L, Availability.AVAILABLE, true, false);
		assertNotEquals(c1, c2);
	}
}
