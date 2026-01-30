/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.bandwidth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BandwidthUtilsTest
{
	@Test
	void findBandwidthOnWindowsAnti()
	{
		// This one has an unplugged ethernet interface with a secondary connection
		// The correct USB Wi-Fi dongle interface
		// And an incorrect XBox adapter
		var input = """
				0
				0
				1300000000
				600000000
				""";

		assertEquals(1_300_000_000L, BandwidthUtils.searchBandwidthOnWindows(input));
	}

	@Test
	void findBandwidthOnWindowsZapek()
	{
		// Mine only has one default ethernet interface
		var input = """
				2500000000
				""";

		assertEquals(2_500_000_000L, BandwidthUtils.searchBandwidthOnWindows(input));
	}

	@Test
	void findBandwidthOnLinux()
	{
		var input = "1000";

		assertEquals(1_000_000_000L, BandwidthUtils.searchBandwidthOnLinux(input));
	}

	@Test
	void findBandwidthOnMac()
	{
		var input = """
				en0: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500
					options=40b<RXCSUM,TXCSUM,VLAN_HWTAGGING,CHANNEL_IO>
					ether 00:0c:29:da:8c:2a\s
					inet6 fe80::184a:e26f:63c5:df33%en0 prefixlen 64 secured scopeid 0x4\s
					inet 192.168.136.128 netmask 0xffffff00 broadcast 192.168.136.255
					nd6 options=201<PERFORMNUD,DAD>
					media: autoselect (1000baseT <full-duplex>)
					status: active
				""";

		assertEquals(1_000_000_000L, BandwidthUtils.searchBandwidthOnMac(input));
	}
}