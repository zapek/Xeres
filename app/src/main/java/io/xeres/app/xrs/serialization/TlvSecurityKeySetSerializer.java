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

package io.xeres.app.xrs.serialization;

import io.netty.buffer.ByteBuf;
import io.xeres.app.xrs.common.SecurityKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvType.*;

final class TlvSecurityKeySetSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvSecurityKeySetSerializer.class);
	private static final String GROUP_ID_VALUE = ""; // unused

	private TlvSecurityKeySetSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, Set<SecurityKey> securityKeys)
	{
		log.trace("Writing TlvSecurityKeySet");

		var len = getSize(securityKeys);
		buf.ensureWritable(len);
		buf.writeShort(SECURITY_KEY_SET.getValue());
		buf.writeInt(len);
		TlvSerializer.serialize(buf, STR_GROUP_ID, GROUP_ID_VALUE);
		securityKeys.stream()
				.sorted()
				.forEach(securityKey -> TlvSerializer.serialize(buf, SECURITY_KEY, securityKey));

		return len;
	}

	static int getSize(Set<SecurityKey> securityKeys)
	{
		return TLV_HEADER_SIZE +
				TlvStringSerializer.getSize(GROUP_ID_VALUE) +
				securityKeys.stream().mapToInt(key -> TlvSerializer.getSize(SECURITY_KEY, key)).sum();
	}

	static Set<SecurityKey> deserialize(ByteBuf buf)
	{
		log.trace("Reading TlvSecurityKeySet");

		var len = TlvUtils.checkTypeAndLength(buf, SECURITY_KEY_SET);

		// STR_GROUP_ID must be empty
		if (!TlvSerializer.deserialize(buf, STR_GROUP_ID).equals(GROUP_ID_VALUE))
		{
			throw new IllegalArgumentException("STR_GROUP_ID is not empty");
		}
		len -= TlvStringSerializer.getSize("");

		Set<SecurityKey> securityKeys = HashSet.newHashSet(2);
		while (len > 0)
		{
			var securityKey = (SecurityKey) TlvSerializer.deserialize(buf, SECURITY_KEY);
			securityKeys.add(securityKey);
			len -= TlvSerializer.getSize(SECURITY_KEY, securityKey);
		}
		return securityKeys;
	}
}
