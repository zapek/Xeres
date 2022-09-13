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

package io.xeres.app.net.protocol;

import io.xeres.app.net.protocol.PeerAddress.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.util.NoSuchElementException;
import java.util.Optional;

import static io.xeres.app.net.protocol.PeerAddress.Type.*;
import static org.junit.jupiter.api.Assertions.*;

class PeerAddressTest
{
	/**
	 * Builds a PeerAddress from a string like "85.123.33.21:21232"
	 */
	@Test
	void PeerAddress_FromIpAndPort_OK()
	{
		var IP_AND_PORT = "85.123.33.21:21232";
		var peerAddress = PeerAddress.fromIpAndPort(IP_AND_PORT);

		assertEquals(Optional.of(IP_AND_PORT), peerAddress.getAddress());
		assertTrue(peerAddress.isValid());
		assertTrue(peerAddress.isExternal());
		assertFalse(peerAddress.isHidden());
		assertFalse(peerAddress.isHostname());
		assertFalse(peerAddress.isLAN());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"500.500.500.500:21232", // overflow
			"85.123.33:21232", // octet missing
			"85.123.33.01:21232", // octet zero prefix
			"85.123.33.a:21232", // octet not a number
			"85.123.33.1:a", // port not a number
			"85.123.33.1:", // separator but missing port
			":2323", // separator but missing IP
			"85.1:21232", // valid IP but confusing
			"85.123.33.0xa", // valid IP but confusing
			"85.65530:21232", // valid IP but confusing
			"283943283", // valid IP but confusing
			"2384902378237892", // invalid IP (and confusing)
			"85.123.33.21:0", // low port
			"85.123.33.21:65537", // illegal port
			"127.0.0.1:21232", // localhost
			"0.0.0.0:21232", // "network" address
			"255.255.255.255:21232", // "broadcast" address
			"0.1.1.1:21232", // non routable
	})
	void PeerAddress_FromIpAndPort_Fail(String source)
	{
		var peerAddress = PeerAddress.fromIpAndPort(source);

		assertFalse(peerAddress.isValid());
		assertTrue(peerAddress.isInvalid());
		assertTrue(peerAddress.getAddress().isEmpty());
		assertTrue(peerAddress.getAddressAsBytes().isEmpty());
		assertFalse(peerAddress.isHostname());
		assertFalse(peerAddress.isHidden());
		assertNull(peerAddress.getSocketAddress());
		assertEquals(INVALID, peerAddress.getType());
		assertThrows(NoSuchElementException.class, peerAddress::getUrl);
	}

	@Test
	void PeerAddress_FromUrl_OK()
	{
		var URL = "ipv4://194.28.22.1:2233";
		var peerAddress = PeerAddress.fromUrl(URL);

		assertEquals(URL, peerAddress.getUrl());
		assertTrue(peerAddress.isValid());
		assertFalse(peerAddress.isHidden());
		assertFalse(peerAddress.isHostname());
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
			"ipv4://194.28.22.1", // missing port
			"ipv5://194.28.22.1:1234", // bad protocol
			"ipv666://23sd.2343.2487.asdk" // nonsense
	})
	void PeerAddress_FromUrl_Fail(String url)
	{
		var peerAddress = PeerAddress.fromUrl(url);

		assertFalse(peerAddress.isValid());
		assertThrows(NoSuchElementException.class, peerAddress::getUrl);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"194.28.22.1:1026",
			"1.0.0.1:1026"
	})
	void PeerAddress_FromAddress_OK(String source)
	{
		var peerAddress = PeerAddress.fromAddress(source);

		assertTrue(peerAddress.isValid());
		assertTrue(peerAddress.isExternal());
		assertFalse(peerAddress.isLAN());
	}

	@Test
	void PeerAddress_FromAddress_MissingPort_Fail()
	{
		var peerAddress = PeerAddress.fromAddress("194.28.22.1");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_NotPublicButPrivateLan_OK()
	{
		var peerAddress = PeerAddress.fromIpAndPort("192.168.1.5:21232");

		assertTrue(peerAddress.isValid());
		assertTrue(peerAddress.isLAN());
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"1.1.1.255:21232", // broadcast convention
			"1.1.1.0:21232" // network convention
	})
	void PeerAddress_FromIpAndPort_ConventionButRoutable_OK(String source)
	{
		var peerAddress = PeerAddress.fromIpAndPort(source);

		assertTrue(peerAddress.isValid());
		assertTrue(peerAddress.isExternal());
		assertFalse(peerAddress.isLAN());
	}

	/**
	 * Tor v2 is not supported anymore
	 */
	@Test
	void PeerAddress_FromTor_v2_Fail()
	{
		var peerAddress = PeerAddress.fromOnion("expyuzz4wqqyqhjn.onion");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromTor_v3_OK()
	{
		var peerAddress = PeerAddress.fromOnion("xpxduj55x2j27l2qytu2tcetykyfxbjbafin3x4i3ywddzphkbrd3jyd.onion:1234");

		assertTrue(peerAddress.isValid());
		assertEquals(Type.TOR, peerAddress.getType());
	}

	@Test
	void PeerAddress_FromI2p_OK()
	{
		var peerAddress = PeerAddress.fromI2p("g6u4vqiuy6bdc3dbu6a7gmi3ip45sqwgtbgrr6uupqaaqfyztrka.b32.i2p:1234");

		assertTrue(peerAddress.isValid());
		assertEquals(Type.I2P, peerAddress.getType());
	}

	@Test
	void PeerAddress_FromTor_WrongAddress_Fail()
	{
		var peerAddress = PeerAddress.fromOnion("192.168.1.2:8080");

		assertFalse(peerAddress.isValid());
		assertFalse(peerAddress.isHidden());
	}

	@Test
	void PeerAddress_FromHidden_OK()
	{
		var peerAddress = PeerAddress.fromHidden("xpxduj55x2j27l2qytu2tcetykyfxbjbafin3x4i3ywddzphkbrd3jyd.onion:1234");

		assertTrue(peerAddress.isValid());
		assertTrue(peerAddress.isHidden());
	}

	@Test
	void PeerAddress_FromHidden_WrongAddress_Fail()
	{
		var peerAddress = PeerAddress.fromHidden("192.168.1.2:8080");

		assertFalse(peerAddress.isValid());
		assertFalse(peerAddress.isHidden());
	}

	@Test
	void PeerAddress_FromHostname_OK()
	{
		var peerAddress = PeerAddress.fromHostname("foo.bar.com");

		assertTrue(peerAddress.isValid());
		assertTrue(peerAddress.isHostname());
		assertTrue(peerAddress.getSocketAddress() instanceof DomainNameSocketAddress);
	}

	@Test
	void PeerAddress_FromHostName_Invalid()
	{
		var peerAddress = PeerAddress.fromHostname("verylonghostnamethatismorethan63charsandislikelyinvalidandwillfailspectacularly.com");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromHostNameAndPort_OK()
	{
		var peerAddress = PeerAddress.fromHostname("foo.bar.com", 8080);

		assertTrue(peerAddress.isValid());
		assertTrue(peerAddress.isHostname());
		assertTrue(peerAddress.getSocketAddress() instanceof InetSocketAddress);
		assertEquals("foo.bar.com", ((InetSocketAddress) peerAddress.getSocketAddress()).getHostString());
		assertEquals(8080, ((InetSocketAddress) peerAddress.getSocketAddress()).getPort());
	}

	@Test
	void PeerAddress_FromSocketAddress_OK()
	{
		var peerAddress = PeerAddress.fromSocketAddress(InetSocketAddress.createUnresolved("foobar.com", 1234));

		assertTrue(peerAddress.isValid());
		assertFalse(peerAddress.isHostname());
		assertTrue(peerAddress.getSocketAddress() instanceof InetSocketAddress);
		assertEquals("foobar.com", ((InetSocketAddress) peerAddress.getSocketAddress()).getHostString());
		assertEquals(1234, ((InetSocketAddress) peerAddress.getSocketAddress()).getPort());
	}

	@Test
	void PeerAddress_FromHostNameAndPortString_OK()
	{
		var peerAddress = PeerAddress.fromHostnameAndPort("foo.bar.com:8080");

		assertTrue(peerAddress.isValid());
		assertTrue(peerAddress.isHostname());
		assertTrue(peerAddress.getSocketAddress() instanceof InetSocketAddress);
		assertEquals("foo.bar.com", ((InetSocketAddress) peerAddress.getSocketAddress()).getHostString());
		assertEquals(8080, ((InetSocketAddress) peerAddress.getSocketAddress()).getPort());
	}

	@Test
	void PeerAddress_Type_Enum_Order()
	{
		assertEquals(0, INVALID.ordinal());
		assertEquals(1, IPV4.ordinal());
		assertEquals(2, IPV6.ordinal());
		assertEquals(3, TOR.ordinal());
		assertEquals(4, HOSTNAME.ordinal());
		assertEquals(5, I2P.ordinal());

		assertEquals(6, values().length);
	}
}
