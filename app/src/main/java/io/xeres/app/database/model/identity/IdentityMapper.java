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

package io.xeres.app.database.model.identity;

import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.dto.identity.IdentityDTO;

import java.util.List;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public final class IdentityMapper
{
	private IdentityMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static IdentityDTO toDTO(IdentityGroupItem identityGroupItem)
	{
		if (identityGroupItem == null)
		{
			return null;
		}

		return new IdentityDTO(
				identityGroupItem.getId(),
				identityGroupItem.getName(),
				identityGroupItem.getGxsId(),
				identityGroupItem.getPublished(),
				identityGroupItem.getType(),
				identityGroupItem.hasImage()
		);
	}

	public static List<IdentityDTO> toGxsIdDTOs(List<IdentityGroupItem> identityGroupItems)
	{
		return emptyIfNull(identityGroupItems).stream()
				.map(IdentityMapper::toDTO)
				.toList();
	}
}
