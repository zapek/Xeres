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

import java.time.Duration;

/// Computes the delay until the next transfer operation given the measured
/// bandwidth and the amount of data to transfer. Replaces the fixed delays that
/// used to cap throughput regardless of the actual bandwidth.
final class BandwidthScheduler
{
	private static final long MIN_DELAY_MILLIS = 50;
	private static final long MAX_DELAY_MILLIS = 2_000;

	private BandwidthScheduler()
	{
	}

	/// Computes how long it takes to transfer [bytesToTransfer] at the given
	/// [bytesPerSecond] rate.
	///
	/// The delay is the time it takes to drain the data at the measured rate, so
	/// a fast peer isn't starved and a slow one isn't over-requested. When no
	/// rate has been measured yet ([bytesPerSecond] is zero or negative), the
	/// [fallback] is returned so the first operation still happens promptly.
	///
	/// @param bytesPerSecond  the measured transfer rate in bytes per second
	/// @param bytesToTransfer the amount of data to transfer
	/// @param fallback        the delay to use when no rate is known
	/// @return the delay until the next transfer operation
	static Duration delayFor(long bytesPerSecond, int bytesToTransfer, Duration fallback)
	{
		if (bytesPerSecond <= 0 || bytesToTransfer <= 0)
		{
			return fallback;
		}
		var millis = (long) Math.ceil(bytesToTransfer * 1000.0 / bytesPerSecond);
		return Duration.ofMillis(Math.clamp(millis, MIN_DELAY_MILLIS, MAX_DELAY_MILLIS));
	}
}
