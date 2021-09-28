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

import static io.xeres.app.crypto.rsid.shortinvite.ShortInviteQuirks.swapBytes;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ShortInviteQuirksTest
{
	@Test
	void ShortInviteQuirks_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(ShortInviteQuirks.class);
	}

	@Test
	void ShortInviteQuirks_SwapBytes_OK()
	{
		var INPUT = new byte[]{1, 2, 3, 4, 5, 6};
		var OUTPUT = new byte[]{4, 3, 2, 1, 5, 6};

		assertArrayEquals(OUTPUT, swapBytes(INPUT));
	}

	@Test
	void ShortInviteQuirks_SwapBytes_WrongInput_NoSwap()
	{
		var INPUT = new byte[]{1, 2, 3, 4, 5, 6, 7};
		var OUTPUT = new byte[]{1, 2, 3, 4, 5, 6, 7};

		assertArrayEquals(OUTPUT, swapBytes(INPUT));
	}
}
