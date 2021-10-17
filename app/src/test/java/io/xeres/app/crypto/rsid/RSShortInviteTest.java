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

import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.common.id.Id;
import io.xeres.common.id.LocationId;
import org.junit.jupiter.api.Test;

import java.security.cert.CertificateParsingException;

import static org.junit.jupiter.api.Assertions.*;

class RSShortInviteTest
{
	@Test
	void ShortInvite_Build_OK()
	{
		var profile = ProfileFakes.createProfile("Nemesis", 0x792b20ca657e2706L, Id.toBytes("06d4b446d209e752fa711a39792b20ca657e2706"), new byte[]{1});
		var location = LocationFakes.createLocation("Home", profile, new LocationId("738ea192064e3f20e766438cc9305bd5"));

		var rsId = new RSIdBuilder(RSId.Type.SHORT_INVITE)
				.setName(profile.getName().getBytes())
				.setPgpFingerprint(profile.getProfileFingerprint().getBytes())
				.setLocationId(location.getLocationId())
				.addLocator(Connection.from(PeerAddress.fromAddress("192.168.1.50:1234")))
				.addLocator(Connection.from(PeerAddress.fromAddress("85.1.2.3:1234")))
				.addLocator(Connection.from(PeerAddress.fromAddress("foo.bar.com:1234")))
				.addLocator(Connection.from(PeerAddress.fromAddress("85.1.2.4:1234")))
				.build();

		var armored = RSIdArmor.getArmored(rsId);

		assertEquals("ABBzjqGSBk4/IOdmQ4zJMFvVAQdOZW1lc2lzAxQG1LRG0gnnUvpxGjl5KyDKZX4nBpENBNJmb28uYmFyLmNvbZIGAwIBVQTSkwYyAajABNICFGlwdjQ6Ly84NS4xLjIuNDoxMjM0BAOiD+U=", armored);
	}

	@Test
	void ShortInvite_Parse_OK() throws CertificateParsingException
	{
		var string = "\nABBzjqGSBk4/IOdmQ4zJMFvVAQdOZW1lc2lzAxQG1LRG0gnnUvpxGjl5KyDKZX4nBpENBNJmb28uYmFyLmNvbZIGAwIBVQTSkwYyAajABNICFGlwdjQ6Ly84NS4xLjIuNDoxMjM0BAOiD+U=\n";

		var rsId = RSId.parse(string);

		assertNotNull(rsId);

		assertTrue(rsId.hasName());
		assertEquals("Nemesis", rsId.getName());

		assertEquals(0x792b20ca657e2706L, rsId.getPgpIdentifier());
		assertArrayEquals(Id.toBytes("06d4b446d209e752fa711a39792b20ca657e2706"), rsId.getPgpFingerprint());

		assertTrue(rsId.hasLocationInfo());
		assertArrayEquals(Id.toBytes("738ea192064e3f20e766438cc9305bd5"), rsId.getLocationId().getBytes());

		assertFalse(rsId.isHiddenNode());

		assertTrue(rsId.hasInternalIp());
		assertTrue(rsId.getInternalIp().getAddress().isPresent());
		assertEquals("192.168.1.50:1234", rsId.getInternalIp().getAddress().get());

		assertTrue(rsId.hasExternalIp());
		assertTrue(rsId.getExternalIp().getAddress().isPresent());
		assertEquals("85.1.2.3:1234", rsId.getExternalIp().getAddress().get());

		assertTrue(rsId.hasDnsName());
		assertTrue(rsId.getDnsName().getAddress().isPresent());
		assertEquals("foo.bar.com:1234", rsId.getDnsName().getAddress().get());

		assertTrue(rsId.hasLocators());
		assertTrue(rsId.getLocators().stream().findFirst().isPresent());
		assertEquals("85.1.2.4:1234", rsId.getLocators().stream().findFirst().get().getAddress().get());
	}
}
