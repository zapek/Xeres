/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.turtle;

import com.sangupta.bloomfilter.AbstractBloomFilter;
import com.sangupta.bloomfilter.core.BitArray;
import com.sangupta.bloomfilter.core.MMapFileBackedBitArray;
import io.xeres.common.id.Sha1Sum;

import java.io.File;

public class TurtleBloomFilter extends AbstractBloomFilter<Sha1Sum>
{
	private BitArray bArray;

	protected TurtleBloomFilter(int expectedInsertions, double falsePositiveProbability)
	{
		super(expectedInsertions, falsePositiveProbability, (sha1Sum, byteSink) -> byteSink.putBytes(sha1Sum.getBytes()));
	}

	@Override
	protected BitArray createBitArray(int numBits)
	{
		try
		{
			bArray = new MMapFileBackedBitArray(new File("foo"), numBits);
			return bArray;
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean contains(Sha1Sum value)
	{
		return super.contains(value.getBytes()); // XXX: the following workaround is needed until https://github.com/sangupta/bloomfilter/pull/5 is merged and a new upstream release is done
	}

	public void clear()
	{
		bArray.clear();
	}
}