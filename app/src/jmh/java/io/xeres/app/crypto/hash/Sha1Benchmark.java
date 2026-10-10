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

package io.xeres.app.crypto.hash;

import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.app.crypto.hash.sha1dc.Sha1DcMessageDigest;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/// Compares the JDK SHA-1 ([Sha1MessageDigest]) against the collision-detecting
/// SHA-1 ([Sha1DcMessageDigest]) for normal (non-collision) inputs.
///
/// Run with: `./gradlew :app:jmh`
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class Sha1Benchmark
{
	@Param({"20", "16384", "1048576"})
	public int size;

	public byte[] data;

	@Setup(Level.Trial)
	public void setup()
	{
		data = new byte[size];
		new Random(12345).nextBytes(data); // fixed seed for reproducibility
	}

	@Benchmark
	public void jdkSha1(Blackhole bh)
	{
		var digest = new Sha1MessageDigest();
		digest.update(data);
		bh.consume(digest.getBytes());
	}

	@Benchmark
	public void sha1Dc(Blackhole bh)
	{
		var digest = new Sha1DcMessageDigest();
		digest.update(data);
		bh.consume(digest.getBytes());
	}
}
