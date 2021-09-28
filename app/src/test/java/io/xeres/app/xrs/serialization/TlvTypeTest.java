/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.serialization;

import org.junit.jupiter.api.Test;

import static io.xeres.app.xrs.serialization.TlvType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TlvTypeTest
{
	@Test
	void TlvType_Enum_Value()
	{
		assertEquals(0, NONE.getValue());
		assertEquals(0x51, STR_NAME.getValue());
		assertEquals(0x57, STR_MSG.getValue());
		assertEquals(0x5a, STR_GENID.getValue());
		assertEquals(0x5c, STR_LOCATION.getValue());
		assertEquals(0x5f, STR_VERSION.getValue());
		assertEquals(0x70, STR_HASH_SHA1.getValue());
		assertEquals(0x83, STR_DYNDNS.getValue());
		assertEquals(0x84, STR_DOM_ADDR.getValue());
		assertEquals(0x85, IPV4.getValue());
		assertEquals(0x86, IPV6.getValue());
		assertEquals(0xa4, STR_KEY_ID.getValue());
		assertEquals(0xb4, STR_SIGN.getValue());
		assertEquals(0x120, SIGN_RSA_SHA1.getValue());
		assertEquals(0x1023, SET_PGP_ID.getValue());
		assertEquals(0x1024, SET_RECOGN.getValue());
		assertEquals(0x1050, SIGNATURE.getValue());
		assertEquals(0x1070, ADDRESS_INFO.getValue());
		assertEquals(0x1071, ADDRESS_SET.getValue());
		assertEquals(0x1072, ADDRESS.getValue());
		assertEquals(0x2223, STRING.getValue());
	}
}
