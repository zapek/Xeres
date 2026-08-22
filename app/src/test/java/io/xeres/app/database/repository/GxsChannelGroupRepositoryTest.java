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

package io.xeres.app.database.repository;

import io.xeres.app.database.model.gxs.ChannelGroupItemFakes;
import io.xeres.testutils.IdFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class GxsChannelGroupRepositoryTest
{
	@Autowired
	private GxsChannelGroupRepository gxsChannelGroupRepository;

	@Test
	void CRUD_Success()
	{
		var channelGroupItem1 = ChannelGroupItemFakes.createChannelGroupItem();
		var channelGroupItem2 = ChannelGroupItemFakes.createChannelGroupItem();
		var channelGroupItem3 = ChannelGroupItemFakes.createChannelGroupItem();

		var savedChannelGroupItem1 = gxsChannelGroupRepository.save(channelGroupItem1);
		gxsChannelGroupRepository.save(channelGroupItem2);
		gxsChannelGroupRepository.save(channelGroupItem3);

		var channelGroupItems = gxsChannelGroupRepository.findAll();
		assertNotNull(channelGroupItems);
		assertEquals(3, channelGroupItems.size());

		var first = gxsChannelGroupRepository.findById(channelGroupItems.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertEquals(savedChannelGroupItem1.getId(), first.getId());
		assertEquals(savedChannelGroupItem1.getName(), first.getName());

		first.setDescription("updated");

		var updatedChannelGroupItem = gxsChannelGroupRepository.save(first);

		assertNotNull(updatedChannelGroupItem);
		assertEquals(first.getId(), updatedChannelGroupItem.getId());
		assertEquals("updated", updatedChannelGroupItem.getDescription());

		gxsChannelGroupRepository.deleteById(first.getId());

		var deleted = gxsChannelGroupRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindByGxsId_Success()
	{
		var channelGroupItem = ChannelGroupItemFakes.createChannelGroupItem();
		gxsChannelGroupRepository.save(channelGroupItem);

		var found = gxsChannelGroupRepository.findByGxsId(channelGroupItem.getGxsId()).orElse(null);

		assertNotNull(found);
		assertEquals(channelGroupItem.getGxsId(), found.getGxsId());
		assertEquals(channelGroupItem.getName(), found.getName());

		assertTrue(gxsChannelGroupRepository.findByGxsId(IdFakes.createGxsId()).isEmpty());
	}

	@Test
	void FindAllByGxsIdIn_Success()
	{
		var channelGroupItem1 = ChannelGroupItemFakes.createChannelGroupItem();
		var channelGroupItem2 = ChannelGroupItemFakes.createChannelGroupItem();
		var channelGroupItem3 = ChannelGroupItemFakes.createChannelGroupItem();

		gxsChannelGroupRepository.save(channelGroupItem1);
		gxsChannelGroupRepository.save(channelGroupItem2);
		gxsChannelGroupRepository.save(channelGroupItem3);

		var found = gxsChannelGroupRepository.findAllByGxsIdIn(Set.of(channelGroupItem1.getGxsId(), channelGroupItem3.getGxsId()));

		assertNotNull(found);
		assertEquals(2, found.size());
		assertTrue(found.contains(channelGroupItem1));
		assertTrue(found.contains(channelGroupItem3));
	}

	@Test
	void FindAllBySubscribedIsTrue_Success()
	{
		var subscribedChannelGroupItem1 = ChannelGroupItemFakes.createChannelGroupItem();
		subscribedChannelGroupItem1.setSubscribed(true);
		var subscribedChannelGroupItem2 = ChannelGroupItemFakes.createChannelGroupItem();
		subscribedChannelGroupItem2.setSubscribed(true);
		var unsubscribedChannelGroupItem = ChannelGroupItemFakes.createChannelGroupItem();

		gxsChannelGroupRepository.save(subscribedChannelGroupItem1);
		gxsChannelGroupRepository.save(subscribedChannelGroupItem2);
		gxsChannelGroupRepository.save(unsubscribedChannelGroupItem);

		var found = gxsChannelGroupRepository.findAllBySubscribedIsTrue();

		assertNotNull(found);
		assertEquals(2, found.size());
	}
}
