/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.xeres.app.crypto.rsid.ShortInvite.*;
import static io.xeres.common.rsid.Type.SHORT_INVITE;
import static org.junit.jupiter.api.Assertions.*;

class RSShortInviteTest
{
	@Test
	void ShortInviteTags_Values()
	{
		assertEquals(0x0, SSL_ID);
		assertEquals(0x1, NAME);
		assertEquals(0x2, LOCATOR);
		assertEquals(0x3, PGP_FINGERPRINT);
		assertEquals(0X4, CHECKSUM);
		assertEquals(0X90, HIDDEN_LOCATOR);
		assertEquals(0X91, DNS_LOCATOR);
		assertEquals(0X92, EXT4_LOCATOR);
		assertEquals(0X93, LOC4_LOCATOR);
	}

	@Test
	void ShortInviteQuirks_SwapBytes_OK()
	{
		var input = new byte[]{1, 2, 3, 4, 5, 6};
		var output = new byte[]{4, 3, 2, 1, 5, 6};

		assertArrayEquals(output, swapBytes(input));
	}

	@Test
	void ShortInviteQuirks_SwapBytes_WrongInput_NoSwap()
	{
		var input = new byte[]{1, 2, 3, 4, 5, 6, 7};
		var output = new byte[]{1, 2, 3, 4, 5, 6, 7};

		assertArrayEquals(output, swapBytes(input));
	}

	@Test
	void ShortInvite_Build_OK()
	{
		var profile = ProfileFakes.createProfile("Nemesis", 0x792b20ca657e2706L, Id.toBytes("06d4b446d209e752fa711a39792b20ca657e2706"), new byte[]{1});
		var location = LocationFakes.createLocation("Home", profile, new LocationId("738ea192064e3f20e766438cc9305bd5"));

		var rsId = new RSIdBuilder(SHORT_INVITE)
				.setName(profile.getName().getBytes())
				.setPgpFingerprint(profile.getProfileFingerprint().getBytes())
				.setLocationId(location.getLocationId())
				.addLocator(Connection.from(PeerAddress.fromAddress("192.168.1.50:1234")))
				.addLocator(Connection.from(PeerAddress.fromAddress("85.1.2.3:1234")))
				.addLocator(Connection.from(PeerAddress.fromAddress("foo.bar.com:1234")))
				.addLocator(Connection.from(PeerAddress.fromAddress("85.1.2.4:1234")))
				.build();

		var armored = rsId.getArmored();

		assertEquals("ABBzjqGSBk4/IOdmQ4zJMFvVAQdOZW1lc2lzAxQG1LRG0gnnUvpxGjl5KyDKZX4nBpENBNJmb28uYmFyLmNvbZIGAwIBVQTSkwYyAajABNICFGlwdjQ6Ly84NS4xLjIuNDoxMjM0BAOiD+U=", armored);
	}

	@Test
	void ShortInvite_Parse_OK()
	{
		var string = "\nABBzjqGSBk4/IOdmQ4zJMFvVAQdOZW1lc2lzAxQG1LRG0gnnUvpxGjl5KyDKZX4nBpENBNJmb28uYmFyLmNvbZIGAwIBVQTSkwYyAajABNICFGlwdjQ6Ly84NS4xLjIuNDoxMjM0BAOiD+U=\n";

		var rsId = RSId.parse(string, SHORT_INVITE);

		assertTrue(rsId.isPresent());

		assertEquals("Nemesis", rsId.get().getName());

		assertEquals(0x792b20ca657e2706L, rsId.get().getPgpIdentifier());
		assertArrayEquals(Id.toBytes("06d4b446d209e752fa711a39792b20ca657e2706"), rsId.get().getPgpFingerprint().getBytes());

		assertArrayEquals(Id.toBytes("738ea192064e3f20e766438cc9305bd5"), rsId.get().getLocationId().getBytes());

		assertTrue(rsId.get().getHiddenNodeAddress().isEmpty());

		assertTrue(rsId.get().getInternalIp().isPresent());
		assertTrue(rsId.get().getInternalIp().get().getAddress().isPresent());
		assertEquals("192.168.1.50:1234", rsId.get().getInternalIp().get().getAddress().get());

		assertTrue(rsId.get().getExternalIp().isPresent());
		assertTrue(rsId.get().getExternalIp().get().getAddress().isPresent());
		assertEquals("85.1.2.3:1234", rsId.get().getExternalIp().get().getAddress().get());

		assertTrue(rsId.get().getDnsName().isPresent());
		assertTrue(rsId.get().getDnsName().get().getAddress().isPresent());
		assertEquals("foo.bar.com:1234", rsId.get().getDnsName().get().getAddress().get());

		assertFalse(rsId.get().getLocators().isEmpty());
		assertTrue(rsId.get().getLocators().stream().findFirst().isPresent());
		assertEquals("85.1.2.4:1234", rsId.get().getLocators().stream().findFirst().get().getAddress().orElseThrow());
	}

	@Test
	void ShortInvite_Parse_Empty()
	{
		var string = "";

		var rsId = RSId.parse(string, SHORT_INVITE);

		assertFalse(rsId.isPresent());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			// Empty
			"",
			// Wrong checksum
			"ABCE1fl2NmWv3Ri9EjwzgIHAAQpaYXBla1hlcmVzAxRBmhvGfPlWxi+DfVZv7SmEFhoE0pIG/tnDVUGzkwZOAajAQbMEA6cUSw==",
			// Wrong checksum length
			"ABCE1fl2NmWv3Ri9EjwzgIHAAQpaYXBla1hlcmVzAxRBmhvGfPlWxi+DfVZv7SmEFhoE0pIG/tnDVUGzkwZOAajAQbMEAqcUSg==",
			// Missing checksum
			"ABCE1fl2NmWv3Ri9EjwzgIHAAQpaYXBla1hlcmVzAxRBmhvGfPlWxi+DfVZv7SmEFhoE0pIG/tnDVUGzkwZOAajAQbM=",
			// Packet shorter than advertised length
			"ABCE1fl2NmWv3Ri9EjwzgIHAAQpaYXBla1hlcmVzAxRBmhvGfPlWxi+DfVZv7SmEFhoE0pIG/tnDVUGzkwZOAajAQbMEBKcUSg==",
			// Missing location id
			"AQpaYXBla1hlcmVzAxRBmhvGfPlWxi+DfVZv7SmEFhoE0pIG/tnDVUGzkwZOAajAQbMEA4YtNA==",
			// Missing name
			"ABCE1fl2NmWv3Ri9EjwzgIHAAxRBmhvGfPlWxi+DfVZv7SmEFhoE0pIG/tnDVUGzkwZOAajAQbMEAzEjTQ==",
			// Missing PGP fingerprint
			"ABCE1fl2NmWv3Ri9EjwzgIHAAQpaYXBla1hlcmVzkgb+2cNVQbOTBk4BqMBBswQDXfgj"
	})
	void ShortInvite_Parse_Error(String string)
	{
		var rsId = RSId.parse(string, SHORT_INVITE);

		assertFalse(rsId.isPresent());
	}
}
