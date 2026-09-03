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

package io.xeres.common.rest.notification.status;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DhtInfoTest
{
	@Test
	void FromStatus_SetsDefaults()
	{
		var info = DhtInfo.fromStatus(DhtStatus.RUNNING);
		assertEquals(DhtStatus.RUNNING, info.dhtStatus());
		assertEquals(0, info.numPeers());
		assertEquals(0, info.receivedPackets());
		assertEquals(0, info.receivedBytes());
		assertEquals(0, info.sentPackets());
		assertEquals(0, info.sentBytes());
		assertEquals(0, info.keyCount());
		assertEquals(0, info.itemCount());
	}

	@Test
	void FromStats_SetsRunning()
	{
		var info = DhtInfo.fromStats(10, 200, 3000, 400, 5000, 60, 70);
		assertEquals(DhtStatus.RUNNING, info.dhtStatus());
		assertEquals(10, info.numPeers());
		assertEquals(200, info.receivedPackets());
		assertEquals(3000, info.receivedBytes());
		assertEquals(400, info.sentPackets());
		assertEquals(5000, info.sentBytes());
		assertEquals(60, info.keyCount());
		assertEquals(70, info.itemCount());
	}

	@Test
	void FromStatus_Off()
	{
		var info = DhtInfo.fromStatus(DhtStatus.OFF);
		assertEquals(DhtStatus.OFF, info.dhtStatus());
	}

	@Test
	void Record_Equality()
	{
		var info1 = DhtInfo.fromStats(10, 200, 3000, 400, 5000, 60, 70);
		var info2 = DhtInfo.fromStats(10, 200, 3000, 400, 5000, 60, 70);
		assertEquals(info1, info2);
		assertEquals(info1.hashCode(), info2.hashCode());
	}
}
