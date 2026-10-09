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

package io.xeres.app.database.model.mail;

import io.xeres.app.database.model.location.Location;
import io.xeres.app.service.UnHtmlService;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.dto.mail.MailMessageDTO;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import org.springframework.data.util.Streamable;

import java.util.List;
import java.util.Map;

import static io.xeres.app.database.model.FileMapper.toFileDTOs;

public final class MailMapper
{
	private MailMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static MailMessageDTO toDTO(MailMessageSummary mail, String fromName)
	{
		if (mail == null)
		{
			return null;
		}

		return new MailMessageDTO(
				mail.getId(),
				mail.getLocationFrom(),
				mail.getIdentityFrom(),
				fromName,
				null,
				null,
				null,
				null,
				null,
				null,
				mail.getPublished(),
				mail.getSubject(),
				null,
				mail.isRead(),
				mail.isStarred(),
				null
		);
	}

	public static List<MailMessageDTO> toSummaryMessageDTOs(Streamable<MailMessageSummary> mails, Map<LocationIdentifier, Location> locationsMap, Map<GxsId, IdentityGroupItem> identitiesMap)
	{
		return mails.stream()
				.map(item -> toDTO(item,
						item.getLocationFrom() != null ? locationsMap.getOrDefault(item.getLocationFrom(), Location.EMPTY).getName()
								: identitiesMap.getOrDefault(item.getIdentityFrom(), IdentityGroupItem.EMPTY).getName()
				))
				.toList();
	}

	public static MailMessageDTO toDTO(UnHtmlService unHtmlService, MailMessage mail, String fromName)
	{
		if (mail == null)
		{
			return null;
		}

		return new MailMessageDTO(
				mail.getId(),
				mail.getLocationFrom(),
				mail.getIdentityFrom(),
				fromName,
				mail.getLocationsTo(),
				mail.getLocationsCc(),
				mail.getLocationsBcc(),
				mail.getIdentitiesTo(),
				mail.getIdentitiesCc(),
				mail.getIdentitiesBcc(),
				mail.getPublished(),
				mail.getSubject(),
				unHtmlService.cleanupMessage(mail.getContent()),
				mail.isRead(),
				mail.isStarred(),
				toFileDTOs(mail.getFiles())
		);
	}
}
