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

package io.xeres.app.service;

import io.xeres.app.database.model.identity.IdentityFakes;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.app.database.model.reputation.ReputationBannedProfile;
import io.xeres.app.database.model.reputation.ReputationIdentity;
import io.xeres.app.database.model.reputation.ReputationIdentityFakes;
import io.xeres.app.database.model.reputation.ReputationUpdate;
import io.xeres.app.database.repository.GxsIdentityRepository;
import io.xeres.app.database.repository.ReputationBannedProfileRepository;
import io.xeres.app.database.repository.ReputationIdentityRepository;
import io.xeres.app.database.repository.ReputationUpdateRepository;
import io.xeres.common.reputation.Opinion;
import io.xeres.common.reputation.Reputation;
import io.xeres.testutils.IdFakes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReputationServiceTest
{
	@Mock
	private ReputationIdentityRepository reputationIdentityRepository;

	@Mock
	private ReputationUpdateRepository reputationUpdateRepository;

	@Mock
	private GxsIdentityRepository gxsIdentityRepository;

	@Mock
	private ReputationBannedProfileRepository reputationBannedProfileRepository;

	@InjectMocks
	private ReputationService reputationService;

	@Test
	void getReputation_WithKnownIdentity_ShouldReturnStoredReputation()
	{
		var gxsId = IdFakes.createGxsId();
		var reputationIdentity = ReputationIdentityFakes.createReputationIdentity(gxsId, Opinion.POSITIVE);
		reputationIdentity.setReputation(Reputation.LOCALLY_POSITIVE);
		var lastUsedBefore = Instant.now();

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.of(reputationIdentity));

		var result = reputationService.getReputation(gxsId);

		assertEquals(Reputation.LOCALLY_POSITIVE, result);
		assertFalse(reputationIdentity.getLastUsed().isBefore(lastUsedBefore));
	}

	@Test
	void getReputation_WithUnknownIdentity_ShouldReturnNeutral()
	{
		var gxsId = IdFakes.createGxsId();

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.empty());

		var result = reputationService.getReputation(gxsId);

		assertEquals(Reputation.NEUTRAL, result);
	}

	@Test
	void getAllReputations_ShouldReturnAll()
	{
		var reputations = List.of(ReputationIdentityFakes.createReputationIdentity(), ReputationIdentityFakes.createReputationIdentity());

		when(reputationIdentityRepository.findAll()).thenReturn(reputations);

		var result = reputationService.getAllReputations();

		assertEquals(2, result.size());
	}

	@Test
	void findByGxsId_ShouldReturnEntry()
	{
		var gxsId = IdFakes.createGxsId();
		var reputationIdentity = ReputationIdentityFakes.createReputationIdentity(gxsId, Opinion.POSITIVE);

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.of(reputationIdentity));

		var result = reputationService.findByGxsId(gxsId);

		assertTrue(result.isPresent());
		assertSame(reputationIdentity, result.get());
	}

	@Test
	void findUpdatedIdentities_ShouldReturnUpdatedOnes()
	{
		var since = Instant.now().minusSeconds(60);
		var reputations = List.of(ReputationIdentityFakes.createReputationIdentity());

		when(reputationIdentityRepository.findAllByOpinionUpdatedAfter(since)).thenReturn(reputations);

		var result = reputationService.findUpdatedIdentities(since);

		assertEquals(1, result.size());
	}

	@Test
	void storeReputationUpdate_WithNewLocation_ShouldCreate()
	{
		var location = LocationFakes.createLocation();
		var when = Instant.now();

		when(reputationUpdateRepository.findByLocation(location)).thenReturn(Optional.empty());

		reputationService.storeReputationUpdate(location, when);

		var captor = ArgumentCaptor.forClass(ReputationUpdate.class);
		verify(reputationUpdateRepository).save(captor.capture());
		assertSame(location, captor.getValue().getLocation());
		assertEquals(when, captor.getValue().getLastUpdated());
	}

	@Test
	void storeReputationUpdate_WithExistingLocation_ShouldUpdate()
	{
		var location = LocationFakes.createLocation();
		var before = Instant.now().minusSeconds(60);
		var when = Instant.now();
		var reputationUpdate = new ReputationUpdate(location, before);

		when(reputationUpdateRepository.findByLocation(location)).thenReturn(Optional.of(reputationUpdate));

		reputationService.storeReputationUpdate(location, when);

		assertEquals(when, reputationUpdate.getLastUpdated());
		verify(reputationUpdateRepository).save(same(reputationUpdate));
	}

	@Test
	void getReputationUpdate_WithKnownLocation_ShouldReturnValue()
	{
		var location = LocationFakes.createLocation();
		var when = Instant.now();

		when(reputationUpdateRepository.findByLocation(location)).thenReturn(Optional.of(new ReputationUpdate(location, when)));

		var result = reputationService.getReputationUpdate(location);

		assertEquals(when, result);
	}

	@Test
	void getReputationUpdate_WithUnknownLocation_ShouldReturnEpoch()
	{
		var location = LocationFakes.createLocation();

		when(reputationUpdateRepository.findByLocation(location)).thenReturn(Optional.empty());

		var result = reputationService.getReputationUpdate(location);

		assertEquals(Instant.EPOCH, result);
	}

	@Test
	void updateIdentityReputation_WithNullArguments_ShouldThrow()
	{
		var gxsId = IdFakes.createGxsId();

		assertThrows(NullPointerException.class, () -> reputationService.updateIdentityReputation(null, Opinion.POSITIVE));
		assertThrows(NullPointerException.class, () -> reputationService.updateIdentityReputation(gxsId, null));
		assertThrows(NullPointerException.class, () -> reputationService.updateIdentityReputation(null, null, Opinion.POSITIVE));
	}

	@Test
	void updateIdentityReputation_WithSameOpinion_ShouldDoNothing()
	{
		var gxsId = IdFakes.createGxsId();
		var reputationIdentity = ReputationIdentityFakes.createReputationIdentity(gxsId, Opinion.POSITIVE);

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.of(reputationIdentity));

		reputationService.updateIdentityReputation(gxsId, Opinion.POSITIVE);

		assertEquals(Opinion.POSITIVE, reputationIdentity.getOpinion());
		verify(reputationIdentityRepository, never()).delete(any());
	}

	@Test
	void updateIdentityReputation_WithChangedOpinion_ShouldRecalculate()
	{
		var gxsId = IdFakes.createGxsId();
		var reputationIdentity = ReputationIdentityFakes.createReputationIdentity(gxsId, Opinion.POSITIVE);

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.of(reputationIdentity));

		reputationService.updateIdentityReputation(gxsId, Opinion.NEGATIVE);

		assertEquals(Opinion.NEGATIVE, reputationIdentity.getOpinion());
		assertEquals(Reputation.LOCALLY_NEGATIVE, reputationIdentity.getReputation());
		verify(reputationIdentityRepository, never()).delete(any());
	}

	@Test
	void updateIdentityReputation_WithNeutralWithoutPeerOpinions_ShouldDelete()
	{
		var gxsId = IdFakes.createGxsId();
		var reputationIdentity = ReputationIdentityFakes.createReputationIdentity(gxsId, Opinion.NEGATIVE);

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.of(reputationIdentity));

		reputationService.updateIdentityReputation(gxsId, Opinion.NEUTRAL);

		verify(reputationIdentityRepository).delete(same(reputationIdentity));
	}

	@Test
	void updateIdentityReputation_WithNeutralWithPeerOpinions_ShouldKeepAndRecalculate()
	{
		var gxsId = IdFakes.createGxsId();
		var peerLocationIdentifier = LocationFakes.createLocation().getLocationIdentifier();
		var reputationIdentity = ReputationIdentityFakes.createReputationIdentity(gxsId, Opinion.POSITIVE);
		reputationIdentity.addOpinion(peerLocationIdentifier, Opinion.NEGATIVE);

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.of(reputationIdentity));

		reputationService.updateIdentityReputation(gxsId, Opinion.NEUTRAL);

		assertEquals(Opinion.NEUTRAL, reputationIdentity.getOpinion());
		assertEquals(Reputation.REMOTELY_NEGATIVE, reputationIdentity.getReputation());
		verify(reputationIdentityRepository, never()).delete(any());
	}

	@Test
	void updateIdentityReputation_WithNeutralOnUnknownIdentity_ShouldDoNothing()
	{
		var gxsId = IdFakes.createGxsId();

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.empty());

		reputationService.updateIdentityReputation(gxsId, Opinion.NEUTRAL);

		verify(reputationIdentityRepository, never()).save(any(ReputationIdentity.class));
	}

	@Test
	void updateIdentityReputation_WithPositiveOnUnknownIdentity_ShouldDoNothing()
	{
		var gxsId = IdFakes.createGxsId();

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.empty());
		when(gxsIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.empty());

		reputationService.updateIdentityReputation(gxsId, Opinion.POSITIVE);

		verify(reputationIdentityRepository, never()).save(any(ReputationIdentity.class));
	}

	@Test
	void updateIdentityReputation_WithNegativeOnUnknownIdentity_ShouldCreateLocallyNegative()
	{
		var gxsId = IdFakes.createGxsId();

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.empty());
		when(gxsIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.empty());
		when(reputationIdentityRepository.save(any(ReputationIdentity.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

		reputationService.updateIdentityReputation(gxsId, Opinion.NEGATIVE);

		var captor = ArgumentCaptor.forClass(ReputationIdentity.class);
		verify(reputationIdentityRepository).save(captor.capture());
		assertEquals(gxsId, captor.getValue().getGxsId());
		assertNull(captor.getValue().getIdentity());
		assertEquals(Opinion.NEGATIVE, captor.getValue().getOpinion());
		assertEquals(Reputation.LOCALLY_NEGATIVE, captor.getValue().getReputation());
	}

	@Test
	void updateIdentityReputation_WithPositiveOnKnownIdentity_ShouldCreateLocallyPositive()
	{
		var gxsId = IdFakes.createGxsId();
		var identity = IdentityFakes.createOwn();

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.empty());
		when(gxsIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.of(identity));
		when(reputationIdentityRepository.save(any(ReputationIdentity.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

		reputationService.updateIdentityReputation(gxsId, Opinion.POSITIVE);

		var captor = ArgumentCaptor.forClass(ReputationIdentity.class);
		verify(reputationIdentityRepository).save(captor.capture());
		assertSame(identity, captor.getValue().getIdentity());
		assertEquals(Opinion.POSITIVE, captor.getValue().getOpinion());
		assertEquals(Reputation.LOCALLY_POSITIVE, captor.getValue().getReputation());
	}

	@Test
	void updateIdentityReputation_WithBannedProfile_ShouldBeLocallyNegative()
	{
		var identity = IdentityFakes.createOwn();
		identity.setProfile(ProfileFakes.createProfile());
		var gxsId = identity.getGxsId();
		var pgpIdentifier = identity.getProfile().getPgpIdentifier();
		var peerLocationIdentifier = LocationFakes.createLocation().getLocationIdentifier();
		var reputationIdentity = new ReputationIdentity(gxsId, identity, Opinion.POSITIVE);
		reputationIdentity.addOpinion(peerLocationIdentifier, Opinion.NEGATIVE);

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.of(reputationIdentity));
		when(reputationBannedProfileRepository.findByPgpIdentifier(pgpIdentifier)).thenReturn(Optional.of(new ReputationBannedProfile(pgpIdentifier)));

		reputationService.updateIdentityReputation(gxsId, Opinion.NEUTRAL);

		assertEquals(Opinion.NEUTRAL, reputationIdentity.getOpinion());
		assertEquals(Reputation.LOCALLY_NEGATIVE, reputationIdentity.getReputation());
		verify(reputationIdentityRepository, never()).delete(any());
	}

	@Test
	void updateIdentityReputation_FromPeerPositiveOnUnknownIdentity_ShouldCreateRemotelyPositive()
	{
		var location = LocationFakes.createLocation();
		var gxsId = IdFakes.createGxsId();

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.empty());
		when(gxsIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.empty());
		when(reputationIdentityRepository.save(any(ReputationIdentity.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

		reputationService.updateIdentityReputation(location, gxsId, Opinion.POSITIVE);

		var captor = ArgumentCaptor.forClass(ReputationIdentity.class);
		verify(reputationIdentityRepository).save(captor.capture());
		assertEquals(gxsId, captor.getValue().getGxsId());
		assertTrue(captor.getValue().hasPeerOpinions());
		assertEquals(1, captor.getValue().getPositiveVotes());
		assertEquals(Reputation.REMOTELY_POSITIVE, captor.getValue().getReputation());
	}

	@Test
	void updateIdentityReputation_FromPeerNegativeOnUnknownIdentity_ShouldCreateRemotelyNegative()
	{
		var location = LocationFakes.createLocation();
		var gxsId = IdFakes.createGxsId();

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.empty());
		when(gxsIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.empty());
		when(reputationIdentityRepository.save(any(ReputationIdentity.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

		reputationService.updateIdentityReputation(location, gxsId, Opinion.NEGATIVE);

		var captor = ArgumentCaptor.forClass(ReputationIdentity.class);
		verify(reputationIdentityRepository).save(captor.capture());
		assertEquals(gxsId, captor.getValue().getGxsId());
		assertEquals(1, captor.getValue().getNegativeVotes());
		assertEquals(Reputation.REMOTELY_NEGATIVE, captor.getValue().getReputation());
	}

	@Test
	void updateIdentityReputation_FromPeerNeutralOnUnknownIdentity_ShouldDoNothing()
	{
		var location = LocationFakes.createLocation();
		var gxsId = IdFakes.createGxsId();

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.empty());

		reputationService.updateIdentityReputation(location, gxsId, Opinion.NEUTRAL);

		verify(reputationIdentityRepository, never()).save(any(ReputationIdentity.class));
	}

	@Test
	void updateIdentityReputation_FromPeerWithSameOpinion_ShouldDoNothing()
	{
		var location = LocationFakes.createLocation();
		var gxsId = IdFakes.createGxsId();
		var reputationIdentity = ReputationIdentityFakes.createReputationIdentity(gxsId, Opinion.NEUTRAL);
		reputationIdentity.addOpinion(location.getLocationIdentifier(), Opinion.POSITIVE);

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.of(reputationIdentity));

		reputationService.updateIdentityReputation(location, gxsId, Opinion.POSITIVE);

		assertEquals(Reputation.NEUTRAL, reputationIdentity.getReputation());
		verify(reputationIdentityRepository, never()).delete(any());
		verify(reputationIdentityRepository, never()).save(any(ReputationIdentity.class));
	}

	@Test
	void updateIdentityReputation_FromPeerRemovingLastOpinion_ShouldDelete()
	{
		var location = LocationFakes.createLocation();
		var gxsId = IdFakes.createGxsId();
		var reputationIdentity = ReputationIdentityFakes.createReputationIdentity(gxsId, Opinion.NEUTRAL);
		reputationIdentity.addOpinion(location.getLocationIdentifier(), Opinion.POSITIVE);

		when(reputationIdentityRepository.findByGxsId(gxsId)).thenReturn(Optional.of(reputationIdentity));

		reputationService.updateIdentityReputation(location, gxsId, Opinion.NEUTRAL);

		assertFalse(reputationIdentity.hasPeerOpinions());
		verify(reputationIdentityRepository).delete(same(reputationIdentity));
	}

	@Test
	void banProfile_WithNewProfile_ShouldSave()
	{
		long pgpIdentifier = 1234L;

		when(reputationBannedProfileRepository.findByPgpIdentifier(pgpIdentifier)).thenReturn(Optional.empty());

		reputationService.banProfile(pgpIdentifier);

		var captor = ArgumentCaptor.forClass(ReputationBannedProfile.class);
		verify(reputationBannedProfileRepository).save(captor.capture());
		assertEquals(pgpIdentifier, captor.getValue().getPgpIdentifier());
	}

	@Test
	void banProfile_WithAlreadyBannedProfile_ShouldNotDuplicate()
	{
		long pgpIdentifier = 1234L;
		var bannedProfile = new ReputationBannedProfile(pgpIdentifier);
		var lastUsedBefore = Instant.now();

		when(reputationBannedProfileRepository.findByPgpIdentifier(pgpIdentifier)).thenReturn(Optional.of(bannedProfile));

		reputationService.banProfile(pgpIdentifier);

		verify(reputationBannedProfileRepository, never()).save(any(ReputationBannedProfile.class));
		assertFalse(bannedProfile.getLastUsed().isBefore(lastUsedBefore));
	}

	@Test
	void unBanProfile_WithBannedProfile_ShouldDelete()
	{
		long pgpIdentifier = 1234L;
		var bannedProfile = new ReputationBannedProfile(pgpIdentifier);

		when(reputationBannedProfileRepository.findByPgpIdentifier(pgpIdentifier)).thenReturn(Optional.of(bannedProfile));

		reputationService.unBanProfile(pgpIdentifier);

		verify(reputationBannedProfileRepository).delete(same(bannedProfile));
	}

	@Test
	void unBanProfile_WithUnknownProfile_ShouldDoNothing()
	{
		long pgpIdentifier = 1234L;

		when(reputationBannedProfileRepository.findByPgpIdentifier(pgpIdentifier)).thenReturn(Optional.empty());

		reputationService.unBanProfile(pgpIdentifier);

		verify(reputationBannedProfileRepository, never()).delete(any());
	}
}
