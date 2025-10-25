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

package io.xeres.app.xrs.service.voip;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class LockBasedSingleEntrySupplier implements Supplier<byte[]>
{
	private byte[] buffer;
	private boolean dataAvailable;
	private final Lock lock = new ReentrantLock();
	private final Condition dataPresent = lock.newCondition();
	private final Condition spaceAvailable = lock.newCondition();

	@Override
	public byte[] get()
	{
		lock.lock();
		try
		{
			while (!dataAvailable)
			{
				dataPresent.await();
			}
			byte[] result = buffer;
			buffer = null;
			dataAvailable = false;
			spaceAvailable.signal();
			return result;
		}
		catch (InterruptedException _)
		{
			Thread.currentThread().interrupt();
			return new byte[0];
		}
		finally
		{
			lock.unlock();
		}
	}

	public void put(byte[] data)
	{
		lock.lock();
		try
		{
			while (dataAvailable)
			{
				spaceAvailable.await();
			}
			buffer = data;
			dataAvailable = true;
			dataPresent.signal();
		}
		catch (InterruptedException _)
		{
			Thread.currentThread().interrupt();
		}
		finally
		{
			lock.unlock();
		}
	}
}
