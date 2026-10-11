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

package io.xeres.common.rest.mail;

import io.xeres.common.dto.FileDTO;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Set;

public record CreateMailMessageRequest(
		Set<LocationIdentifier> locationsTo,
		Set<LocationIdentifier> locationsCc,
		Set<LocationIdentifier> locationsBcc,

		Set<GxsId> identitiesTo,
		Set<GxsId> identitiesCc,
		Set<GxsId> identitiesBcc,

		@NotBlank(message = "Subject must not be empty")
		String subject,

		@NotBlank(message = "Content must not be empty")
		String content,

		List<FileDTO> files
)
{
}
