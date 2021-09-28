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

package io.xeres.app.net.peer.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.xeres.app.net.peer.packet.MultiPacket;
import io.xeres.app.net.peer.packet.Packet;
import io.xeres.app.net.peer.packet.SimplePacket;

import java.net.ProtocolException;
import java.util.List;

import static io.xeres.app.net.peer.packet.MultiPacket.MAXIMUM_PACKET_SIZE;
import static io.xeres.app.net.peer.packet.Packet.HEADER_SIZE;

/**
 * Decodes incoming frames into packets.
 */
public class PacketDecoder extends ByteToMessageDecoder
{
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
	{
		if (in.readableBytes() >= HEADER_SIZE)
		{
			long size;

			if (in.getUnsignedByte(in.readerIndex()) == Packet.SLICE_PROTOCOL_VERSION_ID_01)
			{
				size = (long) in.getUnsignedShort(in.readerIndex() + MultiPacket.HEADER_SIZE_INDEX) + HEADER_SIZE;
			}
			else
			{
				size = in.getUnsignedInt(in.readerIndex() + SimplePacket.HEADER_SIZE_INDEX);
			}

			if (size >= MAXIMUM_PACKET_SIZE - HEADER_SIZE)
			{
				throw new TooLongFrameException("Frame is too long: " + size);
			}
			else if (size < HEADER_SIZE)
			{
				throw new ProtocolException("Packet size too small, size: " + size);
			}

			if (in.readableBytes() >= size)
			{
				out.add(in.readBytes((int) size));
			}
		}
	}
}
