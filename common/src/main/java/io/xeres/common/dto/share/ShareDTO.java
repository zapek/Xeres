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

package io.xeres.common.dto.share;

import io.xeres.common.pgp.Trust;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

import static io.xeres.common.dto.share.ShareConstants.NAME_LENGTH_MAX;
import static io.xeres.common.dto.share.ShareConstants.NAME_LENGTH_MIN;

public record ShareDTO(
		long id,

		@NotNull(message = "Name is mandatory")
		@Size(message = "Name length must be between " + NAME_LENGTH_MIN + " and " + NAME_LENGTH_MAX + " characters", min = NAME_LENGTH_MIN, max = NAME_LENGTH_MAX)
		String name,

		@NotNull(message = "Path is mandatory")
		@Size(message = "Path length must be between 1 and 255 characters", min = 1, max = 255)
		String path,

		boolean searchable,

		Trust browsable,

		Instant lastScanned
)
{
	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || getClass() != o.getClass())
		{
			return false;
		}
		var shareDTO = (ShareDTO) o;
		return id == shareDTO.id && searchable == shareDTO.searchable && Objects.equals(name, shareDTO.name) && Objects.equals(path, shareDTO.path) && browsable == shareDTO.browsable;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name);
	}

	@Override
	public String toString()
	{
		return "ShareDTO{" +
				"name='" + name + '\'' +
				", path='" + path + '\'' +
				", searchable=" + searchable +
				", browsable=" + browsable +
				'}';
	}
}
