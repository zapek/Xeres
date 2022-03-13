/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.common;

import org.junit.jupiter.api.Test;

import static io.xeres.app.xrs.common.Image.Type.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageTest
{
	@Test
	void Image_Enum_order()
	{
		assertEquals(0, UNKNOWN.ordinal());
		assertEquals(1, PNG.ordinal());
		assertEquals(2, JPEG.ordinal());

		assertEquals(3, values().length);
	}
}
