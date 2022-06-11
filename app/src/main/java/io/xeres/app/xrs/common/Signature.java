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

package io.xeres.app.xrs.common;

import io.xeres.common.id.GxsId;

import java.util.Arrays;
import java.util.Objects;

// XXX: maybe should be in crypto? not sure... It's related to identities but I don't like the structure
public record Signature(GxsId gxsId, byte[] data)
{
	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		var signature = (Signature) o;
		return gxsId.equals(signature.gxsId) && Arrays.equals(data, signature.data);
	}

	@Override
	public int hashCode()
	{
		var result = Objects.hash(gxsId);
		result = 31 * result + Arrays.hashCode(data);
		return result;
	}

	@Override
	public String toString()
	{
		return "Signature{" +
				"gxsId=" + gxsId +
				'}';
	}
}
