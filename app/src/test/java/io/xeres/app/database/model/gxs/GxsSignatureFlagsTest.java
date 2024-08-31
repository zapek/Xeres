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

package io.xeres.app.database.model.gxs;

import org.junit.jupiter.api.Test;

import static io.xeres.app.database.model.gxs.GxsSignatureFlags.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GxsSignatureFlagsTest
{
	@Test
	void Enum_Order_Fixed()
	{
		assertEquals(0, ENCRYPTED.ordinal());
		assertEquals(1, ALL_SIGNED.ordinal());
		assertEquals(2, THREAD_HEAD.ordinal());
		assertEquals(3, NONE_REQUIRED.ordinal());
		assertEquals(4, UNUSED_1.ordinal());
		assertEquals(5, UNUSED_2.ordinal());
		assertEquals(6, UNUSED_3.ordinal());
		assertEquals(7, UNUSED_4.ordinal());
		assertEquals(8, ANTI_SPAM.ordinal());
		assertEquals(9, AUTHENTICATION_REQUIRED.ordinal());
		assertEquals(10, IF_NO_PUB_SIGN.ordinal());
		assertEquals(11, TRACK_MESSAGES.ordinal());
		assertEquals(12, ANTI_SPAM_2.ordinal());

		assertEquals(13, values().length);
	}
}
