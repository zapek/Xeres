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
import io.xeres.app.xrs.common.Signature;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvType.*;

final class TlvSignatureSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvSignatureSerializer.class);

	private TlvSignatureSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, Signature signature)
	{
		log.trace("Writing TlvKeySignature");

		var len = getSize(signature);
		buf.ensureWritable(len);
		buf.writeShort(SIGNATURE.getValue());
		buf.writeInt(len);
		TlvSerializer.serialize(buf, STR_KEY_ID, Id.toString(signature.getGxsId()));
		TlvSerializer.serialize(buf, SIGN_RSA_SHA1, signature.getData());

		return len;
	}

	static int getSize(Signature signature)
	{
		return TLV_HEADER_SIZE +
				(TLV_HEADER_SIZE + GxsId.LENGTH * 2) +
				TlvBinarySerializer.getSize(signature.getData());
	}

	static Signature deserialize(ByteBuf buf)
	{
		log.trace("Reading TlvKeySignature");

		TlvUtils.checkTypeAndLength(buf, SIGNATURE);
		var gxsId = new GxsId(Id.asciiStringToBytes((String) TlvSerializer.deserialize(buf, STR_KEY_ID)));
		var data = (byte[]) TlvSerializer.deserialize(buf, SIGN_RSA_SHA1);
		return new Signature(gxsId, data);
	}
}
