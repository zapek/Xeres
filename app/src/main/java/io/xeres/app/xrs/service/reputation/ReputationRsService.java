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

package io.xeres.app.xrs.service.reputation;

import io.xeres.app.database.model.reputation.Opinion;
import io.xeres.app.database.model.reputation.ReputationIdentity;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.ReputationService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceInitPriority;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.reputation.item.ReputationRequestItem;
import io.xeres.app.xrs.service.reputation.item.ReputationUpdateItem;
import io.xeres.common.protocol.xrs.RsServiceType;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;

import static io.xeres.common.protocol.xrs.RsServiceType.GXS_REPUTATION;

@Component
public class ReputationRsService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(ReputationRsService.class);
	public static final int MAX_REPUTATION_UPDATES = 100;

	private final ReputationService reputationService;
	private final PeerConnectionManager peerConnectionManager;

	// XXX: cleanup, etc... see what RS is doing

	ReputationRsService(RsServiceRegistry rsServiceRegistry, ReputationService reputationService, PeerConnectionManager peerConnectionManager)
	{
		super(rsServiceRegistry);
		this.reputationService = reputationService;
		this.peerConnectionManager = peerConnectionManager;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return GXS_REPUTATION;
	}

	@Override
	public RsServiceInitPriority getInitPriority()
	{
		return RsServiceInitPriority.NORMAL;
	}

	@Override
	public void initialize(PeerConnection peerConnection)
	{
		peerConnection.scheduleAtFixedRate(
				() -> askForReputations(peerConnection),
				Duration.ofMinutes(0),
				Duration.ofMinutes(10)
		);
	}

	private void askForReputations(PeerConnection peerConnection)
	{
		log.debug("Asking {} for reputations...", peerConnection);
		peerConnectionManager.writeItem(peerConnection, new ReputationRequestItem((int) reputationService.getReputationUpdate(peerConnection.getLocation()).getEpochSecond()), this);
	}

	@Transactional
	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		if (item instanceof ReputationRequestItem reputationRequestItem)
		{
			sendReputation(sender, reputationRequestItem);
		}
		else if (item instanceof ReputationUpdateItem reputationUpdateItem)
		{
			receiveReputation(sender, reputationUpdateItem);
		}
	}

	private void sendReputation(PeerConnection sender, ReputationRequestItem item)
	{
		log.debug("{} sent ReputationRequestItem {}", sender, item);

		var updatedIdentities = reputationService.findUpdatedIdentities(Instant.ofEpochSecond(item.getLastUpdate()));

		Instant lastUpdated = updatedIdentities.stream()
				.map(ReputationIdentity::getOpinionUpdated)
				.max(Instant::compareTo)
				.orElse(Instant.EPOCH);

		updatedIdentities.stream()
				.gather(Gatherers.windowFixed(MAX_REPUTATION_UPDATES)) // RS uses that limit
				.forEach(chunk -> peerConnectionManager.writeItem(sender,
						new ReputationUpdateItem((int) lastUpdated.getEpochSecond(), chunk.stream()
								.collect(Collectors.toMap(ReputationIdentity::getGxsId, ReputationIdentity::getOpinionInt))),
						this));
	}

	private void receiveReputation(PeerConnection sender, ReputationUpdateItem item)
	{
		MapUtils.emptyIfNull(item.getOpinions()).forEach((gxsId, opinion) -> {
			var opinionToSet = Opinion.from(opinion);
			if (opinionToSet == null)
			{
				log.warn("Wrong opinion {} received from {}", opinion, sender);
				return;
			}
			reputationService.updateIdentityReputation(sender.getLocation(), gxsId, opinionToSet);
		});
		reputationService.storeReputationUpdate(sender.getLocation(), Instant.ofEpochSecond(item.getLatestUpdate()));
	}
}
