/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.gxs;

import io.xeres.app.xrs.service.gxs.item.GxsSyncGroupItem;
import io.xeres.app.xrs.service.gxs.item.TransactionFlags;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest
{
	@Test
	void AddItems_Success()
	{
		var transaction = new Transaction<GxsSyncGroupItem>(1, EnumSet.noneOf(TransactionFlags.class), new ArrayList<>(), 2, null, Transaction.Direction.INCOMING);

		transaction.addItem(new GxsSyncGroupItem());
		transaction.addItem(new GxsSyncGroupItem());

		assertEquals(1, transaction.getId());
		assertFalse(transaction.hasTimeout());
		assertTrue(transaction.hasAllItems());
	}
}
