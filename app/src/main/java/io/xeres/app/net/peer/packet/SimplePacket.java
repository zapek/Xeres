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

import io.netty.buffer.ByteBuf;

/**
 * This is the old packet format of RS. It is still
 * used by RS in some cases (ie. transmission of a single small packet).
 */
public class SimplePacket extends Packet
{
	private static final int HEADER_VERSION_INDEX = 0;
	private static final int HEADER_SERVICE_INDEX = 1;
	private static final int HEADER_SUBPACKET_INDEX = 3;
	public static final int HEADER_SIZE_INDEX = 4;

	protected SimplePacket(ByteBuf in)
	{
		buf = in.retain();
	}

	@Override
	public int getSize()
	{
		return (int) buf.getUnsignedInt(HEADER_SIZE_INDEX);
	}

	@Override
	public ByteBuf getItemBuffer()
	{
		return getBuffer();
	}

	@Override
	public boolean isComplete()
	{
		return true;
	}
}
