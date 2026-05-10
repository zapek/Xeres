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

package io.xeres.app.xrs.service.gxs.item;

import io.xeres.common.id.GxsId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GxsSyncMessageRequestItemTest
{
	@Test
	void testGxsSyncMessageRequestItem()
	{
		var gxsId = GxsId.fromString("11111111111111111111111111111111");
		var now = Instant.now();
		var lastUpdated = now.minus(Duration.ofDays(30));
		var syncLimit = Duration.ofDays(365);

		var request = new GxsSyncMessageRequestItem(gxsId, lastUpdated, syncLimit);

		assertEquals(lastUpdated.getEpochSecond(), request.getLastUpdated());
		assertTrue(Math.abs(now.minus(syncLimit).getEpochSecond() - request.getCreateSince()) <= 1); // GxsSyncMessageRequestItem uses Instant.now() internally so we have to give some slack
	}
}