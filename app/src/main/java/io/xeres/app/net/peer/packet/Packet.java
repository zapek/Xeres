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

package io.xeres.app.net.peer.packet;

import io.netty.buffer.ByteBuf;
import io.xeres.app.xrs.item.RawItem;

import java.net.ProtocolException;
import java.util.Objects;

public abstract class Packet implements Comparable<Packet>
{
	/**
	 * Version of the packet protocol with slicing and grouping support.
	 * Also referred as new format.
	 */
	public static final int SLICE_PROTOCOL_VERSION_ID_01 = 16;

	/**
	 * Size of the header. Same for both packet protocols.
	 */
	public static final int HEADER_SIZE = 8;

	/**
	 * Optimal packet size for the new format. It fits better
	 * in the SSL encapsulation.
	 */
	public static final int OPTIMAL_PACKET_SIZE = 512;

	/**
	 * The maximum packet size, which is the buffer size per connection
	 * used by Retroshare, actually.
	 */
	public static final int MAXIMUM_PACKET_SIZE = 262_143;

	protected int priority = 3;
	private int sequence;

	protected ByteBuf buf;

	public static Packet fromItem(RawItem rawItem)
	{
		Packet packet;
		//if (rawItem.getPacketVersion() == 2) // this handles slice prods, which HAVE to use the old format, for now
		//{
		//	packet = new SimplePacket(rawItem.getBuffer());
		//}
		//return new MultiPacket(item.getBuffer()); // XXX: when the encoder is ready
		packet = new SimplePacket(rawItem.getBuffer());
		packet.setPriority(rawItem.getPriority());
		return packet;
	}

	public static Packet fromBuffer(ByteBuf in) throws ProtocolException
	{
		return MultiPacket.isNewPacket(in) ? new MultiPacket(in) : new SimplePacket(in);
	}

	protected Packet()
	{
	}

	public boolean isMulti()
	{
		return this instanceof MultiPacket;
	}

	public abstract boolean isComplete();

	public abstract int getSize();

	public abstract ByteBuf getItemBuffer();

	void setBuffer(ByteBuf buf) // XXX: for tests... check if it works well enough
	{
		this.buf = buf;
	}

	public ByteBuf getBuffer()
	{
		return buf;
	}

	public int getPriority()
	{
		return priority;
	}

	public void setPriority(int priority)
	{
		this.priority = priority;
	}

	public boolean isRealtimePriority()
	{
		return priority == 9; // XXX: make it nicer
	}

	public void setSequence(int sequence) // XXX: possibly in new packets only. not sure though
	{
		this.sequence = sequence;
	}

	public void dispose()
	{
		buf.release();
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		var packet = (Packet) o;
		return priority == packet.priority && sequence == packet.sequence && buf.equals(packet.buf);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(priority, sequence, buf);
	}

	@Override
	public int compareTo(Packet o)
	{
		var res = getPriority() - o.getPriority();

		if (res == 0 && o != this)
		{
			res = o.sequence - sequence;
		}
		return res;
	}
}
