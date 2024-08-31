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

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import io.xeres.app.net.peer.packet.Packet;
import io.xeres.app.net.peer.packet.SimplePacketBuilder;
import io.xeres.app.net.peer.pipeline.SimplePacketEncoder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PacketEncoderPipelineTest extends AbstractPipelineTest
{
	@Test
	void RsOldPacketEncoder_Success()
	{
		var channel = new EmbeddedChannel(new SimplePacketEncoder());

		Packet outPacket = SimplePacketBuilder.builder().buildPacket();

		channel.writeAndFlush(outPacket);
		ByteBuf outBuf = channel.readOutbound();
		assertEquals(outPacket.getSize(), outBuf.writerIndex());
		ReferenceCountUtil.release(outBuf);
	}

	public void RsNewPacketEncoder_OK()
	{
//		var channel = new EmbeddedChannel(new MultiPacketEncoder());
//
//		Packet inPacket = PacketBuilder.builder().buildPacket();
//
//		channel.attr(PeerHandler.MULTI_PACKET).set(true);
//		channel.writeAndFlush(inPacket);
//		ByteBuf outBuf = channel.readOutbound();
//		assertEquals(inPacket.getSize(), outBuf.readableBytes());
//		ReferenceCountUtil.release(outBuf);
	}

	public void RsNewPacketEncoder_OldPacket_OK()
	{
//		var channel = new EmbeddedChannel(new MultiPacketEncoder());
//
//		Packet inPacket = PacketBuilder.builder()
//				.setData(new byte[]{1, 2, 3, 4})
//				.buildPacket();
//
//		channel.writeAndFlush(inPacket);
//		var outPacket = new byte[4];
//		ByteBuf outBuf = channel.readOutbound();
//		outBuf.readBytes(outPacket);
//		//assertArrayEquals(inPacket.getData(), outPacket);
//
//		ReferenceCountUtil.release(outBuf);
	}

	public void RsPacketEncoder_Small_OK()
	{
//		var channel = new EmbeddedChannel(new MultiPacketEncoder());
//
//		Packet inPacket = PacketBuilder.builder()
//				.setData(new byte[]{1, 2, 3, 4})
//				.buildPacket();
//
//		channel.attr(PeerHandler.MULTI_PACKET).set(true);
//		channel.writeAndFlush(inPacket);
//		channel.runPendingTasks();
//		var outPacket = new byte[4];
//		skipHeader(channel);
//		ByteBuf outBuf = channel.readOutbound();
//		outBuf.readBytes(outPacket);
//		//assertArrayEquals(inPacket.getData(), outPacket);
//
//		ReferenceCountUtil.release(outBuf);
	}

	public void RsPacketEncoder_Optimal_OK()
	{
//		EmbeddedChannel channel = new EmbeddedChannel(new MultiPacketEncoder());
//
//		Packet inPacket = PacketBuilder.builder()
//				.setRandomData(Packet.OPTIMAL_PACKET_SIZE)
//				.buildPacket();
//
//		channel.attr(PeerHandler.MULTI_PACKET).set(true);
//		channel.writeAndFlush(inPacket);
//		channel.runPendingTasks();
//		var outPacket = new byte[Packet.OPTIMAL_PACKET_SIZE];
//		skipHeader(channel);
//		ByteBuf outBuf = channel.readOutbound();
//		outBuf.readBytes(outPacket);
//		//assertArrayEquals(inPacket.getData(), outPacket);
//
//		ReferenceCountUtil.release(outBuf);
	}

	public void RsPacketEncoder_Big_OK()
	{
//		var channel = new EmbeddedChannel(new MultiPacketEncoder());
//
//		byte[] inPacket = PacketBuilder.builder()
//				.setRandomData(Packet.OPTIMAL_PACKET_SIZE * 3 + 200)
//				.build();
//
//		channel.attr(PeerHandler.MULTI_PACKET).set(true);
//		channel.writeAndFlush(Unpooled.wrappedBuffer(inPacket));
//		ByteBuf outBuf = channel.readOutbound();
//		byte[] outPacket = outBuf.array();
//		assertArrayEquals(inPacket, outPacket);
//
//		ReferenceCountUtil.release(outBuf);
	}

	public void RsPacketEncoder_Multiple_OK()
	{
//		var channel = new EmbeddedChannel(new MultiPacketEncoder());
//
//		byte[] inPacket1 = PacketBuilder.builder()
//				.setRandomData(Packet.OPTIMAL_PACKET_SIZE * 3 + 200)
//				.build();
//
//		byte[] inPacket2 = PacketBuilder.builder()
//				.setRandomData(6)
//				.build();
//
//		byte[] inPacket3 = PacketBuilder.builder()
//				.setRandomData(Packet.OPTIMAL_PACKET_SIZE * 2)
//				.build();
//
//		channel.attr(PeerHandler.MULTI_PACKET).set(true);
//		channel.writeAndFlush(Unpooled.wrappedBuffer(inPacket1));
//		channel.writeAndFlush(Unpooled.wrappedBuffer(inPacket2));
//		channel.writeAndFlush(Unpooled.wrappedBuffer(inPacket3));
//		ByteBuf outBuf1 = channel.readOutbound();
//		byte[] outPacket1 = outBuf1.array();
//		ByteBuf outBuf2 = channel.readOutbound();
//		byte[] outPacket2 = outBuf1.array();
//		ByteBuf outBuf3 = channel.readOutbound();
//		byte[] outPacket3 = outBuf1.array();
//
//		assertArrayEquals(inPacket1, outPacket1);
//		assertArrayEquals(inPacket2, outPacket2);
//		assertArrayEquals(inPacket3, outPacket3);
//
//		ReferenceCountUtil.release(outBuf1);
//		ReferenceCountUtil.release(outBuf2);
//		ReferenceCountUtil.release(outBuf3);
	}

	public void RsPacketEncoder_Multiple_Priority_OK()
	{
//		var channel = new EmbeddedChannel(new MultiPacketEncoder());
//
//		byte[] inPacket1 = PacketBuilder.builder()
//				.setRandomData(Packet.OPTIMAL_PACKET_SIZE * 3 + 200)
//				.build();
//
//		byte[] inPacket2 = PacketBuilder.builder()
//				.setRandomData(6)
//				.setPriority(9)
//				.build();
//
//		byte[] inPacket3 = PacketBuilder.builder()
//				.setRandomData(Packet.OPTIMAL_PACKET_SIZE * 2)
//				.build();
//
//		channel.attr(PeerHandler.MULTI_PACKET).set(true);
//		channel.writeAndFlush(Unpooled.wrappedBuffer(inPacket1));
//		channel.writeAndFlush(Unpooled.wrappedBuffer(inPacket2));
//		channel.writeAndFlush(Unpooled.wrappedBuffer(inPacket3));
//
//		ByteBuf outBuf1 = channel.readOutbound();
//		byte[] outPacket1 = outBuf1.array();
//		ByteBuf outBuf2 = channel.readOutbound();
//		byte[] outPacket2 = outBuf1.array();
//		ByteBuf outBuf3 = channel.readOutbound();
//		byte[] outPacket3 = outBuf1.array();
//
//		assertArrayEquals(inPacket1, outPacket1);
//		assertArrayEquals(inPacket2, outPacket2);
//		assertArrayEquals(inPacket3, outPacket3);
//
//		ReferenceCountUtil.release(outBuf1);
//		ReferenceCountUtil.release(outBuf2);
//		ReferenceCountUtil.release(outBuf3);
	}

	private void skipHeader(EmbeddedChannel channel)
	{
		ByteBuf byteBuf = channel.readOutbound();
		byteBuf.skipBytes(8);

		ReferenceCountUtil.release(byteBuf);
	}
}
