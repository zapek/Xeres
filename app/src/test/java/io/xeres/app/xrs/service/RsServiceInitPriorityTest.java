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

package io.xeres.app.xrs.service;

import org.junit.jupiter.api.Test;

import static io.xeres.app.xrs.service.RsServiceInitPriority.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RsServiceInitPriorityTest
{
	@Test
	void RsServiceInitPriority_NoTimeOverlap_OK()
	{
		assertTrue(IMMEDIATE.getMaxTime() < HIGH.getMinTime());
		assertTrue(HIGH.getMaxTime() < NORMAL.getMinTime());
		assertTrue(NORMAL.getMaxTime() < LOW.getMinTime());
		assertEquals(0, OFF.getMinTime());
		assertEquals(0, OFF.getMaxTime());
	}

	@Test
	void RsServiceInitPriority_MinMax_OK()
	{
		assertTrue(IMMEDIATE.getMinTime() <= IMMEDIATE.getMaxTime());
		assertTrue(HIGH.getMinTime() <= HIGH.getMaxTime());
		assertTrue(NORMAL.getMinTime() <= NORMAL.getMaxTime());
		assertTrue(LOW.getMinTime() <= LOW.getMaxTime());
	}
}
