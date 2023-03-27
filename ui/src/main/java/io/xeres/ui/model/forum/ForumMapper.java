/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.ui.model.forum;

import io.xeres.common.dto.forum.ForumDTO;
import io.xeres.common.message.forum.Forum;

public final class ForumMapper
{
	private ForumMapper()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static Forum fromDTO(ForumDTO dto)
	{
		if (dto == null)
		{
			return null;
		}

		var forum = new Forum();
		forum.setId(dto.id());
		forum.setName(dto.name());
		forum.setGxsId(dto.gxsId());
		forum.setDescription(dto.description());
		forum.setSubscribed(dto.subscribed());
		return forum;
	}
}
