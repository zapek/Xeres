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

package io.xeres.app.net.peer;

import io.xeres.app.net.peer.packet.Packet;
import io.xeres.app.xrs.item.RawItem;

import java.util.concurrent.ThreadLocalRandom;

// and perhaps not use RsPacket at all since it's a simulator class
// do an OldPacketBuilder and a NewPacketBuilder?
@Deprecated(forRemoval = true)
public class PacketBuilder
{
	public static final class Builder
	{
		private boolean newPacket = true;
		private boolean noAlloc;
		private int version = 2;
		private int service;
		private int subPacket;
		private RawItem rawItem;
		private int dataSize;
		private int headerSize = 8;
		private int id;
		private int flags = 0;
		private Integer priority;
		private byte[] data;

		private Builder()
		{
		}

		public Builder setOldPacket()
		{
			newPacket = false;
			return this;
		}

		public Builder setVersion(int version)
		{
			this.version = version;
			return this;
		}

		public Builder setStart()
		{
			//flags = Packet.SLICE_FLAG_START;
			return this;
		}

		public Builder setEnd()
		{
			//flags = Packet.SLICE_FLAG_END;
			return this;
		}

		public Builder setMiddle()
		{
			flags = 0;
			return this;
		}

		public Builder setNoAlloc()
		{
			noAlloc = true;
			return this;
		}

		public Builder setItem(RawItem rawItem)
		{
			this.rawItem = rawItem;
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

		public Builder setDataSize(int size)
		{
			dataSize = size;
			return this;
		}

		public Builder setHeaderSize(int size)
		{
			headerSize = size;
			return this;
		}

		public Builder setId(int id)
		{
			this.id = id;
			return this;
		}

		public Builder setFlags(int flags)
		{
			this.flags = flags;
			return this;
		}

		public Builder setData(byte[] data)
		{
			this.data = data;
			return this;
		}

		public Builder setRandomData(int size)
		{
			data = new byte[size];
			ThreadLocalRandom.current().nextBytes(data);
			return this;
		}

		public Builder setPriority(int priority)
		{
			this.priority = priority;
			return this;
		}

		public Packet buildPacket()
		{
			//RsPacket packet = new RsPacket();

			if (newPacket)
			{
				//if ((flags & Packet.SLICE_FLAG_START) != 0)
				{
					//packet.setStart(true);
				}
				//if ((flags & Packet.SLICE_FLAG_END) != 0)
				{
					//packet.setEnd(true);
				}
				//packet.setId(id);
				if (priority != null)
				{
					//packet.setPriority(priority);
				}

				if (dataSize > 0)
				{
					//packet.setData(new byte[dataSize]);

					if (data != null)
					{
						if (data.length > dataSize)
						{
							throw new IllegalArgumentException("data > dataSize");
						}
						//System.arraycopy(data, 0, packet.getData(), 0, data.length);
					}
				}
				else
				{
					if (rawItem != null)
					{
						//data = item.getData();
					}
					if (data != null)
					{
						//packet.setData(data);
						dataSize = data.length;
					}
				}

				var headerAndData = new byte[noAlloc ? 8 : Math.max(8, headerSize + dataSize)];
				headerAndData[0] = Packet.SLICE_PROTOCOL_VERSION_ID_01;
				//if (packet.isStart())
				{
					//headerAndData[1] |= Packet.SLICE_FLAG_START;
				}
				//if (packet.isEnd())
				{
					//headerAndData[1] |= Packet.SLICE_FLAG_END;
				}
				//headerAndData[2] = (byte) (packet.getId() >> 24);
				//headerAndData[3] = (byte) (packet.getId() >> 16);
				//headerAndData[4] = (byte) (packet.getId() >> 8);
				//headerAndData[5] = (byte) (packet.getId());
				headerAndData[6] = (byte) (dataSize >> 8);
				headerAndData[7] = (byte) (dataSize);

				//if (packet.getData() != null && !noAlloc && data != null)
				//{
				//	System.arraycopy(data, 0, headerAndData, 8, data.length);
				//}

				//packet.setData(headerAndData);
				//return packet;
			}
			else
			{
				if (rawItem != null)
				{
					//packet.setData(item.getData());
					//return packet;
				}

				if (dataSize == 0)
				{
					if (data != null)
					{
						dataSize = data.length;
					}
				}

				var headerAndData = new byte[noAlloc ? 8 : Math.max(8, headerSize + dataSize)];
				headerAndData[0] = (byte) version;
				headerAndData[1] = (byte) (service >> 8);
				headerAndData[2] = (byte) (service);
				headerAndData[3] = (byte) subPacket;
				headerAndData[4] = (byte) ((dataSize + headerSize) >> 24);
				headerAndData[5] = (byte) ((dataSize + headerSize) >> 16);
				headerAndData[6] = (byte) ((dataSize + headerSize) >> 8);
				headerAndData[7] = (byte) (dataSize + headerSize);

				if (data != null && !noAlloc)
				{
					System.arraycopy(data, 0, headerAndData, 8, data.length);
				}

				//packet.setData(headerAndData);
				//return packet;
			}
			return null;
		}

		public byte[] build()
		{
			//return buildPacket().getData();
			return new byte[0];
		}
	}

	public static Builder builder()
	{
		return new Builder();
	}
}
