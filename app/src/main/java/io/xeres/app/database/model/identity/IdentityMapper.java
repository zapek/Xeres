/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

import io.xeres.app.xrs.service.gxsid.item.GxsIdGroupItem;
import io.xeres.common.dto.identity.IdentityDTO;
import io.xeres.common.identity.Type;

import java.util.List;

import static java.util.Collections.emptyList;

public final class IdentityMapper
{
	private IdentityMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static IdentityDTO toDTO(Identity identity)
	{
		if (identity == null)
		{
			return null;
		}

		return new IdentityDTO(
				identity.getId(),
				identity.getGxsIdGroupItem().getName(),
				identity.getGxsIdGroupItem().getGxsId(),
				identity.getGxsIdGroupItem().getPublished(),
				identity.getType());
	}

	public static List<IdentityDTO> toDTOs(List<Identity> identities)
	{
		if (identities == null)
		{
			return emptyList();
		}

		return identities.stream()
				.map(IdentityMapper::toDTO)
				.toList();
	}

	public static IdentityDTO toDTO(GxsIdGroupItem gxsIdGroupItem)
	{
		if (gxsIdGroupItem == null)
		{
			return null;
		}

		return new IdentityDTO(
				gxsIdGroupItem.getId(),
				gxsIdGroupItem.getName(),
				gxsIdGroupItem.getGxsId(),
				gxsIdGroupItem.getPublished(),
				gxsIdGroupItem.getAdminPrivateKey() != null ? Type.SIGNED : Type.OTHER // XXX: the type computation is wrong... find a better way
		);
	}

	public static List<IdentityDTO> toGxsIdDTOs(List<GxsIdGroupItem> gxsIdGroupItems)
	{
		if (gxsIdGroupItems == null)
		{
			return emptyList();
		}

		return gxsIdGroupItems.stream()
				.map(IdentityMapper::toDTO)
				.toList();
	}
}
