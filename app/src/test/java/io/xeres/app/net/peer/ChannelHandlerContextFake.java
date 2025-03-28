/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;

import java.net.SocketAddress;

public class ChannelHandlerContextFake implements ChannelHandlerContext
{
	private final Channel channel = new ChannelFake();

	@Override
	public Channel channel()
	{
		return channel;
	}

	@Override
	public EventExecutor executor()
	{
		return null;
	}

	@Override
	public String name()
	{
		return "";
	}

	@Override
	public ChannelHandler handler()
	{
		return null;
	}

	@Override
	public boolean isRemoved()
	{
		return false;
	}

	@Override
	public ChannelHandlerContext fireChannelRegistered()
	{
		return null;
	}

	@Override
	public ChannelHandlerContext fireChannelUnregistered()
	{
		return null;
	}

	@Override
	public ChannelHandlerContext fireChannelActive()
	{
		return null;
	}

	@Override
	public ChannelHandlerContext fireChannelInactive()
	{
		return null;
	}

	@Override
	public ChannelHandlerContext fireExceptionCaught(Throwable throwable)
	{
		return null;
	}

	@Override
	public ChannelHandlerContext fireUserEventTriggered(Object o)
	{
		return null;
	}

	@Override
	public ChannelHandlerContext fireChannelRead(Object o)
	{
		return null;
	}

	@Override
	public ChannelHandlerContext fireChannelReadComplete()
	{
		return null;
	}

	@Override
	public ChannelHandlerContext fireChannelWritabilityChanged()
	{
		return null;
	}

	@Override
	public ChannelFuture bind(SocketAddress socketAddress)
	{
		return null;
	}

	@Override
	public ChannelFuture connect(SocketAddress socketAddress)
	{
		return null;
	}

	@Override
	public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1)
	{
		return null;
	}

	@Override
	public ChannelFuture disconnect()
	{
		return null;
	}

	@Override
	public ChannelFuture close()
	{
		return null;
	}

	@Override
	public ChannelFuture deregister()
	{
		return null;
	}

	@Override
	public ChannelFuture bind(SocketAddress socketAddress, ChannelPromise channelPromise)
	{
		return null;
	}

	@Override
	public ChannelFuture connect(SocketAddress socketAddress, ChannelPromise channelPromise)
	{
		return null;
	}

	@Override
	public ChannelFuture connect(SocketAddress socketAddress, SocketAddress socketAddress1, ChannelPromise channelPromise)
	{
		return null;
	}

	@Override
	public ChannelFuture disconnect(ChannelPromise channelPromise)
	{
		return null;
	}

	@Override
	public ChannelFuture close(ChannelPromise channelPromise)
	{
		return null;
	}

	@Override
	public ChannelFuture deregister(ChannelPromise channelPromise)
	{
		return null;
	}

	@Override
	public ChannelHandlerContext read()
	{
		return null;
	}

	@Override
	public ChannelFuture write(Object o)
	{
		return null;
	}

	@Override
	public ChannelFuture write(Object o, ChannelPromise channelPromise)
	{
		return null;
	}

	@Override
	public ChannelHandlerContext flush()
	{
		return null;
	}

	@Override
	public ChannelFuture writeAndFlush(Object o, ChannelPromise channelPromise)
	{
		return null;
	}

	@Override
	public ChannelFuture writeAndFlush(Object o)
	{
		return null;
	}

	@Override
	public ChannelPromise newPromise()
	{
		return null;
	}

	@Override
	public ChannelProgressivePromise newProgressivePromise()
	{
		return null;
	}

	@Override
	public ChannelFuture newSucceededFuture()
	{
		return null;
	}

	@Override
	public ChannelFuture newFailedFuture(Throwable throwable)
	{
		return null;
	}

	@Override
	public ChannelPromise voidPromise()
	{
		return null;
	}

	@Override
	public ChannelPipeline pipeline()
	{
		return null;
	}

	@Override
	public ByteBufAllocator alloc()
	{
		return null;
	}

	@Override
	public <T> Attribute<T> attr(AttributeKey<T> attributeKey)
	{
		return null;
	}

	@Override
	public <T> boolean hasAttr(AttributeKey<T> attributeKey)
	{
		return false;
	}
}
