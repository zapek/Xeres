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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.xeres.app.net.peer.packet.Packet;
import io.xeres.app.xrs.item.RawItem;

import java.util.List;

@ChannelHandler.Sharable
public class ItemEncoder extends MessageToMessageEncoder<RawItem>
{
	@Override
	protected void encode(ChannelHandlerContext ctx, RawItem msg, List<Object> out)
	{
		out.add(Packet.fromItem(msg));
		msg.dispose();
	}
}
