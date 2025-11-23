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

package io.xeres.ui.model.board;

import io.xeres.common.dto.board.BoardGroupDTO;

public final class BoardMapper
{
	private BoardMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static BoardGroup fromDTO(BoardGroupDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		var boardGroup = new BoardGroup();
		boardGroup.setId(dto.id());
		boardGroup.setName(dto.name());
		boardGroup.setGxsId(dto.gxsId());
		boardGroup.setDescription(dto.description());
		boardGroup.setSubscribed(dto.subscribed());
		boardGroup.setExternal(dto.external());
		return boardGroup;
	}
}
