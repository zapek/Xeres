/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.app.database.model.share;

import io.xeres.app.database.model.file.File;
import io.xeres.common.dto.share.ShareDTO;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public final class ShareMapper
{
	private ShareMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ShareDTO toDTO(Share share, String path)
	{
		if (share == null)
		{
			return null;
		}

		return new ShareDTO(
				share.getId(),
				share.getName(),
				path,
				share.isSearchable(),
				share.getBrowsable()
		);
	}

	public static List<ShareDTO> toDTOs(List<Share> shares, Map<Long, String> filesMap)
	{
		return emptyIfNull(shares).stream()
				.map(share -> toDTO(share, filesMap.get(share.getId())))
				.toList();
	}

	public static Share fromDTO(ShareDTO shareDTO)
	{
		if (shareDTO == null)
		{
			return null;
		}

		var share = new Share();
		share.setId(shareDTO.id());
		share.setName(shareDTO.name());
		share.setFile(File.createFile(Paths.get(shareDTO.path()))); // XXX: ouch... not sure that works...
		share.setSearchable(shareDTO.searchable());
		share.setBrowsable(shareDTO.browsable());
		return share;
	}

	public static List<Share> fromDTOs(List<ShareDTO> shares)
	{
		return shares.stream()
				.map(ShareMapper::fromDTO)
				.toList();
	}
}
