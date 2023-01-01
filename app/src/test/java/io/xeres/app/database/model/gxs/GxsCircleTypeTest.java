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

import static io.xeres.app.database.model.gxs.GxsCircleType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class GxsCircleTypeTest
{
	@Test
	void GxsCircleType_Enum_Order()
	{
		assertEquals(0, UNKNOWN.ordinal());
		assertEquals(1, PUBLIC.ordinal());
		assertEquals(2, EXTERNAL.ordinal());
		assertEquals(3, YOUR_FRIENDS_ONLY.ordinal());
		assertEquals(4, LOCAL.ordinal());
		assertEquals(5, EXTERNAL_SELF.ordinal());
		assertEquals(6, YOUR_EYES_ONLY.ordinal());

		assertEquals(7, values().length);
	}
}
