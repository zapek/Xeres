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
		// XXX: missing ensureWritable()
		buf.writeShort(ADDRESS.getValue());

		if (peerAddress == null)
		{
			buf.writeInt(6);
			return 6;
		}

		switch (peerAddress.getType())
		{
			case IPV4 -> {
				buf.writeInt(18);
				buf.writeShort(IPV4.getValue());
				buf.writeInt(12);
				byte[] addr = peerAddress.getAddressAsBytes().orElseThrow();
				// RS expects little endian
				buf.writeByte(addr[3]);
				buf.writeByte(addr[2]);
				buf.writeByte(addr[1]);
				buf.writeByte(addr[0]);
				buf.writeByte(addr[5]);
				buf.writeByte(addr[4]);
				return 18;
			}
			default -> throw new IllegalArgumentException("Unsupported address type " + peerAddress.getType().name());
		}
	}

	static PeerAddress deserialize(ByteBuf buf)
	{
		int type = buf.readUnsignedShort();
		log.trace("Address type: {}", type);

		if (type == ADDRESS.getValue())
		{
			var totalSize = buf.readInt(); // XXX: check size

			if (totalSize > 6)
			{
				int addrType = buf.readUnsignedShort();
				if (addrType == IPV4.getValue())
				{
					var addrSize = buf.readInt(); // XXX: check size
					log.trace("reading IPv4 address of {} bytes", addrSize);
					var a = new byte[6];

					// RS stores both in little endian
					a[3] = buf.readByte();
					a[2] = buf.readByte();
					a[1] = buf.readByte();
					a[0] = buf.readByte();

					a[5] = buf.readByte();
					a[4] = buf.readByte();

					return PeerAddress.fromByteArray(a);
				}
				else
				{
					log.debug("Skipping unsupported address type {}", addrType);
					var addrSize = buf.readInt();
					buf.skipBytes(addrSize - 6);
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
		// XXX: missing ensureWritable()
		buf.writeShort(ADDRESS_SET.getValue());
		var totalSize = 6;
		int totalSizeOffset = buf.writerIndex();
		buf.writeInt(0);

		if (addresses != null)
		{
			for (PeerAddress address : addresses)
			{
				var size = 18;
				buf.writeShort(ADDRESS_INFO.getValue());
				int sizeOffset = buf.writerIndex();
				buf.writeInt(0);
				size += serialize(buf, address);
				buf.writeLong(0); // XXX: seenTime (64-bits)... we don't have that in PeerAddress... where do we get it from?!
				buf.writeInt(0); // XXX: source (64-bits)... likewise
				buf.setInt(sizeOffset, size);
				totalSize += size;
			}
		}
		buf.setInt(totalSizeOffset, totalSize);
		return totalSize;
	}

	static List<PeerAddress> deserializeList(ByteBuf buf)
	{
		int type = buf.readUnsignedShort();
		var addresses = new ArrayList<PeerAddress>();

		if (type != ADDRESS_SET.getValue())
		{
			throw new IllegalArgumentException("Type " + type + " does not match " + ADDRESS_SET.getValue());
		}

		var totalSize = buf.readInt(); // XXX: check size
		log.trace("Skiping address list (for now)");
		// XXX: add code to parse multiple addresses, it's not very hard, just call the unserialize above
		// XXX: don't forget there can be empty addresses... so probably remove the invalid ones

		return addresses;
	}
}
