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

package io.xeres.common.dto.identity;

import io.xeres.common.id.GxsId;
import io.xeres.common.identity.Type;

import java.time.Instant;
import java.util.Objects;

public record IdentityDTO(
		long id,
		String name,
		GxsId gxsId,
		Instant created,
		Type type
)
{

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		IdentityDTO that = (IdentityDTO) o;
		return name.equals(that.name) && gxsId.equals(that.gxsId);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, gxsId);
	}

	@Override
	public String toString()
	{
		return "IdentityDTO{" +
				"name='" + name + '\'' +
				", gxsId=" + gxsId +
				", type=" + type +
				'}';
	}
}
