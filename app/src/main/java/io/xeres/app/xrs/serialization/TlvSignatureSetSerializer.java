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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvType.*;

final class TlvSignatureSetSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvSignatureSetSerializer.class);

	private TlvSignatureSetSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, Set<Signature> signatures)
	{
		log.trace("Writing TlvSignatureSet");

		var len = getSize(signatures);
		buf.ensureWritable(len);
		buf.writeShort(SIGNATURE_SET.getValue());
		buf.writeInt(len);
		signatures.stream()
				.sorted()
				.forEach(signature -> {
					TlvSerializer.serialize(buf, SIGNATURE_TYPE, signature.getType().getValue());
					TlvSerializer.serialize(buf, SIGNATURE, signature);
				});

		return len;
	}

	static int getSize(Set<Signature> signatures)
	{
		return TLV_HEADER_SIZE +
				signatures.stream().mapToInt(signature -> TlvUint32Serializer.getSize() + TlvSignatureSerializer.getSize(signature)).sum();
	}

	static Set<Signature> deserialize(ByteBuf buf)
	{
		log.trace("Reading TlvSignatureSet");
		var len = TlvUtils.checkTypeAndLength(buf, SIGNATURE_SET);

		Set<Signature> signatures = HashSet.newHashSet(2);

		while (len > 0)
		{
			var type = Signature.Type.findByValue((int) TlvSerializer.deserialize(buf, SIGNATURE_TYPE));
			var signature = (Signature) TlvSerializer.deserialize(buf, SIGNATURE);
			signature.setType(type);
			signatures.add(signature);
			len -= TlvUint32Serializer.getSize() + TlvSignatureSerializer.getSize(signature);
		}
		return signatures;
	}
}
