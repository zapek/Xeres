/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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
import io.xeres.app.database.model.gxs.GxsServiceSetting;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.repository.GxsClientUpdateRepository;
import io.xeres.app.database.repository.GxsGroupItemRepository;
import io.xeres.app.database.repository.GxsServiceSettingRepository;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.GxsId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Helper service to manage group updates comparisons.
 */
@Service
public class GxsUpdateService<G extends GxsGroupItem>
{
	private final GxsClientUpdateRepository gxsClientUpdateRepository;
	private final GxsServiceSettingRepository gxsServiceSettingRepository;
	private final GxsGroupItemRepository gxsGroupItemRepository;

	public GxsUpdateService(GxsClientUpdateRepository gxsClientUpdateRepository, GxsServiceSettingRepository gxsServiceSettingRepository, GxsGroupItemRepository gxsGroupItemRepository)
	{
		this.gxsClientUpdateRepository = gxsClientUpdateRepository;
		this.gxsServiceSettingRepository = gxsServiceSettingRepository;
		this.gxsGroupItemRepository = gxsGroupItemRepository;
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
	public void saveGroup(G gxsGroupItem, Predicate<G> confirmation, Consumer<G> action)
	{
		gxsGroupItem.setId(gxsGroupItemRepository.findByGxsId(gxsGroupItem.getGxsId()).orElse(gxsGroupItem).getId());
		if (confirmation.test(gxsGroupItem) && gxsGroupItem.isExternal()) // Don't overwrite our own groups
		{
			var saved = gxsGroupItemRepository.save(gxsGroupItem);
			action.accept(saved);
		}
	}

	public boolean groupExists(G gxsGroupItem)
	{
		return gxsGroupItemRepository.findByGxsId(gxsGroupItem.getGxsId()).isPresent();
	}
}
