/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.ReferenceCountUtil;
import io.xeres.app.net.peer.packet.MultiPacketBuilder;
import io.xeres.app.net.peer.packet.SimplePacketBuilder;
import io.xeres.app.net.peer.pipeline.ItemDecoder;
import io.xeres.app.net.peer.pipeline.PacketDecoder;
import io.xeres.app.xrs.item.RawItem;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.junit.jupiter.api.Test;

import java.net.ProtocolException;
import java.util.concurrent.ThreadLocalRandom;

import static io.xeres.app.net.peer.packet.MultiPacket.SLICE_FLAG_END;
import static io.xeres.app.net.peer.packet.MultiPacket.SLICE_FLAG_START;
import static io.xeres.app.net.peer.packet.Packet.OPTIMAL_PACKET_SIZE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class PacketDecoderPipelineTest extends AbstractPipelineTest
{
	@Test
	void RsFrameDecoder_NewPacket_OK()
	{
		var channel = new EmbeddedChannel(new PacketDecoder());

		var inPacket = MultiPacketBuilder.builder()
				.build();

		channel.writeInbound(Unpooled.wrappedBuffer(inPacket));
		ByteBuf inBuf = channel.readInbound();
		assertArrayEquals(inPacket, getByteBufAsArray(inBuf));

		ReferenceCountUtil.release(inBuf);
	}

	@Test
	void RsFrameDecoder_NewPacket_ZeroSize()
	{
		var channel = new EmbeddedChannel(new PacketDecoder(), new ItemDecoder());

		var inPacket = MultiPacketBuilder.builder()
				.build();

		channel.writeInbound(Unpooled.wrappedBuffer(inPacket));
		RawItem rawItem = channel.readInbound();
		assertNotNull(rawItem);

		ReferenceCountUtil.release(rawItem);
	}

	@Test
	void RsFrameDecoder_OldPacket_OK()
	{
		var channel = new EmbeddedChannel(new PacketDecoder());

		var inPacket = SimplePacketBuilder.builder()
				.build();

		channel.writeInbound(Unpooled.wrappedBuffer(inPacket));
		ByteBuf inBuf = channel.readInbound();
		assertArrayEquals(inPacket, getByteBufAsArray(inBuf));

		ReferenceCountUtil.release(inBuf);
	}

	@Test
	void RsFrameDecoder_OldPacket_TooSmall()
	{
		var channel = new EmbeddedChannel(new PacketDecoder(), new ItemDecoder());

		var inPacket = SimplePacketBuilder.builder()
				.setHeaderSize(6)
				.build();

		assertThatThrownBy(() -> {
			channel.writeInbound(Unpooled.wrappedBuffer(inPacket));
			channel.checkException();
		}).isInstanceOf(DecoderException.class)
				.hasCauseInstanceOf(ProtocolException.class)
				.hasMessageContaining("Packet size too small");
	}

	/**
	 * The old packets can be oversized, the new ones can't.
	 */
	@Test
	void RsFrameDecoder_OldPacket_Oversized()
	{
		var channel = new EmbeddedChannel(new PacketDecoder(), new ItemDecoder());

		var inPacket = SimplePacketBuilder.builder()
				.setHeaderSize(Integer.MAX_VALUE - 8)
				.build();

		assertThatThrownBy(() -> {
			channel.writeInbound(Unpooled.wrappedBuffer(inPacket));
			channel.checkException();
		}).isInstanceOf(TooLongFrameException.class)
				.hasMessageStartingWith("Frame is too long");
	}

	@Test
	void RsPacketDecoder_OldPacket_Empty_OK()
	{
		var channel = new EmbeddedChannel(new PacketDecoder(), new ItemDecoder());

		var inPacket = SimplePacketBuilder.builder()
				.build();

		channel.writeInbound(Unpooled.wrappedBuffer(inPacket));
		RawItem rawItem = channel.readInbound();
		assertNotNull(rawItem);

		ReferenceCountUtil.release(rawItem);
	}

	@Test
	void RsPacketDecoder_NewPacket_Empty_DoubleStartPacket()
	{
		var channel = new EmbeddedChannel(new PacketDecoder(), new ItemDecoder());

		var inPacket = MultiPacketBuilder.builder()
				.setFlags(SLICE_FLAG_START)
				.build();

		channel.writeInbound(Unpooled.wrappedBuffer(inPacket));
		assertThatThrownBy(() -> {
			channel.writeInbound(Unpooled.wrappedBuffer(inPacket));
			channel.checkException();
		}).isInstanceOf(DecoderException.class)
				.hasCauseInstanceOf(ProtocolException.class)
				.hasMessageFindingMatch("Start packet [0-9]* already received");
	}

	@Test
	void RsPacketDecoder_NewPacket_Empty_MiddlePacketWithoutStartPacket()
	{
		var channel = new EmbeddedChannel(new PacketDecoder(), new ItemDecoder());

		var inPacket = MultiPacketBuilder.builder()
				.setFlags(0)
				.build();

		assertThatThrownBy(() -> {
			channel.writeInbound(Unpooled.wrappedBuffer(inPacket));
			channel.checkException();
		}).isInstanceOf(DecoderException.class)
				.hasCauseInstanceOf(ProtocolException.class)
				.hasMessageFindingMatch("Middle packet [0-9]* received without corresponding start packet");
	}

	@Test
	void RsPacketDecoder_NewPacket_Empty_EndPacketWithoutStartPacket()
	{
		var channel = new EmbeddedChannel(new PacketDecoder(), new ItemDecoder());

		var inPacket = MultiPacketBuilder.builder()
				.setFlags(SLICE_FLAG_END)
				.build();

		assertThatThrownBy(() -> {
			channel.writeInbound(Unpooled.wrappedBuffer(inPacket));
			channel.checkException();
		}).isInstanceOf(DecoderException.class)
				.hasCauseInstanceOf(ProtocolException.class)
				.hasMessageFindingMatch("End packet [0-9]* received without corresponding start packet");
	}

	@Test
	void RsPacketDecoder_NewPacket_Empty_OK()
	{
		var channel = new EmbeddedChannel(new PacketDecoder(), new ItemDecoder());

		var inPacket = MultiPacketBuilder.builder()
				.build();

		channel.writeInbound(Unpooled.wrappedBuffer(inPacket));
		RawItem rawItem = channel.readInbound();
		assertNotNull(rawItem);
		assertEquals(0, rawItem.getBuffer().writerIndex());
		assertFalse(channel.finish());

		ReferenceCountUtil.release(rawItem);
	}

	@Test
	void RsPacketDecoder_NewPacket_Slicing_SizesWithHeaders_OK()
	{
		var channel = new EmbeddedChannel(new PacketDecoder(), new ItemDecoder());

		var inPacket1 = MultiPacketBuilder.builder()
				.setPacketId(1)
				.setFlags(SLICE_FLAG_START)
				.setData(new byte[OPTIMAL_PACKET_SIZE])
				.build();

		var inPacket2 = MultiPacketBuilder.builder()
				.setPacketId(1)
				.setFlags(0)
				.setData(new byte[200])
				.build();

		var inPacket3 = MultiPacketBuilder.builder()
				.setPacketId(1)
				.setFlags(SLICE_FLAG_END)
				.setData(new byte[100])
				.build();

		channel.writeInbound(Unpooled.wrappedBuffer(inPacket1));
		channel.writeInbound(Unpooled.wrappedBuffer(inPacket2));
		channel.writeInbound(Unpooled.wrappedBuffer(inPacket3));

		RawItem rawItem = channel.readInbound();
		assertNotNull(rawItem);
		assertEquals(OPTIMAL_PACKET_SIZE + 200 + 100, rawItem.getBuffer().writerIndex());
		assertFalse(channel.finish());

		ReferenceCountUtil.release(rawItem);
	}

	/**
	 * Creates 3 sliced buffers and tests if they're reassembled properly.
	 */
	@Test
	void RsPacketDecoder_NewPacket_Slicing_DataIntegrity_OK()
	{
		var channel = new EmbeddedChannel(new PacketDecoder(), new ItemDecoder());

		var data1 = new byte[OPTIMAL_PACKET_SIZE];
		var data2 = new byte[200];
		var data3 = new byte[100];

		ThreadLocalRandom.current().nextBytes(data1);
		ThreadLocalRandom.current().nextBytes(data2);
		ThreadLocalRandom.current().nextBytes(data3);

		var hashIn = computeHash(data1, data2, data3);

		var inPacket1 = MultiPacketBuilder.builder()
				.setPacketId(1)
				.setFlags(SLICE_FLAG_START)
				.setData(data1)
				.build();

		var inPacket2 = MultiPacketBuilder.builder()
				.setPacketId(1)
				.setFlags(0)
				.setData(data2)
				.build();

		var inPacket3 = MultiPacketBuilder.builder()
				.setPacketId(1)
				.setFlags(SLICE_FLAG_END)
				.setData(data3)
				.build();

		channel.writeInbound(Unpooled.wrappedBuffer(inPacket1));
		channel.writeInbound(Unpooled.wrappedBuffer(inPacket2));
		channel.writeInbound(Unpooled.wrappedBuffer(inPacket3));

		RawItem rawItem = channel.readInbound();
		assertNotNull(rawItem);
		assertEquals(data1.length + data2.length + data3.length, rawItem.getBuffer().writerIndex());
		assertFalse(channel.finish());
		assertArrayEquals(hashIn, computeHash(getByteBufAsArray(rawItem.getBuffer())));

		ReferenceCountUtil.release(rawItem);
	}

	@Test
	void RsPacketDecoder_NewPacket_Slicing_DataIntegrity_Intermixed_OK()
	{
		var channel = new EmbeddedChannel(new PacketDecoder(), new ItemDecoder());

		var dataA1 = new byte[100];
		var dataA2 = new byte[150];
		var dataA3 = new byte[200];

		var dataB1 = new byte[300];
		var dataB2 = new byte[200];
		var dataB3 = new byte[100];

		ThreadLocalRandom.current().nextBytes(dataA1);
		ThreadLocalRandom.current().nextBytes(dataA2);
		ThreadLocalRandom.current().nextBytes(dataA3);

		ThreadLocalRandom.current().nextBytes(dataB1);
		ThreadLocalRandom.current().nextBytes(dataB2);
		ThreadLocalRandom.current().nextBytes(dataB3);

		var hashInA = computeHash(dataA1, dataA2, dataA3);
		var hashInB = computeHash(dataB1, dataB2, dataB3);

		var inPacketA1 = MultiPacketBuilder.builder()
				.setPacketId(1)
				.setFlags(SLICE_FLAG_START)
				.setData(dataA1)
				.build();

		var inPacketA2 = MultiPacketBuilder.builder()
				.setPacketId(1)
				.setFlags(0)
				.setData(dataA2)
				.build();

		var inPacketA3 = MultiPacketBuilder.builder()
				.setPacketId(1)
				.setFlags(SLICE_FLAG_END)
				.setData(dataA3)
				.build();

		var inPacketB1 = MultiPacketBuilder.builder()
				.setPacketId(2)
				.setFlags(SLICE_FLAG_START)
				.setData(dataB1)
				.build();

		var inPacketB2 = MultiPacketBuilder.builder()
				.setPacketId(2)
				.setFlags(0)
				.setData(dataB2)
				.build();

		var inPacketB3 = MultiPacketBuilder.builder()
				.setPacketId(2)
				.setFlags(SLICE_FLAG_END)
				.setData(dataB3)
				.build();

		channel.writeInbound(Unpooled.wrappedBuffer(inPacketA1));
		channel.writeInbound(Unpooled.wrappedBuffer(inPacketB1));
		channel.writeInbound(Unpooled.wrappedBuffer(inPacketA2));
		channel.writeInbound(Unpooled.wrappedBuffer(inPacketB2));
		channel.writeInbound(Unpooled.wrappedBuffer(inPacketA3));
		channel.writeInbound(Unpooled.wrappedBuffer(inPacketB3));

		RawItem rawItemA = channel.readInbound();
		RawItem rawItemB = channel.readInbound();
		assertNotNull(rawItemA);
		assertNotNull(rawItemB);
		assertEquals(dataA1.length + dataA2.length + dataA3.length, rawItemA.getBuffer().writerIndex());
		assertEquals(dataB1.length + dataB2.length + dataB3.length, rawItemB.getBuffer().writerIndex());
		assertFalse(channel.finish());
		assertArrayEquals(hashInA, computeHash(getByteBufAsArray(rawItemA.getBuffer())));
		assertArrayEquals(hashInB, computeHash(getByteBufAsArray(rawItemB.getBuffer())));

		ReferenceCountUtil.release(rawItemA);
		ReferenceCountUtil.release(rawItemB);
	}

	private byte[] computeHash(byte[]... buffers)
	{
		var hash = new byte[32];

		Digest digest = new SHA256Digest();
		for (var buf : buffers)
		{
			digest.update(buf, 0, buf.length);
		}
		digest.doFinal(hash, 0);
		return hash;
	}
}
