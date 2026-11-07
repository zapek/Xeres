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

package io.xeres.ui.model.mail;

import io.xeres.common.dto.mail.MailMessageDTO;
import io.xeres.ui.client.PaginatedResponse;
import io.xeres.ui.client.WindowedResponse;

import static org.apache.commons.collections4.SetUtils.emptyIfNull;

public final class MailMapper
{
	private MailMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static MailMessage fromDTO(MailMessageDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		var mailMessage = new MailMessage();
		mailMessage.setId(dto.id());
		mailMessage.setType(dto.type());
		mailMessage.setSent(dto.sent());
		mailMessage.setPublished(dto.published());
		mailMessage.setAuthorName(dto.fromName());
		mailMessage.setLocationFrom(dto.locationFrom());
		mailMessage.setIdentityFrom(dto.identityFrom());
		mailMessage.addLocationsTo(emptyIfNull(dto.locationsTo()));
		mailMessage.addLocationsCc(emptyIfNull(dto.locationsCc()));
		mailMessage.addLocationsBcc(emptyIfNull(dto.locationsBcc()));
		mailMessage.addIdentitiesTo(emptyIfNull(dto.identitiesTo()));
		mailMessage.addIdentitiesCc(emptyIfNull(dto.identitiesCc()));
		mailMessage.addIdentitiesBcc(emptyIfNull(dto.identitiesBcc()));
		mailMessage.setSubject(dto.subject());
		mailMessage.setContent(dto.content());
		mailMessage.setStarred(dto.starred());
		mailMessage.setRead(dto.read());
		return mailMessage;
	}

	public static PaginatedResponse<MailMessage> fromDTO(PaginatedResponse<MailMessageDTO> dto)
	{
		return new PaginatedResponse<>(
				dto.content().stream()
						.map(MailMapper::fromDTO)
						.toList(),
				dto.page()
		);
	}

	public static WindowedResponse<MailMessage> fromDTO(WindowedResponse<MailMessageDTO> dto)
	{
		return new WindowedResponse<>(
				dto.content().stream()
						.map(MailMapper::fromDTO)
						.toList(),
				dto.empty(),
				dto.last()
		);
	}
}
