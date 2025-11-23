/*
 * Copyright (c) 2023-2025 by David Gerber - https://zapek.com
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

import io.xeres.app.database.model.gxs.GxsClientUpdate;
import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.database.model.gxs.GxsServiceSetting;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.repository.GxsClientUpdateRepository;
import io.xeres.app.database.repository.GxsGroupItemRepository;
import io.xeres.app.database.repository.GxsMessageItemRepository;
import io.xeres.app.database.repository.GxsServiceSettingRepository;
import io.xeres.app.xrs.common.CommentMessageItem;
import io.xeres.app.xrs.common.VoteMessageItem;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.GxsId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Helper service to manage group and message updates comparisons.
 */
@Service
public class GxsUpdateService<G extends GxsGroupItem, M extends GxsMessageItem>
{
	private final GxsClientUpdateRepository gxsClientUpdateRepository;
	private final GxsServiceSettingRepository gxsServiceSettingRepository;
	private final GxsGroupItemRepository gxsGroupItemRepository;
	private final GxsMessageItemRepository gxsMessageItemRepository;

	public GxsUpdateService(GxsClientUpdateRepository gxsClientUpdateRepository, GxsServiceSettingRepository gxsServiceSettingRepository, GxsGroupItemRepository gxsGroupItemRepository, GxsMessageItemRepository gxsMessageItemRepository)
	{
		this.gxsClientUpdateRepository = gxsClientUpdateRepository;
		this.gxsServiceSettingRepository = gxsServiceSettingRepository;
		this.gxsGroupItemRepository = gxsGroupItemRepository;
		this.gxsMessageItemRepository = gxsMessageItemRepository;
	}

	/**
	 * Gets the last update time of the peer's groups. The peer's time is always used, not our local time.
	 *
	 * @param location    the peer's location
	 * @param serviceType the service type
	 * @return the time when the peer last updated its groups, in peer's time
	 */
	public Instant getLastPeerGroupsUpdate(Location location, RsServiceType serviceType)
	{
		return gxsClientUpdateRepository.findByLocationAndServiceType(location, serviceType.getType())
				.map(GxsClientUpdate::getLastSynced)
				.orElse(Instant.EPOCH).truncatedTo(ChronoUnit.SECONDS);
	}

	/**
	 * Gets the last update of the peer's group messages. The peer's time is always used, not our local time.
	 *
	 * @param location    the peer's location.
	 * @param groupId     the group's gxs id.
	 * @param serviceType the service type.
	 * @return the time when the peer last updated its group messages, in peer's time
	 */
	public Instant getLastPeerMessagesUpdate(Location location, GxsId groupId, RsServiceType serviceType)
	{
		return gxsClientUpdateRepository.findByLocationAndServiceType(location, serviceType.getType())
				.map(gxsClientUpdate -> gxsClientUpdate.getMessageUpdate(groupId))
				.orElse(Instant.EPOCH).truncatedTo(ChronoUnit.SECONDS);
	}

	/**
	 * Sets the last update time of the peer's groups. The peer's time is always used, not our local time.
	 *
	 * @param location    the peer's location
	 * @param update      the peer's last update time, in peer's time (so given by the peer itself). Never supply a time computed locally
	 * @param serviceType the service type
	 */
	@Transactional
	public void setLastPeerGroupsUpdate(Location location, Instant update, RsServiceType serviceType)
	{
		gxsClientUpdateRepository.findByLocationAndServiceType(location, serviceType.getType())
				.ifPresentOrElse(gxsClientUpdate -> gxsClientUpdate.setLastSynced(update), () -> gxsClientUpdateRepository.save(new GxsClientUpdate(location, serviceType.getType(), update)));
	}

	@Transactional
	public void setLastPeerMessageUpdate(Location location, GxsId groupId, Instant update, RsServiceType serviceType)
	{
		gxsClientUpdateRepository.findByLocationAndServiceType(location, serviceType.getType())
				.ifPresentOrElse(gxsClientUpdate -> gxsClientUpdate.addMessageUpdate(groupId, update), () -> gxsClientUpdateRepository.save(new GxsClientUpdate(location, serviceType.getType(), update)));
	}

