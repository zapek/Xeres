/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.common.util;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class ByteUnitUtilsBenchmark
{
	@State(Scope.Thread)
	public static class InputData
	{
		public long[] values;

		@Setup(Level.Trial)
		public void setup()
		{
			int count = 1_000_000;
			values = new long[count];
			var r = new Random(12345); // fixed seed for reproducibility
			for (int i = 0; i < count; i++)
			{
				values[i] = Math.abs(r.nextLong());
				if (values[i] == 0)
				{
					values[i] = 1;
				}
			}
		}
	}

	@Benchmark
	public void fromBytes(InputData input, Blackhole bh)
	{
		var v = input.values;
		for (long l : v)
		{
			bh.consume(ByteUnitUtils.fromBytes(l));
		}
	}
}
