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
import io.xeres.app.xrs.service.gxs.item.GxsSyncGroupStatsItem;
import io.xeres.app.xrs.service.gxs.item.RequestType;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MsgId;
import io.xeres.common.protocol.xrs.RsServiceType;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Helper service to manage various GXS group and message functions.
 */
@Service
public class GxsHelperService<G extends GxsGroupItem, M extends GxsMessageItem>
{
	private final GxsClientUpdateRepository gxsClientUpdateRepository;
	private final GxsServiceSettingRepository gxsServiceSettingRepository;
	private final GxsGroupItemRepository gxsGroupItemRepository;
	private final GxsMessageItemRepository gxsMessageItemRepository;

	public GxsHelperService(GxsClientUpdateRepository gxsClientUpdateRepository, GxsServiceSettingRepository gxsServiceSettingRepository, GxsGroupItemRepository gxsGroupItemRepository, GxsMessageItemRepository gxsMessageItemRepository)
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
	 * @param gxsId     the group's gxs id.
	 * @param serviceType the service type.
	 * @return the time when the peer last updated its group messages, in peer's time
	 */
	public Instant getLastPeerMessagesUpdate(Location location, GxsId gxsId, RsServiceType serviceType)
	{
		return gxsClientUpdateRepository.findByLocationAndServiceType(location, serviceType.getType())
				.map(gxsClientUpdate -> gxsClientUpdate.getMessageUpdate(gxsId))
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

	/**
	 * Sets the last update time of a peer's messages. The peer's time is always used, not our local time.
	 *
	 * @param location    the peer's location
	 * @param gxsId       the group
	 * @param update      the peer's last update time, in peer's time (so given by the peer itself). Never supply a time computed locally.
	 * @param serviceType the service type
	 */
	@Transactional
	public void setLastPeerMessageUpdate(Location location, GxsId gxsId, Instant update, RsServiceType serviceType)
	{
		gxsClientUpdateRepository.findByLocationAndServiceType(location, serviceType.getType())
				.ifPresentOrElse(gxsClientUpdate -> gxsClientUpdate.putMessageUpdate(gxsId, update), () -> {
					var clientUpdate = new GxsClientUpdate(location, serviceType.getType(), Instant.EPOCH);
					clientUpdate.putMessageUpdate(gxsId, update);
					gxsClientUpdateRepository.save(clientUpdate);
				});
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
	 * Sets the service group's last update to now.
	 *
	 * @param serviceType the service type
	 */
	@Transactional
	public void setLastServiceGroupsUpdateNow(RsServiceType serviceType)
	{
		var now = Instant.now(); // we always use local time
		gxsServiceSettingRepository.findById(serviceType.getType())
				.ifPresentOrElse(gxsServiceSetting -> gxsServiceSetting.setLastUpdated(now), () -> gxsServiceSettingRepository.save(new GxsServiceSetting(serviceType.getType(), now)));
	}

	/**
	 * Saves an external group.
	 *
	 * @param gxsGroupItem the group
	 * @param confirmation the confirmation predicate
	 * @return the group
	 */
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

	/**
	 * Gets a group.
	 *
	 * @param gxsId the gxsId of the group
	 * @return the group, null if it doesn't exist
	 */
	public GxsGroupItem getGroup(GxsId gxsId)
	{
		return gxsGroupItemRepository.findByGxsId(gxsId).orElse(null);
	}

	@Transactional(readOnly = true)
	public Optional<GxsSyncGroupStatsItem> findGroupStatsByGxsId(GxsId gxsId)
	{
		return gxsGroupItemRepository.findByGxsIdAndSubscribedIsTrue(gxsId)
				.map(group -> {
					var numberOfPosts = gxsMessageItemRepository.countByGxsId(group.getGxsId());
					return new GxsSyncGroupStatsItem(RequestType.RESPONSE, group.getGxsId(), group.getLastUpdated() != null ? (int) group.getLastUpdated().getEpochSecond() : 0, numberOfPosts);
				});
	}

	@Transactional
	public void updateGroupStats(GxsSyncGroupStatsItem item)
	{
		gxsGroupItemRepository.findByGxsId(item.getGxsId()).ifPresent(group -> {
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
		gxsMessageItem.setId(gxsMessageItemRepository.findByGxsIdAndMsgId(gxsMessageItem.getGxsId(), gxsMessageItem.getMsgId()).orElse(gxsMessageItem).getId());
		if (confirmation.test(gxsMessageItem) /*&& gxsMessageItem.isExternal()*/) // Don't overwrite our own messages (XXX: find a way to do the check)
		{
			return Optional.of(gxsMessageItemRepository.save(gxsMessageItem));
		}
		return Optional.empty();
	}

	public void fixHiddenMessages(GxsId gxsId, Instant since)
	{
		gxsMessageItemRepository.fixIntervalDuplicates(gxsId, since);
		gxsMessageItemRepository.hideOldDuplicates(gxsId, since);
	}

	@Transactional
	public Optional<CommentMessageItem> saveComment(CommentMessageItem commentMessageItem, Predicate<CommentMessageItem> confirmation)
	{
		commentMessageItem.setId(gxsMessageItemRepository.findByGxsIdAndMsgId(commentMessageItem.getGxsId(), commentMessageItem.getMsgId()).orElse(commentMessageItem).getId());
		if (confirmation.test(commentMessageItem) /*&& gxsMessageItem.isExternal()*/) // Don't overwrite our own messages (XXX: find a way to do the check)
		{
			return Optional.of(gxsMessageItemRepository.save(commentMessageItem));
		}
		return Optional.empty();
	}

	@Transactional
	public Optional<VoteMessageItem> saveVote(VoteMessageItem voteMessageItem, Predicate<VoteMessageItem> confirmation)
	{
		voteMessageItem.setId(gxsMessageItemRepository.findByGxsIdAndMsgId(voteMessageItem.getGxsId(), voteMessageItem.getMsgId()).orElse(voteMessageItem).getId());
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
	 * @param msgId the message id
	 * @param authorGxsId  the author id
	 */
	@Transactional
	public void overrideMessage(GxsId gxsId, MsgId msgId, GxsId authorGxsId)
	{
		gxsMessageItemRepository.findByGxsIdAndMsgId(gxsId, msgId).ifPresent(gxsMessageItem -> {
			if (Objects.equals(authorGxsId, gxsMessageItem.getAuthorGxsId()))
			{
				gxsMessageItem.setHidden(true);
			}
		});
	}

	/**
	 * Updates the last posted field of the group. This allows knowing when the last time a message was added in a group was.
	 *
	 * @param gxsId      the group
	 * @param lastPosted the last posted value
	 */
	@Transactional
	public void updateLastPosted(GxsId gxsId, Instant lastPosted)
	{
		gxsGroupItemRepository.findByGxsId(gxsId).ifPresent(gxsGroupItem -> {
			if (gxsGroupItem.getLastUpdated() == null || gxsGroupItem.getLastUpdated().isBefore(lastPosted))
			{
				gxsGroupItem.setLastUpdated(lastPosted);
			}
		});
	}
}
