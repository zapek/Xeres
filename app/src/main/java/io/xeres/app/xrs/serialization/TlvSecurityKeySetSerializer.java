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

package io.xeres.app.xrs.serialization;

import io.netty.buffer.ByteBuf;
import io.xeres.app.xrs.common.SecurityKey;
import io.xeres.app.xrs.common.SecurityKeySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvType.*;

final class TlvSecurityKeySetSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvSecurityKeySetSerializer.class);

	private TlvSecurityKeySetSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, SecurityKeySet securityKeySet)
	{
		log.trace("Writing TlvSecurityKeySet");

		var len = getSize(securityKeySet);
		buf.ensureWritable(len);
		buf.writeShort(SECURITY_KEY_SET.getValue());
		buf.writeInt(len);
		TlvSerializer.serialize(buf, STR_GROUP_ID, securityKeySet.getGroupId());
		securityKeySet.getPublicKeys().forEach((gxsId, securityKey) -> TlvSerializer.serialize(buf, SECURITY_KEY, securityKey));
		securityKeySet.getPrivateKeys().forEach((gxsId, securityKey) -> TlvSerializer.serialize(buf, SECURITY_KEY, securityKey));

		return len;
	}

	static int getSize(SecurityKeySet securityKeySet)
	{
		return TLV_HEADER_SIZE +
				TlvStringSerializer.getSize(securityKeySet.getGroupId()) +
				securityKeySet.getPublicKeys().values().stream().mapToInt(publicKey -> TlvSerializer.getSize(SECURITY_KEY, publicKey)).sum() +
				securityKeySet.getPrivateKeys().values().stream().mapToInt(privateKey -> TlvSerializer.getSize(SECURITY_KEY, privateKey)).sum();
	}

	static SecurityKeySet deserialize(ByteBuf buf)
	{
		log.trace("Reading TlvSecurityKeySet");

		var len = TlvUtils.checkTypeAndLength(buf, SECURITY_KEY_SET);

		// STR_GROUP_ID must be empty
		if (!TlvSerializer.deserialize(buf, STR_GROUP_ID).equals(""))
		{
			throw new IllegalArgumentException("STR_GROUP_ID is not empty");
		}
		len -= TlvStringSerializer.getSize("");

		var securityKeySet = new SecurityKeySet();
		while (len > 0)
		{
			var securityKey = (SecurityKey) TlvSerializer.deserialize(buf, SECURITY_KEY);
			securityKeySet.put(securityKey);
			len -= TlvSerializer.getSize(SECURITY_KEY, securityKey);
		}
		return securityKeySet;
	}
}
