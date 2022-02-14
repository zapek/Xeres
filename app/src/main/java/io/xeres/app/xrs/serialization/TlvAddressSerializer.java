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

package io.xeres.app.xrs.serialization;

import io.netty.buffer.ByteBuf;
import io.xeres.app.net.protocol.PeerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvType.*;

final class TlvAddressSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvAddressSerializer.class);

	private TlvAddressSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, PeerAddress peerAddress)
	{
		buf.ensureWritable(peerAddress == null ? TLV_HEADER_SIZE : (TLV_HEADER_SIZE * 2 + 6));
		buf.writeShort(ADDRESS.getValue());

		if (peerAddress == null)
		{
			buf.writeInt(TLV_HEADER_SIZE);
			return TLV_HEADER_SIZE;
		}

		switch (peerAddress.getType())
		{
			case IPV4 -> {
				buf.writeInt(TLV_HEADER_SIZE * 2 + 6);
				buf.writeShort(IPV4.getValue());
				buf.writeInt(TLV_HEADER_SIZE + 6);
				var address = peerAddress.getAddressAsBytes().orElseThrow();
				// RS expects little endian
				buf.writeByte(address[3]);
				buf.writeByte(address[2]);
				buf.writeByte(address[1]);
				buf.writeByte(address[0]);
				buf.writeByte(address[5]);
				buf.writeByte(address[4]);
				return TLV_HEADER_SIZE * 2 + 6;
			}
			default -> throw new IllegalArgumentException("Unsupported address type " + peerAddress.getType().name());
		}
	}

	static PeerAddress deserialize(ByteBuf buf)
	{
		var type = buf.readUnsignedShort();
		log.trace("Address type: {}", type);

		if (type == ADDRESS.getValue())
		{
			var totalSize = buf.readInt(); // XXX: check size

			if (totalSize > TLV_HEADER_SIZE)
			{
				var addressType = buf.readUnsignedShort();
				var addressSize = buf.readInt();

				if (addressType == IPV4.getValue())
				{
					if (addressSize != TLV_HEADER_SIZE + 6)
					{
						throw new IllegalArgumentException("Wrong IPV4 address size: " + addressSize);
					}
					log.trace("reading IPv4 address of {} bytes", addressSize);
					var address = new byte[6];

					// RS stores both in little endian
					address[3] = buf.readByte();
					address[2] = buf.readByte();
					address[1] = buf.readByte();
					address[0] = buf.readByte();

					address[5] = buf.readByte();
					address[4] = buf.readByte();

					return PeerAddress.fromByteArray(address);
				}
				else
				{
					log.debug("Skipping unsupported address type {}, size: {}", addressType, addressSize);
					buf.skipBytes(addressSize - TLV_HEADER_SIZE);
					return PeerAddress.fromInvalid();
				}
			}
		}
		else
		{
			throw new IllegalArgumentException("Unrecognized address " + type);
		}
		return PeerAddress.fromInvalid();
	}

	static int serializeList(ByteBuf buf, List<PeerAddress> addresses)
	{
		buf.ensureWritable(TLV_HEADER_SIZE);
		buf.writeShort(ADDRESS_SET.getValue());
		var totalSize = TLV_HEADER_SIZE;
		var totalSizeOffset = buf.writerIndex();
		buf.writeInt(0);

		if (addresses != null)
		{
			for (var address : addresses)
			{
				var size = TLV_HEADER_SIZE + 12; // long + int below
				buf.writeShort(ADDRESS_INFO.getValue());
				var sizeOffset = buf.writerIndex();
				buf.writeInt(0);
				size += serialize(buf, address);
				buf.writeLong(0); // XXX: seenTime (64-bits)... we don't have that in PeerAddress... where do we get it from?!
				buf.writeInt(0); // XXX: source (32-bits)... likewise
				buf.setInt(sizeOffset, size);
				totalSize += size;
			}
		}
		buf.setInt(totalSizeOffset, totalSize);
		return totalSize;
	}

	static List<PeerAddress> deserializeList(ByteBuf buf)
	{
		var addresses = new ArrayList<PeerAddress>();

		var totalSize = TlvUtils.checkTypeAndLength(buf, ADDRESS_SET);
		var index = buf.readerIndex();

		while (buf.readerIndex() < index + totalSize)
		{
			var size = TlvUtils.checkTypeAndLength(buf, ADDRESS_INFO);
			if (size > 0)
			{
				var peerAddress = deserialize(buf);
				if (peerAddress.isValid())
				{
					addresses.add(peerAddress);
				}
				buf.readLong(); // XXX: seenTime
				buf.readInt(); // XXX: source
			}
		}
		return addresses;
	}
}
