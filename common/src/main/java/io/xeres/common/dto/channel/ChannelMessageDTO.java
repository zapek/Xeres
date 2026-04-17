/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.common.dto.channel;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MsgId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

public record ChannelMessageDTO(
		long id,
		GxsId gxsId,
		MsgId msgId,
		long originalId,
		long parentId,
		GxsId authorGxsId,
		String authorName,
		String name,
		Instant published,
		String content,
		boolean hasImage,
		int imageWidth,
		int imageHeight,
		boolean hasFiles,
		@JsonInclude(NON_EMPTY)
		List<ChannelFileDTO> files,
		boolean read
)
{
	public ChannelMessageDTO
	{
		if (files == null)
		{
			files = new ArrayList<>();
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof ChannelMessageDTO that))
		{
			return false;
		}
		return Objects.equals(gxsId, that.gxsId);
	}

	@Override
	public int hashCode()
	{
		return Objects.hashCode(gxsId);
	}

	@Override
	public String toString()
	{
		return "ChannelMessageDTO{" +
				"gxsId=" + gxsId +
				", name='" + name + '\'' +
				'}';
	}
}
