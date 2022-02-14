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
import io.xeres.app.xrs.common.Signature;
import io.xeres.app.xrs.common.SignatureSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvType.*;

final class TlvSignatureSetSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvSignatureSetSerializer.class);

	private TlvSignatureSetSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, SignatureSet signatureSet)
	{
		log.trace("Writing TlvSignatureSet");

		var len = getSize(signatureSet);
		buf.ensureWritable(len);
		buf.writeShort(SIGNATURE_SET.getValue());
		buf.writeInt(len);
		signatureSet.getSignatures().forEach((signType, keySignature) -> {
			TlvSerializer.serialize(buf, SIGNATURE_TYPE, signType);
			TlvSerializer.serialize(buf, SIGNATURE, keySignature);
		});

		return len;
	}

	static int getSize(SignatureSet signatureSet)
	{
		return TLV_HEADER_SIZE +
				signatureSet.getSignatures().values().stream().mapToInt(signature -> TlvSerializer.getSize(SIGNATURE_TYPE) + TlvSerializer.getSize(SIGNATURE, signature)).sum();
	}

	static SignatureSet deserialize(ByteBuf buf)
	{
		log.trace("Reading TlvSignatureSet");
		var len = TlvUtils.checkTypeAndLength(buf, SIGNATURE_SET);

		var keySignatureSet = new SignatureSet();
		while (len > 0)
		{
			var type = SignatureSet.Type.findByValue((int) TlvSerializer.deserialize(buf, SIGNATURE_TYPE));
			var keySignature = (Signature) TlvSerializer.deserialize(buf, SIGNATURE);
			keySignatureSet.put(type, keySignature);
			len -= TlvSerializer.getSize(SIGNATURE_TYPE) + TlvSerializer.getSize(SIGNATURE, keySignature);
		}
		return keySignatureSet;
	}
}
