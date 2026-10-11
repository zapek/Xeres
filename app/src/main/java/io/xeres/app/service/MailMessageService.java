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

import io.xeres.app.database.model.gxs.GxsGroupItem;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.model.mail.MailMessageSummary;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MailMessageService
{
	private final IdentityService identityService;
	private final LocationService locationService;

	public MailMessageService(IdentityService identityService, LocationService locationService)
	{
		this.identityService = identityService;
		this.locationService = locationService;
	}

	public Map<GxsId, IdentityGroupItem> getIdentitiesMapFromSummaries(Streamable<MailMessageSummary> mailMessages)
	{
		var authors = mailMessages.stream()
				.map(MailMessageSummary::getIdentityFrom)
				.collect(Collectors.toSet());

		return identityService.findAll(authors).stream()
				.collect(Collectors.toMap(GxsGroupItem::getGxsId, Function.identity()));
	}

	public Map<LocationIdentifier, Location> getLocationsMapFromSummaries(Streamable<MailMessageSummary> mailMessages)
	{
		var authors = mailMessages.stream()
				.map(MailMessageSummary::getLocationFrom)
				.collect(Collectors.toSet());

		return locationService.findAll(authors).stream()
				.collect(Collectors.toMap(Location::getLocationIdentifier, Function.identity()));
	}
}
