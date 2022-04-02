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
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.xrs.common.SecurityKey;
import io.xeres.app.xrs.common.SecurityKeySet;
import io.xeres.app.xrs.common.Signature;
import io.xeres.app.xrs.common.SignatureSet;
import io.xeres.common.id.GxsId;

import java.util.List;
import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvType.SIGNATURE_TYPE;

final class TlvSerializer
{
	private TlvSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	@SuppressWarnings("unchecked")
	static int serialize(ByteBuf buf, TlvType type, Object value)
	{
		return switch (type)
				{
					case STR_NAME, STR_MSG, STR_LOCATION, STR_VERSION, STR_HASH_SHA1, STR_DYNDNS, STR_DOM_ADDR, STR_GENID, STR_KEY_ID, STR_GROUP_ID -> TlvStringSerializer.serialize(buf, type, (String) value);
					case ADDRESS -> TlvAddressSerializer.serialize(buf, (PeerAddress) value);
					case ADDRESS_SET -> TlvAddressSerializer.serializeList(buf, (List<PeerAddress>) value);
					case SIGNATURE -> TlvSignatureSerializer.serialize(buf, (Signature) value);
					case SET_PGP_ID -> TlvSetSerializer.serializeLong(buf, type, (Set<Long>) value);
					case SET_RECOGN -> TlvStringSetRefSerializer.serialize(buf, type, (List<String>) value);
					case STRING -> TlvStringSerializer.serialize(buf, TlvType.NONE, (String) value);
					case SIGNATURE_SET -> TlvSignatureSetSerializer.serialize(buf, (SignatureSet) value);
					case SIGNATURE_TYPE -> TlvUint32Serializer.serialize(buf, SIGNATURE_TYPE, (Integer) value);
					case SECURITY_KEY -> TlvSecurityKeySerializer.serialize(buf, (SecurityKey) value);
					case SECURITY_KEY_SET -> TlvSecurityKeySetSerializer.serialize(buf, (SecurityKeySet) value);
					case IMAGE -> TlvImageSerializer.serialize(buf, (byte[]) value);
					case SIGN_RSA_SHA1, KEY_EVP_PKEY, STR_SIGN, BIN_IMAGE -> TlvBinarySerializer.serialize(buf, type, (byte[]) value);
					case IPV4, IPV6, ADDRESS_INFO, NONE -> throw new IllegalArgumentException("Can't use type " + type + " for direct TLV serialization");
				};
	}

	static int getSize(TlvType type, Object value)
	{
		return switch (type)
				{
					case SIGN_RSA_SHA1 -> TlvBinarySerializer.getSize((byte[]) value);
					case SIGNATURE -> TlvSignatureSerializer.getSize((Signature) value);
					case SECURITY_KEY -> TlvSecurityKeySerializer.getSize((SecurityKey) value);
					default -> throw new IllegalArgumentException("Not implemented for type " + type);
				};
	}

	static int getSize(TlvType type)
	{
		return switch (type)
				{
					case STR_KEY_ID -> TLV_HEADER_SIZE + GxsId.LENGTH * 2;
					case SIGNATURE_TYPE -> TlvUint32Serializer.getSize();
					default -> throw new IllegalArgumentException("Not implemented for type " + type);
				};
	}

	static Object deserialize(ByteBuf buf, TlvType type)
	{
		return switch (type)
				{
					case STR_NAME, STR_MSG, STR_LOCATION, STR_VERSION, STR_HASH_SHA1, STR_DYNDNS, STR_DOM_ADDR, STR_GENID, STR_KEY_ID, STR_GROUP_ID -> TlvStringSerializer.deserialize(buf, type);
					case ADDRESS -> TlvAddressSerializer.deserialize(buf);
					case ADDRESS_SET -> TlvAddressSerializer.deserializeList(buf);
					case SIGNATURE -> TlvSignatureSerializer.deserialize(buf);
					case SET_PGP_ID -> TlvSetSerializer.deserializeLong(buf, type);
					case SET_RECOGN -> TlvStringSetRefSerializer.deserialize(buf, type);
					case STRING -> TlvStringSerializer.deserialize(buf, TlvType.NONE);
					case SIGNATURE_SET -> TlvSignatureSetSerializer.deserialize(buf);
					case SIGNATURE_TYPE -> TlvUint32Serializer.deserialize(buf, SIGNATURE_TYPE);
					case SECURITY_KEY -> TlvSecurityKeySerializer.deserialize(buf);
					case SECURITY_KEY_SET -> TlvSecurityKeySetSerializer.deserialize(buf);
					case IMAGE -> TlvImageSerializer.deserialize(buf);
					case SIGN_RSA_SHA1, KEY_EVP_PKEY, STR_SIGN, BIN_IMAGE -> TlvBinarySerializer.deserialize(buf, type);
					case IPV4, IPV6, ADDRESS_INFO, NONE -> throw new IllegalArgumentException("Can't use type " + type + " for direct TLV deserialization");
				};
	}
}
