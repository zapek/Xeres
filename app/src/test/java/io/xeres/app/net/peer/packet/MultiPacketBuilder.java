/*
 * Copyright (c) 2019-2020 by David Gerber - https://zapek.com
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

package io.xeres.app.net.peer.packet;

import io.netty.buffer.Unpooled;
import io.xeres.app.xrs.item.RawItem;

import static io.xeres.app.net.peer.packet.MultiPacket.SLICE_FLAG_END;
import static io.xeres.app.net.peer.packet.MultiPacket.SLICE_FLAG_START;
import static io.xeres.app.net.peer.packet.Packet.SLICE_PROTOCOL_VERSION_ID_01;

public final class MultiPacketBuilder
{
	private MultiPacketBuilder()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static final class Builder
	{
		private int flags = SLICE_FLAG_START | SLICE_FLAG_END; // XXX: make that settable
		private int packetId;
		private byte[] data = new byte[0];
		private RawItem rawItem;

		private Builder()
		{
		}

		public Builder setFlags(int flags)
		{
			this.flags = flags;
			return this;
		}

		public Builder setPacketId(int packetId)
		{
			this.packetId = packetId;
			return this;
		}

		public Builder setData(byte[] data)
		{
			this.data = data;
			return this;
		}

		public Builder setRawItem(RawItem rawItem)
		{
			this.rawItem = rawItem;
			return this;
		}

		public MultiPacket buildPacket()
		{
			var buf = Unpooled.buffer();

			buf.writeByte(SLICE_PROTOCOL_VERSION_ID_01);
			buf.writeByte(flags);
			buf.writeInt(packetId);

			if (rawItem != null)
			{
				var itemBuf = rawItem.getBuffer();
				buf.writeShort(itemBuf.writerIndex());
				buf.writeBytes(rawItem.getBuffer());
			}
			else
			{
				buf.writeShort(data.length);
				buf.writeBytes(data);
			}
			return new MultiPacket(buf);
		}

		public byte[] build() // XXX: is it what we want or do we just need the content?
		{
			var buf = buildPacket().getBuffer();
			var bytes = new byte[buf.writerIndex()];
			buf.readBytes(bytes);
			return bytes;
		}
	}

	public static Builder builder()
	{
		return new Builder();
	}
}
