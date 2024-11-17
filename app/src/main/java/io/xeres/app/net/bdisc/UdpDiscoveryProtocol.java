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

import io.netty.buffer.Unpooled;
import io.xeres.app.net.bdisc.UdpDiscoveryPeer.Status;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.ProfileFingerprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import static io.xeres.app.net.bdisc.ProtocolVersion.*;

public final class UdpDiscoveryProtocol
{
	private static final Logger log = LoggerFactory.getLogger(UdpDiscoveryProtocol.class);

	private static final int MAGIC_HEADER_OLD = 0x524e3655; // RN6U
	private static final int MAGIC_HEADER_VERSIONED = 0x534f3756; // SO7V

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

		var magicHeader = buffer.getInt();
		ProtocolVersion protocolVersion;

		switch (magicHeader)
		{
			case MAGIC_HEADER_OLD ->
			{
				buffer.get(); // reserved

				protocolVersion = VERSION_0;
			}
			case MAGIC_HEADER_VERSIONED ->
			{
				var versionNum = buffer.get();
				if (versionNum > VERSION_2.ordinal())
				{
					log.warn("Unsupported protocol version: {}", versionNum);
					return null;
				}
				protocolVersion = ProtocolVersion.values()[versionNum];
			}
			default ->
			{
				log.warn("Unsupported magic header: {}", magicHeader);
				return null;
			}
		}
		buffer.get(); // reserved
		buffer.get(); // reserved
		buffer.get(); // reserved

		var peer = new UdpDiscoveryPeer();
		peer.setIpAddress(peerAddress.getAddress().getHostAddress());
		var packetStatusNum = buffer.get();
		if (packetStatusNum > Status.LEAVING.ordinal())
		{
			log.warn("Unknown packet status: {}", packetStatusNum);
			return null;
		}
		peer.setStatus(Status.values()[packetStatusNum]);
		peer.setAppId(buffer.getInt());
		peer.setPeerId(buffer.getInt());

		if (protocolVersion == VERSION_0)
		{
			peer.setPacketIndex(buffer.getInt());
			buffer.get(); // packet index reset "overflow" flag
		}
		else
		{
			peer.setPacketIndex(buffer.getLong());
		}

		int userDataSize = buffer.getShort();
		if (protocolVersion == VERSION_0)
		{
			buffer.getShort(); // padding size
		}
		if (userDataSize > buffer.remaining())
		{
			throw new IllegalArgumentException("Userdata size of " + userDataSize + " is too big (" + buffer.remaining() + " remaining)");
		}

		var buf = Unpooled.wrappedBuffer(buffer);
		if (protocolVersion != VERSION_2)
		{
			peer.setFingerprint((ProfileFingerprint) Serializer.deserializeIdentifierWithSize(buf, ProfileFingerprint.class, ProfileFingerprint.V4_LENGTH));
		}
		else
		{
			var fingerPrintSize = buffer.get();
			switch (fingerPrintSize)
			{
				case 20 -> peer.setFingerprint((ProfileFingerprint) Serializer.deserializeIdentifierWithSize(buf, ProfileFingerprint.class, ProfileFingerprint.V4_LENGTH));
				case 32 -> peer.setFingerprint((ProfileFingerprint) Serializer.deserializeIdentifierWithSize(buf, ProfileFingerprint.class, ProfileFingerprint.LENGTH));
				default -> throw new IllegalArgumentException("Unknown fingerprint size:" + fingerPrintSize);
			}
		}
		peer.setLocationId((LocationId) Serializer.deserializeIdentifier(buf, LocationId.class));
		peer.setLocalPort(Serializer.deserializeShort(buf));
		peer.setProfileName(Serializer.deserializeString(buf));

		return peer;
	}

	public static ByteBuffer createPacket(int maxSize, Status status, int appId, int peerId, int counter, ProfileFingerprint fingerprint, LocationId locationId, int localPort, String profileName)
	{
		var buffer = ByteBuffer.allocate(maxSize);

		buffer.putInt(MAGIC_HEADER_VERSIONED);
		if (fingerprint.getLength() == 32)
		{
			buffer.put((byte) VERSION_2.ordinal()); // protocol version
		}
		else if (fingerprint.getLength() == 20)
		{
			buffer.put((byte) VERSION_1.ordinal()); // protocol version
		}
		else
		{
			throw new IllegalArgumentException("Unknown fingerprint size:" + fingerprint.getLength());
		}
		buffer.put((byte) 0);
		buffer.put((byte) 0);
		buffer.put((byte) 0);

		buffer.put((byte) status.ordinal());
		buffer.putInt(appId);
		buffer.putInt(peerId);

		buffer.putLong(counter);

		var buf = Unpooled.buffer();
		Serializer.serialize(buf, fingerprint, ProfileFingerprint.class);
		Serializer.serialize(buf, locationId, LocationId.class);
		Serializer.serialize(buf, (short) localPort);
		Serializer.serialize(buf, profileName);

		buffer.putShort((short) buf.writerIndex());
		buffer.put(buf.nioBuffer());
		buf.release();

		return buffer;
	}
}
