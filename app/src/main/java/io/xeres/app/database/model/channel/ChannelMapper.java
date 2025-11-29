/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.database.model.channel;

import io.xeres.app.xrs.service.channel.item.ChannelGroupItem;
import io.xeres.common.dto.channel.ChannelGroupDTO;

import java.util.List;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public final class ChannelMapper
{
	private ChannelMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ChannelGroupDTO toDTO(ChannelGroupItem item)
	{
		if (item == null)
		{
			return null;
		}

		return new ChannelGroupDTO(
				item.getId(),
				item.getGxsId(),
				item.getName(),
				item.getDescription(),
				item.hasImage(),
				item.isSubscribed(),
				item.isExternal()
		);
	}

	public static List<ChannelGroupDTO> toDTOs(List<ChannelGroupItem> items)
	{
		return emptyIfNull(items).stream()
				.map(ChannelMapper::toDTO)
				.toList();
	}
}
