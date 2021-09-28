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

package io.xeres.app.crypto.rsid.shortinvite;

import io.xeres.testutils.TestUtils;
import org.junit.jupiter.api.Test;

import static io.xeres.app.crypto.rsid.shortinvite.ShortInviteTags.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ShortInviteTagsTest
{
	@Test
	void ShortInviteTags_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ShortInviteTags.class);
	}

	@Test
	void ShortInviteTags_Values()
	{
		assertEquals(0x0, SSLID);
		assertEquals(0x1, NAME);
		assertEquals(0x2, LOCATOR);
		assertEquals(0x3, PGP_FINGERPRINT);
		assertEquals(0X4, CHECKSUM);
		assertEquals(0X90, HIDDEN_LOCATOR);
		assertEquals(0X91, DNS_LOCATOR);
		assertEquals(0X92, EXT4_LOCATOR);
		assertEquals(0X93, LOC4_LOCATOR);
	}
}
