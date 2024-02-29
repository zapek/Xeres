/*
 * Copyright (c) 2023-2024 by David Gerber - https://zapek.com
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

package io.xeres.app.service.file;

import com.sangupta.bloomfilter.AbstractBloomFilter;
import com.sangupta.bloomfilter.core.BitArray;
import com.sangupta.bloomfilter.core.JavaBitSetArray;
import com.sangupta.bloomfilter.core.MMapFileBackedBitArray;
import io.xeres.common.id.Sha1Sum;

import java.nio.file.Path;
import java.util.Collection;

/**
 * A Bloom filter implementation specifically designed for storing Turtle file hashes.
 * <p>
 * Use add() to insert entries and mightContain() to check if an entry might be in it. False positives
 * are possible and one just has to make sure that the probability is low enough so that accesses to the
 * database are kept at a minimum when not needed. In any case a match needs a database access for confirmation.
 * <p>
 * Removing an entry is not possible. One has to clear and re-add all entries.
 * <p>
 * The entries are persisted to disk.
 */
public class HashBloomFilter
{
	private static final String PERSISTENT_FILE = "turtle_bf";
	private final AbstractBloomFilter<Sha1Sum> bFilter;
	private BitArray bArray;

	public HashBloomFilter(String baseDir, int expectedInsertions, double falsePositiveProbability)
	{
		bFilter = new AbstractBloomFilter<>(expectedInsertions, falsePositiveProbability, (sha1Sum, byteSink) -> byteSink.putBytes(sha1Sum.getBytes()))
		{
			@Override
			protected BitArray createBitArray(int numBits)
			{
				if (baseDir == null)
				{
					bArray = new JavaBitSetArray(numBits);
					return bArray;
				}
				else
				{
					try
					{
						bArray = new MMapFileBackedBitArray(Path.of(baseDir, PERSISTENT_FILE).toFile(), numBits);
						return bArray;
					}
					catch (Exception e)
					{
						throw new RuntimeException(e);
					}
				}
			}

			@Override
			public boolean contains(Sha1Sum value)
			{
				return super.contains(value.getBytes()); // XXX: the following workaround (the getBytes() call) is needed until https://github.com/sangupta/bloomfilter/pull/5 is merged and a new upstream release is done
			}
		};
	}

	/**
	 * Adds a value.
	 *
	 * @param value the value to be added
	 */
	public void add(Sha1Sum value)
	{
		bFilter.add(value);
	}

	/**
	 * Adds all the values from the given collection.
	 *
	 * @param values the collection of values to be added
	 */
	public void addAll(Collection<Sha1Sum> values)
	{
		bFilter.addAll(values);
	}

	/**
	 * Determines if the given value might be in the bloom filter.
	 *
	 * @param value the value to check
	 * @return true if the value is possibly in it, false if it's definitely not
	 */
	public boolean mightContain(Sha1Sum value)
	{
		return bFilter.contains(value);
	}

	/**
	 * Determines if all the given values might be contained in the bloom filter.
	 *
	 * @param values the collection of values to check
	 * @return true if all the values are possibly in it, false if at least one is definitely not
	 */
	public boolean mightContainAll(Collection<Sha1Sum> values)
	{
		return bFilter.containsAll(values);
	}

	/**
	 * Clears the Bloom filter back to an empty state.
	 */
	public void clear()
	{
		bArray.clear();
	}
}