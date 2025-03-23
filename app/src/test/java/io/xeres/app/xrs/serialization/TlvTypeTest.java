/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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
	void Enum_Value()
	{
		assertEquals(0, NONE.getValue());
		assertEquals(0x30, INT_SIZE.getValue());
		assertEquals(0x31, INT_POPULARITY.getValue());
		assertEquals(0x32, INT_AGE.getValue());
		assertEquals(0x35, INT_BANDWIDTH.getValue());
		assertEquals(0x41, LONG_OFFSET.getValue());
		assertEquals(0x51, STR_NAME.getValue());
		assertEquals(0x52, STR_PATH.getValue());
		assertEquals(0x54, STR_VALUE.getValue());
		assertEquals(0x57, STR_MSG.getValue());
		assertEquals(0x5a, STR_GENID.getValue());
		assertEquals(0x5c, STR_LOCATION.getValue());
		assertEquals(0x5f, STR_VERSION.getValue());
		assertEquals(0x70, STR_HASH_SHA1.getValue());
		assertEquals(0x83, STR_DYNDNS.getValue());
		assertEquals(0x84, STR_DOM_ADDR.getValue());
		assertEquals(0x85, IPV4.getValue());
		assertEquals(0x86, IPV6.getValue());
		assertEquals(0xa0, STR_GROUP_ID.getValue());
		assertEquals(0xa4, STR_KEY_ID.getValue());
		assertEquals(0xb3, STR_DESCR.getValue());
		assertEquals(0xb4, STR_SIGN.getValue());
		assertEquals(0x110, KEY_EVP_PKEY.getValue());
		assertEquals(0x120, SIGN_RSA_SHA1.getValue());
		assertEquals(0x130, BIN_IMAGE.getValue());
		assertEquals(0x140, BIN_FILE_DATA.getValue());
		assertEquals(0x1000, FILE_ITEM.getValue());
		assertEquals(0x1002, FILE_DATA.getValue());
		assertEquals(0x1022, SET_HASH.getValue());
		assertEquals(0x1023, SET_PGP_ID.getValue());
		assertEquals(0x1024, SET_RECOGN.getValue());
		assertEquals(0x1025, SET_GXS_ID.getValue());
		assertEquals(0x1028, SET_GXS_MSG_ID.getValue());
		assertEquals(0x1040, SECURITY_KEY.getValue());
		assertEquals(0x1041, SECURITY_KEY_SET.getValue());
		assertEquals(0x1050, SIGNATURE.getValue());
		assertEquals(0x1051, SIGNATURE_SET.getValue());
		assertEquals(0x1052, SIGNATURE_TYPE.getValue());
		assertEquals(0x1060, IMAGE.getValue());
		assertEquals(0x1070, ADDRESS_INFO.getValue());
		assertEquals(0x1071, ADDRESS_SET.getValue());
		assertEquals(0x1072, ADDRESS.getValue());
		assertEquals(0xffff, UNKNOWN.getValue());

		assertEquals(43, values().length);
	}
}
