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

import io.xeres.app.database.model.reputation.ReputationIdentityFakes;
import io.xeres.common.reputation.Opinion;
import io.xeres.testutils.IdFakes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ReputationIdentityRepositoryTest
{
	@Autowired
	private ReputationIdentityRepository reputationIdentityRepository;

	@Test
	void CRUD_Success()
	{
		var gxsId1 = IdFakes.createGxsId();
		var gxsId2 = IdFakes.createGxsId();
		var gxsId3 = IdFakes.createGxsId();

		var identity1 = ReputationIdentityFakes.createFreshReputationIdentity(gxsId1, Opinion.POSITIVE);
		var identity2 = ReputationIdentityFakes.createFreshReputationIdentity(gxsId2, Opinion.NEUTRAL);
		var identity3 = ReputationIdentityFakes.createFreshReputationIdentity(gxsId3, Opinion.NEGATIVE);

		var savedIdentity = reputationIdentityRepository.save(identity1);
		reputationIdentityRepository.save(identity2);
		reputationIdentityRepository.save(identity3);

		var identities = reputationIdentityRepository.findAll();
		assertNotNull(identities);
		assertEquals(3, identities.size());

		var found = reputationIdentityRepository.findByGxsId(gxsId1).orElse(null);

		assertNotNull(found);
		assertEquals(gxsId1, found.getGxsId());
		assertEquals(savedIdentity.getId(), found.getId());
		assertEquals(Opinion.POSITIVE, found.getOpinion());

		found.setOpinion(Opinion.NEGATIVE);

		var updatedIdentity = reputationIdentityRepository.save(found);

		assertNotNull(updatedIdentity);
		assertEquals(gxsId1, updatedIdentity.getGxsId());
		assertEquals(found.getId(), updatedIdentity.getId());
		assertEquals(Opinion.NEGATIVE, updatedIdentity.getOpinion());

		var foundForDelete = reputationIdentityRepository.findByGxsId(gxsId1).orElse(null);
		assertNotNull(foundForDelete);
		reputationIdentityRepository.delete(foundForDelete);

		var deleted = reputationIdentityRepository.findByGxsId(gxsId1);
		assertTrue(deleted.isEmpty());
	}

	@Test
	void findByGxsId_Success()
	{
		var gxsId = IdFakes.createGxsId();
		var identity = ReputationIdentityFakes.createFreshReputationIdentity(gxsId, Opinion.POSITIVE);

		reputationIdentityRepository.save(identity);

		var found = reputationIdentityRepository.findByGxsId(gxsId);

		assertTrue(found.isPresent());
		assertEquals(gxsId, found.get().getGxsId());
		assertEquals(Opinion.POSITIVE, found.get().getOpinion());
	}

	@Test
	void findByGxsId_NotFound()
	{
		var gxsId = IdFakes.createGxsId();

		var found = reputationIdentityRepository.findByGxsId(gxsId);

		assertTrue(found.isEmpty());
	}

	@Test
	void findAllByOpinionUpdatedAfter_Success()
	{
		var beforeTime = Instant.now().minusSeconds(60);
		var afterTime = Instant.now().plusSeconds(60);

		var identity1 = ReputationIdentityFakes.createFreshReputationIdentity(IdFakes.createGxsId(), Opinion.POSITIVE);
		var identity2 = ReputationIdentityFakes.createFreshReputationIdentity(IdFakes.createGxsId(), Opinion.NEUTRAL);

		reputationIdentityRepository.save(identity1);
		reputationIdentityRepository.save(identity2);

		var found = reputationIdentityRepository.findAllByOpinionUpdatedAfter(beforeTime);

		assertNotNull(found);
		assertEquals(2, found.size());

		var notFound = reputationIdentityRepository.findAllByOpinionUpdatedAfter(afterTime);
		assertTrue(notFound.isEmpty());
	}
}
