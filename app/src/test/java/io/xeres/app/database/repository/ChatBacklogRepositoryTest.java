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

import io.xeres.app.database.model.chat.ChatBacklog;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.testutils.IdFakes;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Limit;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ChatBacklogRepositoryTest
{
	@Autowired
	private ProfileRepository profileRepository;

	@Autowired
	private LocationRepository locationRepository;

	@Autowired
	private ChatBacklogRepository chatBacklogRepository;

	@Autowired
	private EntityManager entityManager;

	private Location saveLocation(String name)
	{
		var profile = profileRepository.save(ProfileFakes.createFreshProfile(name, IdFakes.createLong()));
		return locationRepository.save(LocationFakes.createFreshLocation(name, profile));
	}

	private void setCreated(String message, Instant created)
	{
		// Bulk update because @CreationTimestamp overrides the value on persist
		entityManager.createQuery("UPDATE ChatBacklog b SET b.created = :created WHERE b.message = :message")
				.setParameter("created", created)
				.setParameter("message", message)
				.executeUpdate();
		entityManager.clear();
	}

	@Test
	void CRUD_Success()
	{
		var location = saveLocation("test1");

		var backlog1 = new ChatBacklog(location, true, "hello");
		var backlog2 = new ChatBacklog(location, false, "hi");

		chatBacklogRepository.save(backlog1);
		chatBacklogRepository.save(backlog2);

		entityManager.flush();
		entityManager.clear();

		var backlogs = chatBacklogRepository.findAll();
		assertNotNull(backlogs);
		assertEquals(2, backlogs.size());

		var first = chatBacklogRepository.findById(backlogs.getFirst().getId()).orElse(null);

		assertNotNull(first);
		assertTrue(first.isOwn());
		assertEquals("hello", first.getMessage());

		first.setMessage("updated");

		var updatedBacklog = chatBacklogRepository.save(first);

		assertNotNull(updatedBacklog);
		assertEquals(first.getId(), updatedBacklog.getId());
		assertEquals("updated", updatedBacklog.getMessage());

		chatBacklogRepository.deleteById(first.getId());

		var deleted = chatBacklogRepository.findById(first.getId());
		assertTrue(deleted.isEmpty());
	}

	@Test
	void FindAllByLocationAndCreatedAfterOrderByCreatedDesc_Success()
	{
		var location1 = saveLocation("test1");
		var location2 = saveLocation("test2");

		var now = Instant.now();

		chatBacklogRepository.save(new ChatBacklog(location1, true, "old message"));
		chatBacklogRepository.save(new ChatBacklog(location1, false, "recent message"));
		chatBacklogRepository.save(new ChatBacklog(location2, false, "other location message"));

		setCreated("old message", now.minusSeconds(3600));
		setCreated("recent message", now.minusSeconds(60));

		// Only recent messages of location1 are returned, newest first
		var found = chatBacklogRepository.findAllByLocationAndCreatedAfterOrderByCreatedDesc(location1, now.minusSeconds(120), Limit.unlimited());

		assertNotNull(found);
		assertEquals(1, found.size());
		assertEquals("recent message", found.getFirst().getMessage());

		// All messages of location1 are returned when the range covers everything
		var all = chatBacklogRepository.findAllByLocationAndCreatedAfterOrderByCreatedDesc(location1, Instant.EPOCH, Limit.unlimited());

		assertNotNull(all);
		assertEquals(2, all.size());
		assertEquals("recent message", all.getFirst().getMessage()); // newest first
		assertEquals("old message", all.getLast().getMessage());

		// Messages from other locations are not included
		var otherLocation = chatBacklogRepository.findAllByLocationAndCreatedAfterOrderByCreatedDesc(location2, Instant.EPOCH, Limit.unlimited());

		assertNotNull(otherLocation);
		assertEquals(1, otherLocation.size());
		assertEquals("other location message", otherLocation.getFirst().getMessage());
	}

	@Test
	void FindAllByLocationAndCreatedAfterOrderByCreatedDesc_WithLimit_Success()
	{
		var location = saveLocation("test1");

		var now = Instant.now();

		chatBacklogRepository.save(new ChatBacklog(location, true, "first"));
		chatBacklogRepository.save(new ChatBacklog(location, false, "second"));

		setCreated("first", now.minusSeconds(3600));
		setCreated("second", now.minusSeconds(60));

		var limited = chatBacklogRepository.findAllByLocationAndCreatedAfterOrderByCreatedDesc(location, Instant.EPOCH, Limit.of(1));

		assertNotNull(limited);
		assertEquals(1, limited.size());
		assertEquals("second", limited.getFirst().getMessage()); // newest first
	}

	@Test
	void DeleteAllByCreatedBefore_Success()
	{
		var location = saveLocation("test1");

		var now = Instant.now();

		chatBacklogRepository.save(new ChatBacklog(location, true, "old"));
		chatBacklogRepository.save(new ChatBacklog(location, false, "recent"));

		setCreated("old", now.minusSeconds(3600));
		setCreated("recent", now.minusSeconds(60));

		chatBacklogRepository.deleteAllByCreatedBefore(now.minusSeconds(120));

		var remaining = chatBacklogRepository.findAll();

		assertEquals(1, remaining.size());
		assertEquals("recent", remaining.getFirst().getMessage());
	}

	@Test
	void DeleteAllByLocation_Success()
	{
		var location1 = saveLocation("test1");
		var location2 = saveLocation("test2");

		chatBacklogRepository.save(new ChatBacklog(location1, true, "hello"));
		chatBacklogRepository.save(new ChatBacklog(location2, false, "hi"));

		chatBacklogRepository.deleteAllByLocation(location1);

		var remaining = chatBacklogRepository.findAll();

		assertEquals(1, remaining.size());
		assertEquals("hi", remaining.getFirst().getMessage());
	}
}
