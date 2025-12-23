/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.common.*;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.Identifier;
import io.xeres.common.id.MessageId;
import io.xeres.common.id.Sha1Sum;

import java.util.List;
import java.util.Set;

import static io.xeres.app.xrs.serialization.TlvType.SIGNATURE_TYPE;

/**
 * This class if for serializing/deserializing TLVs by:
 * <ul>
 * <li>{@code @RsSerialized} annotations</li>
 * <li>classes outside the {@code serialization} package</li>
 * </ul>
 * For anything else, use the TLV classes directly because they don't require casting of the
 * return types, and they have the {@code getSize()} method.
 */
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
					case STR_NONE, STR_NAME, STR_MSG, STR_LOCATION, STR_VERSION, STR_HASH_SHA1, STR_DYNDNS, STR_DOM_ADDR, STR_GENID, STR_KEY_ID, STR_GROUP_ID, STR_VALUE, STR_DESCR, STR_PATH, STR_LINK, STR_COMMENT, STR_TITLE, STR_GXS_MESSAGE_COMMENT -> TlvStringSerializer.serialize(buf, type, (String) value);
					case INT_AGE, INT_POPULARITY, INT_SIZE, INT_BANDWIDTH -> TlvUint32Serializer.serialize(buf, type, (int) value);
					case LONG_OFFSET -> TlvUint64Serializer.serialize(buf, type, (long) value);
					case ADDRESS -> TlvAddressSerializer.serialize(buf, (PeerAddress) value);
					case ADDRESS_SET -> TlvAddressSerializer.serializeList(buf, (List<PeerAddress>) value);
					case SIGNATURE -> TlvSignatureSerializer.serialize(buf, (Signature) value);
					case SET_PGP_ID -> TlvSetSerializer.serializeLong(buf, type, (Set<Long>) value);
					case SET_HASH, SET_GXS_ID, SET_GXS_MSG_ID -> TlvSetSerializer.serializeIdentifier(buf, type, (Set<? extends Identifier>) value);
					case SET_RECOGN -> TlvStringSetRefSerializer.serialize(buf, type, (List<String>) value);
					case SIGNATURE_SET -> TlvSignatureSetSerializer.serialize(buf, (Set<Signature>) value);
					case SIGNATURE_TYPE -> TlvUint32Serializer.serialize(buf, SIGNATURE_TYPE, (int) value);
					case SECURITY_KEY -> TlvSecurityKeySerializer.serialize(buf, (SecurityKey) value);
					case SECURITY_KEY_SET -> TlvSecurityKeySetSerializer.serialize(buf, (Set<SecurityKey>) value);
					case IMAGE -> TlvImageSerializer.serialize(buf, (byte[]) value);
					case FILE_SET -> TlvFileSetSerializer.serialize(buf, (FileSet) value);
					case FILE_ITEM -> TlvFileItemSerializer.serialize(buf, (FileItem) value);
					case FILE_DATA -> TlvFileDataSerializer.serialize(buf, (FileData) value);
					case SIGN_RSA_SHA1, KEY_EVP_PKEY, STR_SIGN, BIN_IMAGE, BIN_FILE_DATA -> TlvBinarySerializer.serialize(buf, type, (byte[]) value);
					case IPV4, IPV6, ADDRESS_INFO, UNKNOWN -> throw new IllegalArgumentException("Can't use type " + type + " for direct TLV serialization");
				};
	}

	static Object deserialize(ByteBuf buf, TlvType type)
	{
		return switch (type)
				{
					case STR_NONE, STR_NAME, STR_MSG, STR_LOCATION, STR_VERSION, STR_HASH_SHA1, STR_DYNDNS, STR_DOM_ADDR, STR_GENID, STR_KEY_ID, STR_GROUP_ID, STR_VALUE, STR_DESCR, STR_PATH, STR_LINK, STR_COMMENT, STR_TITLE, STR_GXS_MESSAGE_COMMENT -> TlvStringSerializer.deserialize(buf, type);
					case INT_AGE, INT_POPULARITY, INT_SIZE, INT_BANDWIDTH -> TlvUint32Serializer.deserialize(buf, type);
					case LONG_OFFSET -> TlvUint64Serializer.deserialize(buf, type);
					case ADDRESS -> TlvAddressSerializer.deserialize(buf);
					case ADDRESS_SET -> TlvAddressSerializer.deserializeList(buf);
					case SIGNATURE -> TlvSignatureSerializer.deserialize(buf);
					case SET_PGP_ID -> TlvSetSerializer.deserializeLong(buf, type);
					case SET_HASH -> TlvSetSerializer.deserializeIdentifier(buf, type, Sha1Sum.class);
					case SET_GXS_ID -> TlvSetSerializer.deserializeIdentifier(buf, type, GxsId.class);
					case SET_GXS_MSG_ID -> TlvSetSerializer.deserializeIdentifier(buf, type, MessageId.class);
					case SET_RECOGN -> TlvStringSetRefSerializer.deserialize(buf, type);
					case SIGNATURE_SET -> TlvSignatureSetSerializer.deserialize(buf);
					case SIGNATURE_TYPE -> TlvUint32Serializer.deserialize(buf, SIGNATURE_TYPE);
					case SECURITY_KEY -> TlvSecurityKeySerializer.deserialize(buf);
					case SECURITY_KEY_SET -> TlvSecurityKeySetSerializer.deserialize(buf);
					case IMAGE -> TlvImageSerializer.deserialize(buf);
					case FILE_SET -> TlvFileSetSerializer.deserialize(buf);
					case FILE_ITEM -> TlvFileItemSerializer.deserialize(buf);
					case FILE_DATA -> TlvFileDataSerializer.deserialize(buf);
					case SIGN_RSA_SHA1, KEY_EVP_PKEY, STR_SIGN, BIN_IMAGE, BIN_FILE_DATA -> TlvBinarySerializer.deserialize(buf, type);
					case IPV4, IPV6, ADDRESS_INFO, UNKNOWN -> throw new IllegalArgumentException("Can't use type " + type + " for direct TLV deserialization");
				};
	}
}
