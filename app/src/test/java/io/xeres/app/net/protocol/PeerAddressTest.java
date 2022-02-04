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

import java.net.InetSocketAddress;
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
		String IP_AND_PORT = "85.123.33.21:21232";
		PeerAddress peerAddress = PeerAddress.fromIpAndPort(IP_AND_PORT);

		assertEquals(Optional.of(IP_AND_PORT), peerAddress.getAddress());
	}

	@Test
	void PeerAddress_FromIpAndPort_IllegalIpOctetsOverflow_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("500.500.500.500:21232");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_IllegalIpOctetMissing_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("85.123.33:21232");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_IllegalIpZeroPrefix_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("85.123.33.01:21232");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_IllegalIpNotANumber_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("85.123.33.a:21232");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_IllegalPortNotANumber_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("85.123.33.1:a");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_SeparatorButMissingPort_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("85.123.33.1:");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_SeparatorButMissingIp_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort(":2323");

		assertFalse(peerAddress.isValid());
	}

	/**
	 * This kind of IP is legal (ie. "ping 127.1" will work) but we don't want it as it's confusing.
	 */
	@Test
	void PeerAddress_FromIpAndPort_LegalIpButNotWanted_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("85.1:21232");

		assertFalse(peerAddress.isValid());
	}

	/**
	 * That one too. Even more messed up.
	 */
	@Test
	void PeerAddress_FromIpAndPort_LegalIpButNotWanted2_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("85.65530:21232");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_LowPort_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("85.123.33.21:0");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_IllegalPort_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("85.123.33.21:65537");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_Bullshit_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("2384902378237892");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromUrl_OK()
	{
		PeerAddress peerAddress = PeerAddress.fromUrl("ipv4://194.28.22.1:2233");

		assertTrue(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromUrl_MissingPort_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromUrl("ipv4://194.28.22.1");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromUrl_Invalid_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromUrl("ipv666://23sd.2343.2487.asdk");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromAddress_OK()
	{
		PeerAddress peerAddress = PeerAddress.fromAddress("194.28.22.1:1026");

		assertTrue(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromAddress2_OK()
	{
		PeerAddress peerAddress = PeerAddress.fromAddress("1.0.0.1:1026");

		assertTrue(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromAddress_MissingPort_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromAddress("194.28.22.1");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_NonRoutableButLocalhost_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("127.0.0.1:21232");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_NotPublicButPrivateLan_OK()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("192.168.1.5:21232");

		assertTrue(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_NonRoutableButNetwork_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("0.0.0.0:21232");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_NonRoutable3_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("255.255.255.255:21232");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_BroadcastConventionButRoutable_OK()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("1.1.1.255:21232");

		assertTrue(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_NonRoutable5_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("0.1.1.1:21232");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromIpAndPort_NetworkConventionButRoutable_OK()
	{
		PeerAddress peerAddress = PeerAddress.fromIpAndPort("1.1.1.0:21232");

		assertTrue(peerAddress.isValid());
	}

	/**
	 * Tor v2 is not supported anymore
	 */
	@Test
	void PeerAddress_FromTor_v2_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromOnion("expyuzz4wqqyqhjn.onion");

		assertFalse(peerAddress.isValid());
	}

	@Test
	void PeerAddress_FromTor_v3_OK()
	{
		PeerAddress peerAddress = PeerAddress.fromOnion("xpxduj55x2j27l2qytu2tcetykyfxbjbafin3x4i3ywddzphkbrd3jyd.onion");

		assertTrue(peerAddress.isValid());
		assertEquals(Type.TOR, peerAddress.getType());
	}

	@Test
	void PeerAddress_FromTor_WrongAddress_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromOnion("192.168.1.2:8080");

		assertFalse(peerAddress.isValid());
		assertFalse(peerAddress.isHidden());
	}

	@Test
	void PeerAddress_FromHidden_OK()
	{
		PeerAddress peerAddress = PeerAddress.fromHidden("xpxduj55x2j27l2qytu2tcetykyfxbjbafin3x4i3ywddzphkbrd3jyd.onion");

		assertTrue(peerAddress.isValid());
		assertTrue(peerAddress.isHidden());
	}

	@Test
	void PeerAddress_FromHidden_WrongAddress_Fail()
	{
		PeerAddress peerAddress = PeerAddress.fromHidden("192.168.1.2:8080");

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
