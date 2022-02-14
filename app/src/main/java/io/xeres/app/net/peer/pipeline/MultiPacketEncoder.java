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
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.ScheduledFuture;
import io.xeres.app.net.peer.PeerAttribute;
import io.xeres.app.net.peer.packet.Packet;

import java.util.PriorityQueue;

import static io.xeres.app.net.peer.packet.MultiPacket.MAXIMUM_ID;

// XXX: this is a mess... rewrite it later when I'll have a better architecture. basically I don't even know if I can do that with netty properly
// something like... intercept the writabilityChanged event... don't pass it up as we can still fill in our queue (then pass it, to not gobble all memory).
// then write once we get the event again.. and so on. I think it has to be a ChannelDuplex subclass too! though I don't see where I can get the events... sigh
// or... just write() then use flush() after either 1/2 or 1/4 of a seconds or if a high priority packet comes (should the high priority one really get in front?)
// if it does have to then it's a bit more complicated
public class MultiPacketEncoder extends ChannelOutboundHandlerAdapter // XXX: must extend ChannelOutboundHandlerAdapter
{
	private static final boolean USE_PACKET_SLICING = false; // XXX: set that to TRUE to get the "proper" logic...
	private static final boolean USE_PACKET_GROUPING = false;
	private static final boolean USE_QOS = false;

	private final PriorityQueue<Packet> queue = new PriorityQueue<>();
	private ScheduledFuture<Void> flusher;

	private int written;
	private final int hiWater = Packet.OPTIMAL_PACKET_SIZE;
	private int packetId;

	// XXX: we can override read() too!

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
	{
		var packet = (Packet) msg;

		if (USE_PACKET_SLICING && Boolean.TRUE.equals(ctx.channel().attr(PeerAttribute.MULTI_PACKET).get()))
		{
			if (USE_PACKET_GROUPING)
			{
				if (getPacketSizeWithHeader(packet) + written > hiWater)
				{
					// Slice and send (note that original RS doesn't do it)
				}
			}
			// XXX: if we add to the queue, we need to send a new promise I think... so it knows it was "written" and can read more
			// XXX: well, same problem then! it needs ctx.write(Unpooled.EMPTY_BUFFER, promise)

			enqueue(packet);
			if (packet.isRealtimePriority())
			{
				flusher.cancel(false);
				//writePacket(ctx);
				ctx.flush();
			}
			else
			{
				if (false)
				{
					// XXX: this doesn't work because MessageToByteEncoder does send an empty buffer if we didn't write anything. do we have to subclass ChannelOutboundHandlerAdapter then? this seems complicated... there must be an easier way
					// XXX: actually! maybe the "easier" way would be to queue before sending...

					// XXX: otherwise! just issue write() calls here and only flush() with the executor. it's the flush() which executes the send() syscall
					// so just fill with write() until we reach 512? and either flush directly or with the executor?
					// XXX: !!! there's a FlushConsolidationHandler! Check it! -> well, no. this is not what we want
					//ByteBuf slice = out.retainedSlice();
					//flusher = ctx.executor().schedule(() -> writePacket(ctx, slice), 250, TimeUnit.MILLISECONDS); // XXX: will have to tweak the delays... also depends on the packet priority I guess. MAKE SURE THIS RUNS ON THE SAME THREAD as encode()!!!
				}
				else
				{
					//writePacket(ctx, out);
				}
			}
		}
		else
		{
			// Send the old way
			//ctx.writeAndFlush(packet.getData(), promise); // XXX: is this correct?! maybe not...
		}
	}

	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception
	{
		flusher.cancel(false);
		// XXX: do we have to wait for writeFuture()?!
		super.close(ctx, promise);
	}

	private void enqueue(Packet packet)
	{
		var remainingSize = packet.getSize();

		if (remainingSize >= Packet.OPTIMAL_PACKET_SIZE)
		{
			var offset = 0;
			var sequence = 0;

			while (remainingSize > 0)
			{
				var copySize = Math.min(remainingSize, Packet.OPTIMAL_PACKET_SIZE);

				//RsPacket slice = new RsPacket(packet.getPriority());
				var newData = new byte[copySize];
				//System.arraycopy(packet.getData(), offset, newData, 0, copySize);
				//slice.setData(newData);
				//slice.setId(packetId);
				//slice.setSequence(sequence);
				if (offset == 0)
				{
					//slice.setStart(true);
				}

				sequence++;
				offset += copySize;
				remainingSize -= copySize;
				if (remainingSize == 0)
				{
					//slice.setEnd(true);
				}
				//queue.add(slice);
			}
			incrementId();
		}
		else
		{
			queue.add(packet); // XXX: how do we ensure this won't grow too much? (ie. peer stopped responding...). we probably have to send some "read" event down the line
		}
	}

	private void writePacket(ChannelHandlerContext ctx, Packet packet, ByteBuf out)
	{
		var sizeWithHeader = getPacketSizeWithHeader(packet);

		out.ensureWritable(sizeWithHeader); // XXX: IndexOutOfBoundsException -> close the connection if it's the case. maybe it already does it

		out.writeByte(Packet.SLICE_PROTOCOL_VERSION_ID_01);
		//out.writeByte(packet.getFlags());
		//out.writeInt(packet.getId());
		out.writeShort(packet.getSize());
		//out.writeBytes(packet.getData());

		ctx.write(out);
		written += sizeWithHeader;
	}

	private int getPacketSizeWithHeader(Packet packet)
	{
		return packet.getSize() + 8;
	}

	private void incrementId()
	{
		packetId++;
		if (packetId >= MAXIMUM_ID)
		{
			packetId = 0;
		}
	}
}
