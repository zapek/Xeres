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

package io.xeres.app.net.upnp;

import io.xeres.testutils.FakeHttpServer;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlPointTest
{
	@Test
	void ControlPoint_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ControlPoint.class);
	}

	@Test
	void ControlPoint_AddPortMapping_OK()
	{
		var fakeHTTPServer = new FakeHttpServer("/control", 200, null);

		var added = ControlPoint.addPortMapping(
				URI.create("http://localhost:" + fakeHTTPServer.getPort() + "/control"),
				"urn:schemas-upnp-org:service:WANIPConnection:1",
				"192.168.1.78",
				2000,
				2000,
				3600,
				Protocol.TCP
		);
		assertTrue(added);

		fakeHTTPServer.shutdown();
	}

	@Test
	void ControlPoint_RemovePortMapping_OK()
	{
		var fakeHTTPServer = new FakeHttpServer("/control", 200, null);

		var removed = ControlPoint.removePortMapping(
				URI.create("http://localhost:" + fakeHTTPServer.getPort() + "/control"),
				"urn:schemas-upnp-org:service:WANIPConnection:1",
				2000,
				Protocol.TCP
		);
		assertTrue(removed);

		fakeHTTPServer.shutdown();
	}

	@Test
	void ControlPoint_GetExternalIPAddress_OK()
	{
		var responseBody = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
				"<s:Body>" +
				"<u:GetExternalIPAddressResponse xmlns:u=\"urn:schemas-upnp-org:service:WANIPConnection:1\">" +
				"<NewExternalIPAddress>1.1.1.1</NewExternalIPAddress>" +
				"</u:GetExternalIPAddressResponse>" +
				"</s:Body>" +
				"</s:Envelope>";

		var fakeHTTPServer = new FakeHttpServer("/control", 200, responseBody.getBytes());

		var response = ControlPoint.getExternalIpAddress(
				URI.create("http://localhost:" + fakeHTTPServer.getPort() + "/control"),
				"urn:schemas-upnp-org:service:WANIPConnection:1"
		);

		assertEquals("1.1.1.1", response);

		fakeHTTPServer.shutdown();
	}
}
