/*
 * Copyright (c) 2023-2026 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.service.gxs.item.GxsSyncGroupStatsItem;
import io.xeres.app.xrs.service.gxs.item.RequestType;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MessageId;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

	@Transactional(readOnly = true)
	public Optional<GxsSyncGroupStatsItem> findGroupStatsByGxsId(GxsId groupId)
	{
		return gxsGroupItemRepository.findByGxsIdAndSubscribedIsTrue(groupId)
				.map(group -> {
					var numberOfPosts = gxsMessageItemRepository.countByGxsId(group.getGxsId());
					return new GxsSyncGroupStatsItem(RequestType.RESPONSE, group.getGxsId(), group.getLastUpdated() != null ? (int) group.getLastUpdated().getEpochSecond() : 0, numberOfPosts);
				});
	}

	@Transactional
	public void updateGroupStats(GxsSyncGroupStatsItem item)
	{
		gxsGroupItemRepository.findByGxsId(item.getGroupId()).ifPresent(group -> {
			group.setVisibleMessageCount(Math.max(group.getVisibleMessageCount(), item.getNumberOfPosts()));
			if (item.getLastPostTimestamp() > group.getLastActivity().getEpochSecond())
			{
				group.setLastActivity(Instant.ofEpochSecond(item.getLastPostTimestamp()));
			}
			// XXX: how to set popularity?
		});
	}

	@Transactional
	public Set<GxsId> findGroupsToRequestStats(Instant now, Duration delay)
	{
		return gxsGroupItemRepository.findByOrderByLastStatistics(Limit.of(2)).stream()
				.filter(gxsGroupItem -> Duration.between(gxsGroupItem.getLastStatistics(), now).compareTo(delay) > 0)
				.map(gxsGroupItem -> {
					gxsGroupItem.setLastStatistics(now);
					return gxsGroupItem.getGxsId();
				})
				.collect(Collectors.toSet());
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

	/**
	 * Overrides a message. This allows to "edit" a message. If the message is found, it's marked as hidden.
	 *
	 * @param gxsId     the group of the message
	 * @param messageId the message id
	 * @param authorId  the author id
	 */
	@Transactional
	public void overrideMessage(GxsId gxsId, MessageId messageId, GxsId authorId)
	{
		gxsMessageItemRepository.findByGxsIdAndMessageId(gxsId, messageId).ifPresent(gxsMessageItem -> {
			if (authorId.equals(gxsMessageItem.getAuthorId()))
			{
				gxsMessageItem.setHidden(true);
			}
		});
	}

	@Transactional
	public void updateLastPosted(GxsId groupId, Instant lastPosted)
	{
		gxsGroupItemRepository.findByGxsId(groupId).ifPresent(gxsGroupItem -> {
			if (gxsGroupItem.getLastUpdated() == null || gxsGroupItem.getLastUpdated().isBefore(lastPosted))
			{
				gxsGroupItem.setLastUpdated(lastPosted);
			}
		});
	}
}
