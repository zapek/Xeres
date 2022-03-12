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

import static io.xeres.app.net.peer.packet.Packet.HEADER_SIZE;

public final class SimplePacketBuilder
{
	private SimplePacketBuilder()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static final class Builder
	{
		private int headerSize = HEADER_SIZE;
		private int version;
		private int service;
		private int subPacket;
		private byte[] data = new byte[0];
		private RawItem rawItem;

		private Builder()
		{
		}

		public Builder setVersion(int version)
		{
			this.version = version;
			return this;
		}

		public Builder setService(int service)
		{
			this.service = service;
			return this;
		}

		public Builder setSubPacket(int subPacket)
		{
			this.subPacket = subPacket;
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

		public Builder setHeaderSize(int size)
		{
			this.headerSize = size;
			return this;
		}

		public SimplePacket buildPacket()
		{
			var buf = Unpooled.buffer();

			if (rawItem != null)
			{
				buf.writeBytes(rawItem.getBuffer());
			}
			else
			{
				buf.writeByte(version);
				buf.writeShort(service);
				buf.writeByte(subPacket);
				buf.writeInt(headerSize + data.length);
				buf.writeBytes(data);
			}
			return new SimplePacket(buf);
		}

		public byte[] build()
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
