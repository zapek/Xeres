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

package io.xeres.ui.model;

import io.xeres.common.dto.FileDTO;
import io.xeres.common.id.Sha1Sum;

import java.util.List;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public final class FileMapper
{
	private FileMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static List<File> fromFileDTOs(List<FileDTO> dtos)
	{
		return emptyIfNull(dtos).stream()
				.map(FileMapper::fromFileDTO)
				.toList();
	}

	public static File fromFileDTO(FileDTO dto)
	{
		if (dto == null)
		{
			return null;
		}
		return new File(dto.name(), dto.path(), File.State.DONE, dto.size(), dto.hash().asString());
	}

	public static List<FileDTO> toFileDTOs(List<File> files)
	{
		return emptyIfNull(files).stream()
				.map(FileMapper::toDTO)
				.toList();
	}

	public static FileDTO toDTO(File file)
	{
		if (file == null)
		{
			return null;
		}
		return new FileDTO(file.getSize(), Sha1Sum.fromString(file.getHash()), file.getName(), file.getPath(), 0);
	}
}
