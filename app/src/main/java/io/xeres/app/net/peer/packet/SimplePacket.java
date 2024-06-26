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
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

/**
 * This is the old packet format of RS. It is still
 * used by RS in some cases (for example, transmission of a single small packet).
 */
public class SimplePacket extends Packet
{
	public static final int HEADER_SIZE_INDEX = 4;

	protected SimplePacket(ByteBuf in)
	{
		buf = in.retain();
	}

	public SimplePacket(ChannelHandlerContext ctx, List<MultiPacket> packets)
	{
		priority = packets.stream().findFirst().orElseThrow().getPriority();
		buf = ctx.alloc().buffer();
		packets.forEach(packet -> {
			buf.writeBytes(packet.getBuffer(), HEADER_SIZE, packet.getSize());
			packet.dispose();
		});
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
