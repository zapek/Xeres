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

package io.xeres.app.net.peer.pipeline;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.xeres.app.net.peer.packet.MultiPacket;
import io.xeres.app.net.peer.packet.Packet;
import io.xeres.app.net.peer.packet.SimplePacket;
import io.xeres.app.xrs.item.RawItem;

import java.net.ProtocolException;
import java.util.*;

/**
 * Decodes RS Packets and produces a RawItem.
 */
public class ItemDecoder extends MessageToMessageDecoder<ByteBuf>
{
	private static final int MAX_SLICES = 195512; // maximum number of slices per packets (XXX: does RS have a limit there? I don't think so actually)
	private static final int MAX_CONCURRENT_PACKETS = 16; // maximum number of concurrent packets
	private final Map<Integer, List<MultiPacket>> accumulator = HashMap.newHashMap(MAX_CONCURRENT_PACKETS);

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws ProtocolException
	{
		var packet = Packet.fromBuffer(in);

		if (packet.isMulti())
		{
			decodeNewPacket(ctx, (MultiPacket) packet, out);
		}
		else
		{
			out.add(new RawItem(packet));
		}
	}

	// XXX: when an error happens (eg. slices exceeded), we should remove (dispose) all packets from the accumulator and refuse any further packet with such id because they're incomplete and will reach the decoding stage (and fail there)
	private void decodeNewPacket(ChannelHandlerContext ctx, MultiPacket packet, List<Object> out) throws ProtocolException
	{
		if (packet.isComplete())
		{
			if (accumulator.containsKey(packet.getId()))
			{
				throw new ProtocolException("Start packet " + packet.getId() + " already received");
			}
			out.add(new RawItem(packet));
		}
		else if (packet.isStart())
		{
			if (accumulator.containsKey(packet.getId()))
			{
				throw new ProtocolException("Start packet " + packet.getId() + " already received");
			}
			if (accumulator.size() > MAX_CONCURRENT_PACKETS)
			{
				throw new ProtocolException("Too many concurrent packets (" + accumulator.size() + ")");
			}
			var list = new ArrayList<MultiPacket>();
			list.add(packet);
			accumulator.put(packet.getId(), list);
		}
		else if (packet.isMiddle())
		{
			var list = Optional.ofNullable(accumulator.get(packet.getId())).orElseThrow(() -> new ProtocolException("Middle packet " + packet.getId() + " received without corresponding start packet"));
			if (list.size() > MAX_SLICES)
			{
				throw new ProtocolException("Packet " + packet.getId() + " has too many slices (" + list.size() + ")");
			}
			list.add(packet);
		}
		else if (packet.isEnd())
		{
			var list = Optional.ofNullable(accumulator.remove(packet.getId())).orElseThrow(() -> new ProtocolException("End packet " + packet.getId() + " received without corresponding start packet"));
			list.add(packet);
			out.add(new RawItem(new SimplePacket(ctx, list)));
		}
	}
}
