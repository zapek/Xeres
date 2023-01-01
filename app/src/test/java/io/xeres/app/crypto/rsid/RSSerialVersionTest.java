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

package io.xeres.app.crypto.rsid;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;

import static io.xeres.app.crypto.rsid.RSSerialVersion.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RSSerialVersionTest
{
	@Test
	void RSSerialVersion_Enum_Order()
	{
		assertEquals(0, V06_0000.ordinal());
		assertEquals(1, V06_0001.ordinal());
		assertEquals(2, V07_0001.ordinal());

		assertEquals(3, values().length);
	}

	@Test
	void RSSerialVersion_GetFromSerialNumber_OK()
	{
		var RS_Old = new BigInteger(Integer.toString(ThreadLocalRandom.current().nextInt(100000, 2000000000)), 16);
		var RS_6_4 = new BigInteger("60000", 16);
		var RS_6_5 = new BigInteger("60001", 16);
		var RS_7 = new BigInteger("70001", 16);

		assertEquals(V06_0000, RSSerialVersion.getFromSerialNumber(RS_6_4));
		assertEquals(RSSerialVersion.V06_0001, RSSerialVersion.getFromSerialNumber(RS_6_5));
		assertEquals(RSSerialVersion.V07_0001, RSSerialVersion.getFromSerialNumber(RS_7));
		assertEquals(V06_0000, RSSerialVersion.getFromSerialNumber(RS_Old));
	}
}
