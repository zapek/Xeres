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

package io.xeres.app.net.bdisc;

import io.netty.buffer.Unpooled;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.ProfileFingerprint;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public final class UdpDiscoveryProtocol
{
	private static final int MAGIC = 0x524e3655; // RN6U

	private UdpDiscoveryProtocol()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static UdpDiscoveryPeer parsePacket(ByteBuffer buffer, InetSocketAddress peerAddress)
	{
		if (buffer.limit() < 29)
		{
			throw new IllegalArgumentException("Buffer is too small: " + buffer.limit());
		}

		if (buffer.getInt() != MAGIC)
		{
			throw new IllegalArgumentException("Wrong magic number in header");
		}

		var peer = new UdpDiscoveryPeer();
		peer.setIpAddress(peerAddress.getAddress().getHostAddress());

		buffer.getInt(); // reserved
		peer.setStatus(UdpDiscoveryPeer.Status.values()[buffer.get()]);
		peer.setAppId(buffer.getInt());
		peer.setPeerId(buffer.getInt());

		// Because of https://github.com/truvorskameikin/udp-discovery-cpp/commit/d37a19f2326d2a44dff65c2ce26c7d19380ef699 the following
		// either uses a 64-bit packetIndex or a 32-bit one + overflow byte. We read the padding ahead to figure out
		// which version it is.
		if (buffer.getShort(7) == 0)
		{
			peer.setPacketIndex(buffer.getInt()); // XXX: this one just increments all the time
			buffer.get(); // padding
		}
		else
		{
			peer.setPacketIndex(buffer.getLong());
		}

		int userDataSize = buffer.getShort();
		int paddingSize = buffer.getShort();
		if (userDataSize > buffer.remaining())
		{
			throw new IllegalArgumentException("Userdata size of " + userDataSize + " is too big (" + buffer.remaining() + " remaining)");
		}

		var buf = Unpooled.wrappedBuffer(buffer);
		peer.setFingerprint((ProfileFingerprint) Serializer.deserializeIdentifier(buf, ProfileFingerprint.class));
		peer.setLocationId((LocationId) Serializer.deserializeIdentifier(buf, LocationId.class));
		peer.setLocalPort(Serializer.deserializeShort(buf));
		peer.setProfileName(Serializer.deserializeString(buf));

		return peer;
	}

	public static ByteBuffer createPacket(int maxSize, UdpDiscoveryPeer.Status status, int appId, int peerId, int counter, ProfileFingerprint fingerprint, LocationId locationId, int localPort, String profileName)
	{
		var buffer = ByteBuffer.allocate(maxSize);

		buffer.putInt(MAGIC);
		buffer.putInt(0); // reserved

		buffer.put((byte) status.ordinal());
		buffer.putInt(appId);
		buffer.putInt(peerId);

		// XXX: now this sucks... what to put here? 32-bit or 64-bit?! For now we'll put the 32-bit
		// version but ideally we should monitor what happens and use one or the other depending on
		// what is seen.
		buffer.putInt(counter);
		buffer.put((byte) 0);

		var buf = Unpooled.buffer();
		Serializer.serialize(buf, fingerprint, ProfileFingerprint.class);
		Serializer.serialize(buf, locationId, LocationId.class);
		Serializer.serialize(buf, (short) localPort);
		Serializer.serialize(buf, profileName);

		buffer.putShort((short) buf.writerIndex());
		buffer.putShort((short) 0);
		buffer.put(buf.nioBuffer());
		buf.release();

		return buffer;
	}
}
