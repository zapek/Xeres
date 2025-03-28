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

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class DefaultItemFuture implements ItemFuture
{
	private final Future<Void> future;
	private final int size;


	public DefaultItemFuture(Future<Void> future, int size)
	{
		this.future = future;
		this.size = size;
	}

	public DefaultItemFuture(Future<Void> future)
	{
		this.future = future;
		size = 0;
	}

	@Override
	public int getSize()
	{
		return size;
	}

	@Override
	public boolean isSuccess()
	{
		return future.isSuccess();
	}

	@Override
	public boolean isCancellable()
	{
		return future.isCancellable();
	}

	@Override
	public Throwable cause()
	{
		return future.cause();
	}

	@Override
	public Future<Void> addListener(GenericFutureListener<? extends Future<? super Void>> genericFutureListener)
	{
		return future.addListener(genericFutureListener);
	}

	@SafeVarargs
	@Override
	public final Future<Void> addListeners(GenericFutureListener<? extends Future<? super Void>>... genericFutureListeners)
	{
		return future.addListeners(genericFutureListeners);
	}

	@Override
	public Future<Void> removeListener(GenericFutureListener<? extends Future<? super Void>> genericFutureListener)
	{
		return future.removeListener(genericFutureListener);
	}

	@SafeVarargs
	@Override
	public final Future<Void> removeListeners(GenericFutureListener<? extends Future<? super Void>>... genericFutureListeners)
	{
		return future.removeListeners(genericFutureListeners);
	}

	@Override
	public Future<Void> sync() throws InterruptedException
	{
		return future.sync();
	}

	@Override
	public Future<Void> syncUninterruptibly()
	{
		return future.syncUninterruptibly();
	}

	@Override
	public Future<Void> await() throws InterruptedException
	{
		return future.await();
	}

	@Override
	public Future<Void> awaitUninterruptibly()
	{
		return future.awaitUninterruptibly();
	}

	@Override
	public boolean await(long l, TimeUnit timeUnit) throws InterruptedException
	{
		return future.await(l, timeUnit);
	}

	@Override
	public boolean await(long l) throws InterruptedException
	{
		return future.await(l);
	}

	@Override
	public boolean awaitUninterruptibly(long l, TimeUnit timeUnit)
	{
		return future.awaitUninterruptibly(l, timeUnit);
	}

	@Override
	public boolean awaitUninterruptibly(long l)
	{
		return future.awaitUninterruptibly(l);
	}

	@Override
	public Void getNow()
	{
		return future.getNow();
	}

	@Override
	public boolean cancel(boolean b)
	{
		return future.cancel(b);
	}

	@Override
	public boolean isCancelled()
	{
		return future.isCancelled();
	}

	@Override
	public boolean isDone()
	{
		return future.isDone();
	}

	@Override
	public Void get() throws InterruptedException, ExecutionException
	{
		return future.get();
	}

	@Override
	public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
	{
		return future.get(timeout, unit);
	}
}
