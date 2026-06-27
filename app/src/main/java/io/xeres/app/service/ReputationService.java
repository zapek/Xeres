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

import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.model.reputation.*;
import io.xeres.app.database.repository.GxsIdentityRepository;
import io.xeres.app.database.repository.ReputationBannedProfileRepository;
import io.xeres.app.database.repository.ReputationIdentityRepository;
import io.xeres.app.database.repository.ReputationUpdateRepository;
import io.xeres.common.id.GxsId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static io.xeres.app.database.model.reputation.ReputationUpdate.DEFAULT_REPUTATION_UPDATE;

@Service
public class ReputationService
{
	private static final Logger log = LoggerFactory.getLogger(ReputationService.class);

	private final ReputationIdentityRepository reputationIdentityRepository;
	private final ReputationUpdateRepository reputationUpdateRepository;
	private final GxsIdentityRepository gxsIdentityRepository; // We use the repository instead of the service to avoid updating identity accesses
	private final ReputationBannedProfileRepository reputationBannedProfileRepository;

	public ReputationService(ReputationIdentityRepository reputationIdentityRepository, ReputationUpdateRepository reputationUpdateRepository, GxsIdentityRepository gxsIdentityRepository, ReputationBannedProfileRepository reputationBannedProfileRepository)
	{
		this.reputationIdentityRepository = reputationIdentityRepository;
		this.reputationUpdateRepository = reputationUpdateRepository;
		this.gxsIdentityRepository = gxsIdentityRepository;
		this.reputationBannedProfileRepository = reputationBannedProfileRepository;
	}

	@Transactional
	public Reputation getReputation(GxsId gxsId)
	{
		var reputation = reputationIdentityRepository.findByGxsId(gxsId);
		reputation.ifPresent(ReputationIdentity::updateLastUsed);
		return reputation.orElse(ReputationIdentity.DEFAULT_REPUTATION).getReputation();
	}

	public List<ReputationIdentity> findUpdatedIdentities(Instant since)
	{
		return reputationIdentityRepository.findAllByOpinionUpdatedAfter(since);
	}

	public void storeReputationUpdate(Location peer, Instant when)
	{
		var reputationUpdate = new ReputationUpdate(peer, when);
		reputationUpdateRepository.save(reputationUpdate);
	}

	public Instant getReputationUpdate(Location peer)
	{
		return reputationUpdateRepository.findByLocation(peer).orElse(DEFAULT_REPUTATION_UPDATE).getLastUpdated();
	}

	/**
	 * Sets an identity reputation by the local node.
	 *
	 * @param gxsId   the gxsId of the identity to change
	 * @param opinion the opinion to set
	 */
	@Transactional
	public void updateIdentityReputation(GxsId gxsId, Opinion opinion)
	{
		Objects.requireNonNull(gxsId);
		Objects.requireNonNull(opinion);

		var reputationIdentity = reputationIdentityRepository.findByGxsId(gxsId).orElse(null);
		if (reputationIdentity != null)
		{
			if (reputationIdentity.getOpinion() == opinion)
			{
				return;
			}
			if (opinion == Opinion.NEUTRAL && !reputationIdentity.hasPeerOpinions())
			{
				reputationIdentityRepository.delete(reputationIdentity);
				return;
			}
			reputationIdentity.setOpinion(opinion);
		}
		else
		{
			if (opinion == Opinion.NEUTRAL)
			{
				return;
			}
			reputationIdentityRepository.save(new ReputationIdentity(gxsId, gxsIdentityRepository.findByGxsId(gxsId).orElse(null), opinion));
		}
	}

	/**
	 * Sets an identity reputation by a remote peer.
	 *
	 * @param location the location changing the opinion
	 * @param gxsId    the gxsId of the identity to change
	 * @param opinion  the opinion to set
	 */
	@Transactional
	public void updateIdentityReputation(Location location, GxsId gxsId, Opinion opinion)
	{
		Objects.requireNonNull(location);
		Objects.requireNonNull(gxsId);
		Objects.requireNonNull(opinion);

		boolean changed;

		var reputationIdentity = reputationIdentityRepository.findByGxsId(gxsId).orElse(null);
		if (reputationIdentity != null)
		{
			if (opinion == Opinion.NEUTRAL)
			{
				changed = reputationIdentity.removeOpinion(location.getLocationIdentifier());
			}
			else
			{
				changed = reputationIdentity.addOpinion(location.getLocationIdentifier(), opinion);
			}
		}
		else
		{
			if (opinion == Opinion.NEUTRAL)
			{
				return;
			}
			reputationIdentity = reputationIdentityRepository.save(new ReputationIdentity(gxsId, gxsIdentityRepository.findByGxsId(gxsId).orElse(null), location.getLocationIdentifier(), opinion));
			changed = true;
		}

		if (changed)
		{
			if (!reputationIdentity.hasPeerOpinions() && reputationIdentity.getOpinion() == Opinion.NEUTRAL)
			{
				reputationIdentityRepository.delete(reputationIdentity);
				return;
			}

			recalculateReputation(reputationIdentity);
		}
	}

	@Transactional
	public void banProfile(long pgpId)
	{
		var bannedProfile = reputationBannedProfileRepository.findByPgpIdentifier(pgpId);
		bannedProfile.ifPresent(ReputationBannedProfile::updateLastUsed);
		if (bannedProfile.isEmpty())
		{
			reputationBannedProfileRepository.save(new ReputationBannedProfile(pgpId));
		}
	}

	@Transactional
	public void unBanProfile(long pgpId)
	{
		var bannedProfile = reputationBannedProfileRepository.findByPgpIdentifier(pgpId);
		bannedProfile.ifPresent(reputationBannedProfileRepository::delete);
	}

	private void recalculateReputation(ReputationIdentity identity)
	{
		int negativeVotes = identity.getNegativeVotes();
		int positiveVotes = identity.getPositiveVotes();

		switch (identity.getOpinion())
		{
			case NEGATIVE -> identity.setReputation(Reputation.LOCALLY_NEGATIVE);
			case POSITIVE -> identity.setReputation(Reputation.LOCALLY_POSITIVE);
			case NEUTRAL ->
			{
				if (isProfileBanned(identity))
				{
					identity.setReputation(Reputation.LOCALLY_NEGATIVE);
					return;
				}

				if (positiveVotes > negativeVotes)
				{
					identity.setReputation(Reputation.REMOTELY_POSITIVE);
				}
				else if (positiveVotes < negativeVotes)
				{
					identity.setReputation(Reputation.REMOTELY_NEGATIVE);
				}
				else
				{
					identity.setReputation(Reputation.NEUTRAL);
				}
			}
		}
	}

	private boolean isProfileBanned(ReputationIdentity identity)
	{
		if (identity.getPgpIdentifier() != null)
		{
			var bannedProfile = reputationBannedProfileRepository.findByPgpIdentifier(identity.getPgpIdentifier());
			bannedProfile.ifPresent(ReputationBannedProfile::updateLastUsed);
			return bannedProfile.isPresent();
		}
		return false;
	}
}
