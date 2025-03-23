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

package io.xeres.app.xrs.service.bandwidth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BandwidthUtilsTest
{
	@Test
	void findBandwidthAnti()
	{
		var input = """
				"(PDH-CSV 4.0)","\\\\VITAMINB12\\Network Interface(Intel[R] I211 Gigabit Network Connection)\\Current Bandwidth","\\\\VITAMINB12\\Network Interface(Intel[R] I211 Gigabit Network Connection _2)\\Current Bandwidth","\\\\VITAMINB12\\Network Interface(ASUS USB-AC68 USB Wireless adapter)\\Current Bandwidth","\\\\VITAMINB12\\Network Interface(Xbox Wireless Adapter for Windows)\\Current Bandwidth"
				"03/23/2025 11:54:55.150","0.000000","0.000000","1300000000.000000","600000000.000000"
				""";

		assertEquals(1300000000L, BandwidthUtils.searchBandwidth(input));
	}

	@Test
	void findBandwidthZapek()
	{
		var input = """
				"(PDH-CSV 4.0)","\\\\B650\\Network Interface(Realtek Gaming 2.5GbE Family Controller)\\Current Bandwidth"
				"03/23/2025 08:36:48.197","2500000000.000000"
				""";

		assertEquals(2500000000L, BandwidthUtils.searchBandwidth(input));
	}
}