	/**
	 * Gets the last time our service's groups were updated. This uses the local time.
	 *
	 * @param serviceType the service type
	 * @return the last time
	 */
	public Instant getLastServiceGroupsUpdate(RsServiceType serviceType)
	{
		return gxsServiceSettingRepository.findById(serviceType.getType())
				.map(GxsServiceSetting::getLastUpdated)
				.orElse(Instant.EPOCH).truncatedTo(ChronoUnit.SECONDS);
	}

	/**
	 * Sets the last time our service's groups were updated.
	 *
	 * @param serviceType the service type
	 */
	@Transactional
	public void setLastServiceGroupsUpdateNow(RsServiceType serviceType)
	{
		var now = Instant.now(); // we always use local time
		gxsServiceSettingRepository.findById(serviceType.getType())
				.ifPresentOrElse(gxsServiceSetting -> gxsServiceSetting.setLastUpdated(Instant.now()), () -> gxsServiceSettingRepository.save(new GxsServiceSetting(serviceType.getType(), now)));
	}

	@Transactional
	public Optional<G> saveGroup(G gxsGroupItem, Predicate<G> confirmation)
	{
		gxsGroupItem.setId(gxsGroupItemRepository.findByGxsId(gxsGroupItem.getGxsId()).orElse(gxsGroupItem).getId());
		if (confirmation.test(gxsGroupItem) && gxsGroupItem.isExternal()) // Don't overwrite our own groups
		{
			return Optional.of(gxsGroupItemRepository.save(gxsGroupItem));
		}
		return Optional.empty();
	}

	public Optional<GxsGroupItem> getExistingGroup(G gxsGroupItem)
	{
		return gxsGroupItemRepository.findByGxsId(gxsGroupItem.getGxsId());
	}

	@Transactional
	public Optional<M> saveMessage(M gxsMessageItem, Predicate<M> confirmation)
	{
		gxsMessageItem.setId(gxsMessageItemRepository.findByGxsIdAndMessageId(gxsMessageItem.getGxsId(), gxsMessageItem.getMessageId()).orElse(gxsMessageItem).getId());
		if (confirmation.test(gxsMessageItem) /*&& gxsMessageItem.isExternal()*/) // Don't overwrite our own messages (XXX: find a way to do the check)
		{
			return Optional.of(gxsMessageItemRepository.save(gxsMessageItem));
		}
		return Optional.empty();
	}

	@Transactional
	public Optional<CommentMessageItem> saveComment(CommentMessageItem commentMessageItem, Predicate<CommentMessageItem> confirmation)
	{
		commentMessageItem.setId(gxsMessageItemRepository.findByGxsIdAndMessageId(commentMessageItem.getGxsId(), commentMessageItem.getMessageId()).orElse(commentMessageItem).getId());
		if (confirmation.test(commentMessageItem) /*&& gxsMessageItem.isExternal()*/) // Don't overwrite our own messages (XXX: find a way to do the check)
		{
			return Optional.of(gxsMessageItemRepository.save(commentMessageItem));
		}
		return Optional.empty();
	}

	@Transactional
	public Optional<VoteMessageItem> saveVote(VoteMessageItem voteMessageItem, Predicate<VoteMessageItem> confirmation)
	{
		voteMessageItem.setId(gxsMessageItemRepository.findByGxsIdAndMessageId(voteMessageItem.getGxsId(), voteMessageItem.getMessageId()).orElse(voteMessageItem).getId());
		if (confirmation.test(voteMessageItem) /*&& gxsMessageItem.isExternal()*/) // Don't overwrite our own messages (XXX: find a way to do the check)
		{
			return Optional.of(gxsMessageItemRepository.save(voteMessageItem));
		}
		return Optional.empty();
	}
}
