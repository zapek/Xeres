/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

import io.xeres.app.database.model.gxs.GxsClientUpdate;
import io.xeres.app.database.model.gxs.GxsServiceSetting;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.repository.GxsClientUpdateRepository;
import io.xeres.app.database.repository.GxsServiceSettingRepository;
import io.xeres.app.xrs.service.RsServiceType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Transactional(readOnly = true)
public class GxsExchangeService
{
	private final GxsClientUpdateRepository gxsClientUpdateRepository;
	private final GxsServiceSettingRepository gxsServiceSettingRepository;

	public GxsExchangeService(GxsClientUpdateRepository gxsClientUpdateRepository, GxsServiceSettingRepository gxsServiceSettingRepository)
	{
		this.gxsClientUpdateRepository = gxsClientUpdateRepository;
		this.gxsServiceSettingRepository = gxsServiceSettingRepository;
	}

	public Instant getLastPeerGroupsUpdate(Location location, RsServiceType serviceType)
	{
		return gxsClientUpdateRepository.findByLocationAndServiceType(location, serviceType.getType())
				.map(GxsClientUpdate::getLastSynced)
				.orElse(Instant.EPOCH).truncatedTo(ChronoUnit.SECONDS);
	}

	@Transactional
	public void setLastPeerGroupsUpdate(Location location, RsServiceType serviceType)
	{
		var now = Instant.now(); // we always use local time
		gxsClientUpdateRepository.findByLocationAndServiceType(location, serviceType.getType())
				.ifPresentOrElse(gxsClientUpdate -> {
					gxsClientUpdate.setLastSynced(now);
					gxsClientUpdateRepository.save(gxsClientUpdate);
				}, () -> gxsClientUpdateRepository.save(new GxsClientUpdate(location, serviceType.getType(), now)));
	}

	/**
	 * Gets the last time the service's groups were updated. This uses the local time.
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
	 * Sets the last time the service's groups were updated. This uses the local time.
	 * @param serviceType the service type
	 */
	@Transactional
	public void setLastServiceGroupsUpdateNow(RsServiceType serviceType)
	{
		var now = Instant.now(); // we always use local time
		gxsServiceSettingRepository.findById(serviceType.getType())
				.ifPresentOrElse(gxsServiceSetting -> {
					gxsServiceSetting.setLastUpdated(now);
					gxsServiceSettingRepository.save(gxsServiceSetting);
				}, () -> gxsServiceSettingRepository.save(new GxsServiceSetting(serviceType.getType(), now)));
	}
}
