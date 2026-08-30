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

package io.xeres.app.xrs.service.filetransfer;

import java.util.ArrayDeque;
import java.util.Deque;

/// Tracks the amount of bytes transferred over a sliding time window and
/// produces a smoothed rate in bytes per second. The rate is computed as the
/// sum of the samples within the window divided by the window duration, then
/// smoothed with an exponential moving average to avoid sudden variations.
/// This is the primary signal used to estimate the achievable bandwidth
/// towards a peer.
final class RateTracker
{
	private static final double SMOOTHING_FACTOR = 0.25;

	private final long windowNanos;
	private final Deque<Sample> samples = new ArrayDeque<>();
	private long smoothedRate;

	private record Sample(long timestamp, long bytes)
	{
	}

	RateTracker(long windowMillis)
	{
		windowNanos = windowMillis * 1_000_000L;
	}

	/// Records that some bytes have been transferred.
	///
	/// @param bytes the number of bytes
	void addBytes(long bytes)
	{
		var now = System.nanoTime();
		samples.addLast(new Sample(now, bytes));
		prune(now);
	}

	/// Returns the smoothed rate in bytes per second over the configured window.
	///
	/// @return the rate, or 0 if no bytes have been recorded within the window
	long getBytesPerSecond()
	{
		var now = System.nanoTime();
		prune(now);
		if (samples.isEmpty())
		{
			smoothedRate = 0;
			return 0;
		}

		var elapsedSec = Math.max(windowNanos / 1_000_000_000.0, 1.0);
		var bytes = samples.stream().mapToLong(Sample::bytes).sum();
		var measured = (long) (bytes / elapsedSec);

		if (smoothedRate == 0)
		{
			smoothedRate = measured;
		}
		else
		{
			smoothedRate = (long) (smoothedRate * (1.0 - SMOOTHING_FACTOR) + measured * SMOOTHING_FACTOR);
		}
		return smoothedRate;
	}

	private void prune(long now)
	{
		while (!samples.isEmpty() && now - samples.peekFirst().timestamp() > windowNanos)
		{
			samples.removeFirst();
		}
	}
}
