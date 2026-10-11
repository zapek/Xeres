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

package io.xeres.app.database.model;

import io.xeres.app.xrs.common.FileItem;
import io.xeres.common.dto.FileDTO;

import java.util.List;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public final class FileMapper
{
	private FileMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static FileDTO toFileDTO(FileItem item)
	{
		if (item == null)
		{
			return null;
		}
		return new FileDTO(
				item.size(),
				item.hash(),
				item.name(),
				item.path(),
				item.age()
		);
	}

	public static List<FileDTO> toFileDTOs(List<FileItem> files)
	{
		return emptyIfNull(files).stream()
				.map(FileMapper::toFileDTO)
				.toList();
	}

	public static List<FileItem> toFileItems(List<FileDTO> dtos)
	{
		return emptyIfNull(dtos).stream()
				.map(FileMapper::toFileItem)
				.toList();
	}

	public static FileItem toFileItem(FileDTO dto)
	{
		if (dto == null)
		{
			return null;
		}
		return new FileItem(dto.size(), dto.hash(), dto.name(), dto.path(), dto.age());
	}
}
