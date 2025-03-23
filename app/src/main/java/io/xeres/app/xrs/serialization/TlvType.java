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

public enum TlvType
{
	NONE(0x0), // Used to write strings without TLVs
	INT_SIZE(0x30),
	INT_POPULARITY(0x31),
	INT_AGE(0x32),
	INT_BANDWIDTH(0x35),
	LONG_OFFSET(0x41),
	STR_NAME(0x51),
	STR_PATH(0x52),
	STR_VALUE(0x54),
	STR_MSG(0x57),
	STR_GENID(0x5a),
	STR_LOCATION(0x5c),
	STR_VERSION(0x5f),
	STR_HASH_SHA1(0x70),
	STR_DYNDNS(0x83),
	STR_DOM_ADDR(0x84),
	IPV4(0x85),
	IPV6(0x86),
	STR_GROUP_ID(0xa0),
	STR_KEY_ID(0xa4),
	STR_DESCR(0xb3),
	STR_SIGN(0xb4),
	KEY_EVP_PKEY(0x110),
	SIGN_RSA_SHA1(0x120),
	BIN_IMAGE(0x130),
	BIN_FILE_DATA(0x140),
	FILE_ITEM(0x1000),
	FILE_DATA(0x1002),
	SET_HASH(0x1022),
	SET_PGP_ID(0x1023),
	SET_RECOGN(0x1024),
	SET_GXS_ID(0x1025),
	SET_GXS_MSG_ID(0x1028),
	SECURITY_KEY(0x1040),
	SECURITY_KEY_SET(0x1041),
	SIGNATURE(0x1050),
	SIGNATURE_SET(0x1051),
	SIGNATURE_TYPE(0x1052),
	IMAGE(0x1060),
	ADDRESS_INFO(0x1070),
	ADDRESS_SET(0x1071),
	ADDRESS(0x1072),
	UNKNOWN(0xffff); // Used to signal that an unknown TLV has been found

	private final int value;

	TlvType(int value)
	{
		this.value = value;
	}

	public int getValue()
	{
		return value;
	}

	/**
	 * Gets a TLV from the value.
	 *
	 * @param value the TLV value
	 * @return the TLV or UNKNOWN if the value is not known (including for NONE and UNKNOWN itself)
	 */
	public static TlvType fromValue(int value)
	{
		for (TlvType tlvType : values())
		{
			if (tlvType.getValue() == value && tlvType != NONE)
			{
				return tlvType;
			}

		}
		return UNKNOWN;
	}
}
