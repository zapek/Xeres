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

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BandwidthSchedulerTest
{
	@Test
	void delayFor_NoRate_UsesFallback()
	{
		assertEquals(Duration.ofMillis(250), BandwidthScheduler.delayFor(0, 16384, Duration.ofMillis(250)));
	}

	@Test
	void delayFor_NegativeRate_UsesFallback()
	{
		assertEquals(Duration.ofMillis(50), BandwidthScheduler.delayFor(-1, 8192, Duration.ofMillis(50)));
	}

	@Test
	void delayFor_ComputesDrainTime()
	{
		// 16 KB at 1 MB/s = 16 ms, floored to 50 ms.
		assertEquals(Duration.ofMillis(50), BandwidthScheduler.delayFor(1024 * 1024, 16384, Duration.ofMillis(250)));
	}

	@Test
	void delayFor_SlowPeer_ComputesLongerDelay()
	{
		// 8 KB at 160 KB/s = 50 ms.
		assertEquals(Duration.ofMillis(50), BandwidthScheduler.delayFor(160 * 1024, 8192, Duration.ofMillis(50)));
	}

	@Test
	void delayFor_IsCappedByMaximum()
	{
		// 16 KB at 1 KB/s = 16 s, capped to 2 s.
		assertEquals(Duration.ofMillis(2_000), BandwidthScheduler.delayFor(1024, 16384, Duration.ofMillis(250)));
	}
}
