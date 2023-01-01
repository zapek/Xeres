/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.app.net.dht;

import io.xeres.common.id.LocationId;
import io.xeres.common.id.Sha1Sum;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;

import java.nio.charset.StandardCharsets;

final class NodeId
{
	private static final String VERSION = "RS_VERSION_0.5.1\0"; // null terminator is included

	private NodeId()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static byte[] create(LocationId locationId)
	{
		var sha1sum = new byte[Sha1Sum.LENGTH];

		Digest digest = new SHA1Digest();
		var version = VERSION.getBytes(StandardCharsets.US_ASCII);
		digest.update(version, 0, version.length);
		digest.update(locationId.getBytes(), 0, LocationId.LENGTH);
		digest.doFinal(sha1sum, 0);

		return sha1sum;
	}
}
