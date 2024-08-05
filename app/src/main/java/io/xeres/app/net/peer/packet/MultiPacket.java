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

import java.net.ProtocolException;

/**
 * This packet supports slicing and grouping for a more efficient
 * transmission over an SSL link.
 */
public class MultiPacket extends Packet
{
	/**
	 * Maximum packet ID. Wraps around.
	 */
	public static final int MAXIMUM_ID = 16_777_216;

	/**
	 * Flag set for starting packets and full packets
	 * in the new format.
	 */
	public static final int SLICE_FLAG_START = 1;

	/**
	 * Flag set for ending packets and full packets
	 * in the new format.
	 */
	public static final int SLICE_FLAG_END = 2;

	private static final int HEADER_VERSION_INDEX = 0;
	private static final int HEADER_FLAG_INDEX = 1;
	private static final int HEADER_PACKET_ID_INDEX = 2;
	public static final int HEADER_SIZE_INDEX = 6;

	protected static boolean isNewPacket(ByteBuf in) throws ProtocolException
	{
		if (in.getUnsignedByte(HEADER_VERSION_INDEX) == SLICE_PROTOCOL_VERSION_ID_01)
		{
			var id = (int) in.getUnsignedInt(HEADER_PACKET_ID_INDEX);
			if (id >= MAXIMUM_ID || id < 0)
			{
				throw new ProtocolException("Illegal packet id (" + id + ")");
			}
			return true;
		}
		return false;
	}

	protected MultiPacket(ByteBuf in)
	{
		buf = in.retain();
	}

	public void setStart()
	{
		addFlags(SLICE_FLAG_START);
	}

	public boolean isStart()
	{
		return (getFlags() & SLICE_FLAG_START) == SLICE_FLAG_START;
	}

	public void setEnd()
	{
		addFlags(SLICE_FLAG_END);
	}

	public boolean isEnd()
	{
		return (getFlags() & SLICE_FLAG_END) == SLICE_FLAG_END;
	}

	public boolean isMiddle()
	{
		return !(isStart() || isEnd());
	}

	public boolean isSlice()
	{
		return !isComplete();
	}

	public void setId(int id)
	{
		buf.setInt(HEADER_PACKET_ID_INDEX, id);
	}

	public int getId()
	{
		return (int) buf.getUnsignedInt(HEADER_PACKET_ID_INDEX);
	}

	@Override
	public int getSize()
	{
		return buf.getUnsignedShort(HEADER_SIZE_INDEX);
	}

	@Override
	public ByteBuf getItemBuffer()
	{
		return getBuffer().slice(HEADER_SIZE, getSize());
	}

	private int getFlags()
	{
		return buf.getUnsignedByte(HEADER_FLAG_INDEX);
	}

	private void addFlags(int newFlags)
	{
		int currentFlags = buf.getUnsignedByte(HEADER_FLAG_INDEX);
		currentFlags |= newFlags;
		buf.setByte(HEADER_FLAG_INDEX, currentFlags);
	}

	private void removeFlags(int newFlags)
	{
		int currentFlags = buf.getUnsignedByte(HEADER_FLAG_INDEX);
		currentFlags &= ~newFlags;
		buf.setByte(HEADER_FLAG_INDEX, currentFlags);
	}

	@Override
	public boolean isComplete()
	{
		return isStart() && isEnd();
	}
}
