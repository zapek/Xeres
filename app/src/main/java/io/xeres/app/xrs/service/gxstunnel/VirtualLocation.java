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

import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.app.database.model.location.Location;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;

final class VirtualLocation
{
	private VirtualLocation()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static Location fromGxsIds(GxsId ownId, GxsId distantId)
	{
		var buf = new byte[GxsId.LENGTH * 2];

		// Sort the IDs, that way the same ID is generated on both sides.
		// This helps with debugging.
		if (ownId.compareTo(distantId) < 0)
		{
			System.arraycopy(ownId.getBytes(), 0, buf, 0, GxsId.LENGTH);
			System.arraycopy(distantId.getBytes(), 0, buf, GxsId.LENGTH, distantId.getLength());
		}
		else
		{
			System.arraycopy(distantId.getBytes(), 0, buf, 0, GxsId.LENGTH);
			System.arraycopy(ownId.getBytes(), 0, buf, GxsId.LENGTH, ownId.getLength());
		}
		var digest = new Sha1MessageDigest();
		digest.update(buf);
		return Location.createLocation("GxsTunnelVirtualLocation", new LocationIdentifier(digest.getBytes()));
	}
}
