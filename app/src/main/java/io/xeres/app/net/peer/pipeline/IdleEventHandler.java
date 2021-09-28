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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleUserEventChannelHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.xrs.service.sliceprobe.item.SliceProbeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Event handler that automatically closes the connection if the peer doesn't send anything
 * during a certain time. We also send a SliceProbeItem if we're idle ourselves (which is unlikely
 * to happen during normal operations (ie. RTT and heartbeat services).
 */
@ChannelHandler.Sharable
public class IdleEventHandler extends SimpleUserEventChannelHandler<IdleStateEvent>
{
	private static final Logger log = LoggerFactory.getLogger(IdleEventHandler.class);

	private final Duration timeout;

	public IdleEventHandler(Duration timeout)
	{
		super();
		this.timeout = timeout;
	}

	@Override
	protected void eventReceived(ChannelHandlerContext ctx, IdleStateEvent evt)
	{
		if (evt.state() == IdleState.READER_IDLE)
		{
			log.info("No activity for {} seconds, closing channel of {}", timeout.toSeconds(), ctx.channel().remoteAddress());
			ctx.close();
		}
		else if (evt.state() == IdleState.WRITER_IDLE)
		{
			log.info("Sending idle slicing probe");
			PeerConnectionManager.writeItem(ctx, new SliceProbeItem());
		}
	}
}
