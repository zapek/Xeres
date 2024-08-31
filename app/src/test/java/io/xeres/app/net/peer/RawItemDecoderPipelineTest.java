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

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import io.xeres.app.net.peer.packet.MultiPacketBuilder;
import io.xeres.app.net.peer.packet.SimplePacketBuilder;
import io.xeres.app.net.peer.pipeline.ItemDecoder;
import io.xeres.app.net.peer.pipeline.PacketDecoder;
import io.xeres.app.xrs.item.RawItem;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.service.sliceprobe.item.SliceProbeItem;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RawItemDecoderPipelineTest extends AbstractPipelineTest
{
	@Test
	void NewPacket_Decode_Success()
	{
		var channel = new EmbeddedChannel(new PacketDecoder(), new ItemDecoder());

		var item = new SliceProbeItem();
		item.setOutgoing(ByteBufAllocator.DEFAULT, null);
		var itemIn = item.serializeItem(EnumSet.noneOf(SerializationFlags.class));

		var inPacket = MultiPacketBuilder.builder()
				.setRawItem(itemIn)
				.build();

		channel.writeInbound(Unpooled.wrappedBuffer(inPacket));

		RawItem rawItemOut = channel.readInbound();
		assertNotNull(rawItemOut);
		assertArrayEquals(getByteBufAsArray(itemIn.getBuffer()), getByteBufAsArray(rawItemOut.getBuffer()));

		ReferenceCountUtil.release(rawItemOut);
	}

	@Test
	void OldPacket_Decode_Success()
	{
		var channel = new EmbeddedChannel(new PacketDecoder(), new ItemDecoder());

		var item = new SliceProbeItem();
		item.setOutgoing(ByteBufAllocator.DEFAULT, null);
		var itemIn = item.serializeItem(EnumSet.noneOf(SerializationFlags.class));

		var inPacket = SimplePacketBuilder.builder()
				.setRawItem(itemIn)
				.build();

		channel.writeInbound(Unpooled.wrappedBuffer(inPacket));

		RawItem rawItemOut = channel.readInbound();
		assertNotNull(rawItemOut);
		assertArrayEquals(getByteBufAsArray(itemIn.getBuffer()), getByteBufAsArray(rawItemOut.getBuffer()));

		ReferenceCountUtil.release(rawItemOut);
	}
}
