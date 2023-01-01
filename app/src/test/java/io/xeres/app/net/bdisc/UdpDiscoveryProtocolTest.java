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

package io.xeres.app.net.bdisc;

import io.xeres.common.id.Id;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.ProfileFingerprint;
import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class UdpDiscoveryProtocolTest
{
	private static final int APP_ID = 904571;
	private static final int PEER_ID = 1730783293;
	private static final int PACKET_INDEX = 32921;
	private static final UdpDiscoveryPeer.Status STATUS_PRESENT = UdpDiscoveryPeer.Status.PRESENT;
	private static final ProfileFingerprint FINGERPRINT = new ProfileFingerprint(Id.toBytes("54B7C121B73E434539DC3E0BA87461B115390F34"));
	private static final LocationId LOCATION_ID = new LocationId(Id.toBytes("ec65a805a3faa6d4b88e7a2ee5a45f33"));
	private static final String LOCAL_IP = "127.0.0.1";
	private static final int LOCAL_PORT = 8600;
	private static final String PROFILE_NAME = "retroshare.ch";

	private static final String DATA = "524e36550000000000000dcd7b6729a83d00008099000037000054b7c121b73e434539dc3e0ba87461b115390f34ec65a805a3faa6d4b88e7a2ee5a45f3321980000000d726574726f73686172652e6368";

	private static final String DATA_NEW = "534f37560100000000000dcd7b6729a83d0000000000008099003754b7c121b73e434539dc3e0ba87461b115390f34ec65a805a3faa6d4b88e7a2ee5a45f3321980000000d726574726f73686172652e6368";

	@Test
	void UdpDiscoveryProtocol_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(UdpDiscoveryProtocol.class);
	}

	@Test
	void UdpDiscoveryProtocol_ParsePacket_OK()
	{
		var peer = UdpDiscoveryProtocol.parsePacket(ByteBuffer.wrap(Id.toBytes(DATA)), new InetSocketAddress(LOCAL_IP, 6666));

		assertNotNull(peer);
		assertEquals(APP_ID, peer.getAppId());
		assertEquals(PEER_ID, peer.getPeerId());
		assertEquals(PACKET_INDEX, peer.getPacketIndex());
		assertEquals(STATUS_PRESENT, peer.getStatus());
		assertEquals(FINGERPRINT, peer.getFingerprint());
		assertEquals(LOCATION_ID, peer.getLocationId());
		assertEquals(LOCAL_IP, peer.getIpAddress());
		assertEquals(LOCAL_PORT, peer.getLocalPort());
		assertEquals(PROFILE_NAME, peer.getProfileName());
	}

	@Test
	void UdpDiscoveryProtocol_CreatePacket_OK()
	{
		var data = UdpDiscoveryProtocol.createPacket(
				512,
				STATUS_PRESENT,
				APP_ID,
				PEER_ID,
				PACKET_INDEX,
				FINGERPRINT,
				LOCATION_ID,
				LOCAL_PORT,
				PROFILE_NAME);

		var a = new byte[data.position()];
		data.flip();
		data.get(a);
		assertArrayEquals(Id.toBytes(DATA_NEW), a);
	}
}
