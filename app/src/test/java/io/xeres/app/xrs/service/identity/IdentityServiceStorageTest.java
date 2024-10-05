/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.identity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentityServiceStorageTest
{
	@Test
	void ParseIdentityString_Success()
	{
		var input = "v2 {P:K:1 I:133E525084DE5D4D}{T:F:4096 P:1614029822 T:1614029841}{R:50 50 0 0}";

		var identityServiceStorage = new IdentityServiceStorage(input);
		assertTrue(identityServiceStorage.isSuccess());
	}

	@Test
	void ParseIdentityString_Input_Output_Success()
	{
		var input = "v2 {P:K:1 I:133E525084DE5D4D}{T:F:4096 P:1614029822 T:1614029841}{R:50 50 0 0}";

		var identityServiceStorage = new IdentityServiceStorage(input);
		assertEquals(input, identityServiceStorage.out());
	}

	@Test
	void ParseIdentityString_Negative_Rating_Success()
	{
		var input = "v2 {P:K:1 I:133E525084DE5D4D}{T:F:4096 P:1614029822 T:1614029841}{R:50 -50 0 0}";

		var identityServiceStorage = new IdentityServiceStorage(input);
		assertTrue(identityServiceStorage.isSuccess());
	}

	@Test
	void ParseIdentityString_WrongVersion_Failure()
	{
		var input = "v1 {P:K:1 I:133E525084DE5D4D}{T:F:4096 P:1614029822 T:1614029841}{R:50 50 0 0}";

		var identityServiceStorage = new IdentityServiceStorage(input);
		assertFalse(identityServiceStorage.isSuccess());
	}

	@Test
	void ParseIdentityString_NegativePublish_Failure()
	{
		var input = "v2 {P:K:1 I:133E525084DE5D4D}{T:F:4096 P:-1 T:1614029841}{R:50 50 0 0}";

		var identityServiceStorage = new IdentityServiceStorage(input);
		assertFalse(identityServiceStorage.isSuccess());
	}
}