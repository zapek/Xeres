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

class ReputationTest
{
	@Test
	void Reputation_Order()
	{
		assertEquals(0, Reputation.LOCALLY_NEGATIVE.ordinal());
		assertEquals(1, Reputation.REMOTELY_NEGATIVE.ordinal());
		assertEquals(2, Reputation.NEUTRAL.ordinal());
		assertEquals(3, Reputation.REMOTELY_POSITIVE.ordinal());
		assertEquals(4, Reputation.LOCALLY_POSITIVE.ordinal());
	}
}