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

package io.xeres.app.xrs.service.gxstunnel;

import io.xeres.common.id.GxsId;
import io.xeres.common.id.Sha1Sum;
import io.xeres.common.util.SecureRandomUtils;

final class DestinationHash
{
	private DestinationHash()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static Sha1Sum createRandomHash(GxsId to)
	{
		var buf = new byte[Sha1Sum.LENGTH];

		SecureRandomUtils.nextBytes(buf);
		System.arraycopy(to.getBytes(), 0, buf, 4, GxsId.LENGTH);

		return new Sha1Sum(buf);
	}

	public static GxsId getGxsIdFromHash(Sha1Sum hash)
	{
		var buf = new byte[GxsId.LENGTH];

		System.arraycopy(hash.getBytes(), 4, buf, 0, GxsId.LENGTH);
		return new GxsId(buf);
	}
}
