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

package io.xeres.app.database.repository;

import io.xeres.app.database.model.gxs.GxsIdGroupItemFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GxsIdRepositoryTest
{
	@Autowired
	private GxsIdRepository gxsIdRepository;

	@Test
	void GxsIdRepository_CRUD_OK()
	{
		var gxsIdGroupItem1 = GxsIdGroupItemFakes.createGxsIdGroupItem();
		var gxsIdGroupItem2 = GxsIdGroupItemFakes.createGxsIdGroupItem();
		var gxsIdGroupItem3 = GxsIdGroupItemFakes.createGxsIdGroupItem();

		var savedGxsIdGroupItem1 = gxsIdRepository.save(gxsIdGroupItem1);
		var savedGxsIdGroupItem2 = gxsIdRepository.save(gxsIdGroupItem2);
		gxsIdRepository.save(gxsIdGroupItem3);

		var gxsIdGroupItems = gxsIdRepository.findAll();
		assertNotNull(gxsIdGroupItems);
		assertEquals(3, gxsIdGroupItems.size());

		var first = gxsIdRepository.findById(gxsIdGroupItems.get(0).getId()).orElse(null);
		assertNotNull(first);
		assertEquals(savedGxsIdGroupItem1.getId(), first.getId());
		assertEquals(savedGxsIdGroupItem1.getName(), first.getName());

		var second = gxsIdRepository.findByGxsId(gxsIdGroupItem2.getGxsId()).orElse(null);
		assertNotNull(second);
		assertEquals(savedGxsIdGroupItem2.getId(), second.getId());
		assertEquals(savedGxsIdGroupItem2.getName(), second.getName());

		first.setStatus(1);

		var updateGxsIdGroupItem = gxsIdRepository.save(first);

		assertNotNull(updateGxsIdGroupItem);
		assertEquals(first.getId(), updateGxsIdGroupItem.getId());
		assertEquals(1, updateGxsIdGroupItem.getStatus());

		gxsIdRepository.deleteById(first.getId());

		var deleted = gxsIdRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}
}
