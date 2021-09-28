/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.crypto.rsid;

import org.junit.jupiter.api.Test;

import java.security.cert.CertificateParsingException;

import static org.junit.jupiter.api.Assertions.*;

class RSShortInviteTest
{
	@Test
	void ShortInvite_Parse_OK() throws CertificateParsingException
	{
		String string = "\nABB6pdA9HxM0rYO13vpw2+8BAxT1dVEUN811dCoiyW0TPlJQhN5dTQEFemFwZWuSBv7Zw1UhUAQDOuCY\n"; // XXX: try it with a later ID too!

		RSId rsId = RSId.parse(string);

		assertNotNull(rsId);
		assertNull(rsId.getPgpPublicKey());
		assertFalse(rsId.hasInternalIp()); // XXX: try with a later ID... which has the right IP in the certificate
		assertNull(rsId.getInternalIp());
		assertTrue(rsId.hasExternalIp());
		assertNotNull(rsId.getExternalIp());
		assertNotNull(rsId.getLocationId());
	}
}
