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

package io.xeres.app.net.upnp;

import io.xeres.testutils.FakeHttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTest
{
	@Test
	void Device_From_OK() throws IOException
	{
		byte[] routerReply = Files.readAllBytes(ResourceUtils.getFile(ResourceUtils.CLASSPATH_URL_PREFIX + "upnp/routers/RT-AC87U.xml").toPath());
		var fakeHTTPServer = new FakeHttpServer("/rootDesc.xml", 200, routerReply);

		var inetSocketAddress = new InetSocketAddress(fakeHTTPServer.getPort());
		String httpuReply = "HTTP/1.1 200 OK\n" +
				"CACHE-CONTROL: max-age=120\n" +
				"ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1\n" +
				"USN: uuid:3ddcd1d3-2380-45f5-b069-88d7f644f8d8::urn:schemas-upnp-org:device:InternetGatewayDevice:1\n" +
				"EXT:\n" +
				"SERVER: AsusWRT/384.13 UPnP/1.1 MiniUPnPd/2.1\n" +
				"LOCATION: http://localhost:" + fakeHTTPServer.getPort() + "/rootDesc.xml\n" +
				"OPT: \"http://schemas.upnp.org/upnp/1/0/\"; ns=01\n" +
				"01-NLS: 1594920600\n" +
				"BOOTID.UPNP.ORG: 1594920600\n" +
				"CONFIGID.UPNP.ORG: 1337\n" +
				"\n";

		Device device = Device.from(
				inetSocketAddress,
				ByteBuffer.wrap(httpuReply.getBytes())
		);
		assertTrue(device.isValid());
		assertFalse(device.isInvalid());
		assertEquals(inetSocketAddress, device.getInetSocketAddress());
		assertTrue(device.hasLocation());
		assertEquals("http://localhost:" + fakeHTTPServer.getPort() + "/rootDesc.xml", device.getLocationUrl().toString());
		assertTrue(device.hasServer());
		assertEquals("AsusWRT/384.13 UPnP/1.1 MiniUPnPd/2.1", device.getServer());
		assertTrue(device.hasUsn());
		assertEquals("uuid:3ddcd1d3-2380-45f5-b069-88d7f644f8d8::urn:schemas-upnp-org:device:InternetGatewayDevice:1", device.getUsn());

		device.addControlPoint();
		assertTrue(device.hasControlPoint());

		assertTrue(device.hasControlUrl());
		assertEquals("http://localhost:" + fakeHTTPServer.getPort() + "/ctl/IPConn", device.getControlUrl().toString());
		assertTrue(device.hasManufacturer());
		assertEquals("ASUSTek", device.getManufacturer());
		assertEquals("http://www.asus.com/", device.getManufacturerUrl().toString());
		assertTrue(device.hasModelName());
		assertEquals("RT-AC87U", device.getModelName());
		assertTrue(device.hasPresentationUrl());
		assertEquals("http://192.168.1.1:80/", device.getPresentationUrl().toString());
		assertTrue(device.hasSerialNumber());
		assertEquals("88:d7:f6:44:f8:d8", device.getSerialNumber());
		assertEquals("urn:schemas-upnp-org:service:WANIPConnection:1", device.getServiceType());

		fakeHTTPServer.shutdown();
	}
}
