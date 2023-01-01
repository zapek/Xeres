/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.app.net.upnp;

import org.junit.jupiter.api.Test;

import static io.xeres.app.net.upnp.Protocol.TCP;
import static io.xeres.app.net.upnp.Protocol.UDP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PortMappingTest
{
	@Test
	void PortMapping_Compare_OK()
	{
		var mapping1 = new PortMapping(1025, TCP);
		var mapping2 = new PortMapping(1025, TCP);

		assertEquals(mapping1, mapping2);
	}

	@Test
	void PortMapping_Compare_InequalPort_Fail()
	{
		var mapping1 = new PortMapping(1025, TCP);
		var mapping2 = new PortMapping(1026, TCP);

		assertNotEquals(mapping1, mapping2);
	}

	@Test
	void PortMapping_Compare_InequalProtocols_Fail()
	{
		var mapping1 = new PortMapping(1025, TCP);
		var mapping2 = new PortMapping(1025, UDP);

		assertNotEquals(mapping1, mapping2);
	}
}
