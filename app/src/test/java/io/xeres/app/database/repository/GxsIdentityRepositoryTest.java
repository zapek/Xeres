/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

import io.xeres.app.database.model.gxs.IdentityGroupItemFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GxsIdentityRepositoryTest
{
	@Autowired
	private GxsIdentityRepository gxsIdentityRepository;

	@Test
	void CRUD_Success()
	{
		var gxsIdGroupItem1 = IdentityGroupItemFakes.createIdentityGroupItem();
		var gxsIdGroupItem2 = IdentityGroupItemFakes.createIdentityGroupItem();
		var gxsIdGroupItem3 = IdentityGroupItemFakes.createIdentityGroupItem();

		var savedGxsIdGroupItem1 = gxsIdentityRepository.save(gxsIdGroupItem1);
		var savedGxsIdGroupItem2 = gxsIdentityRepository.save(gxsIdGroupItem2);
		gxsIdentityRepository.save(gxsIdGroupItem3);

		var gxsIdGroupItems = gxsIdentityRepository.findAll();
		assertNotNull(gxsIdGroupItems);
		assertEquals(3, gxsIdGroupItems.size());

		var first = gxsIdentityRepository.findById(gxsIdGroupItems.getFirst().getId()).orElse(null);
		assertNotNull(first);
		assertEquals(savedGxsIdGroupItem1.getId(), first.getId());
		assertEquals(savedGxsIdGroupItem1.getName(), first.getName());

		var second = gxsIdentityRepository.findByGxsId(gxsIdGroupItem2.getGxsId()).orElse(null);
		assertNotNull(second);
		assertEquals(savedGxsIdGroupItem2.getId(), second.getId());
		assertEquals(savedGxsIdGroupItem2.getName(), second.getName());

		first.setIdentityScore(10);

		var updatedGxsIdGroupItem = gxsIdentityRepository.save(first);

		assertNotNull(updatedGxsIdGroupItem);
		assertEquals(first.getId(), updatedGxsIdGroupItem.getId());
		assertEquals(10, updatedGxsIdGroupItem.getIdentityScore());

		gxsIdentityRepository.deleteById(first.getId());

		var deleted = gxsIdentityRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}
}
