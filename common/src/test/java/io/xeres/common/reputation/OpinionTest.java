/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.common.reputation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpinionTest
{
	@Test
	void From_Zero_ReturnsNegative()
	{
		assertEquals(Opinion.NEGATIVE, Opinion.from(0));
	}

	@Test
	void From_One_ReturnsNeutral()
	{
		assertEquals(Opinion.NEUTRAL, Opinion.from(1));
	}

	@Test
	void From_Two_ReturnsPositive()
	{
		assertEquals(Opinion.POSITIVE, Opinion.from(2));
	}

	@Test
	void From_InvalidValue_ReturnsNull()
	{
		assertNull(Opinion.from(3));
	}

	@Test
	void From_NegativeValue_ReturnsNull()
	{
		assertNull(Opinion.from(-1));
	}
}
