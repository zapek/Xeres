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

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;

final class TlvUtils
{
	private TlvUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Checks if the buffer contains the right TlvType and if the length is at least TLV_HEADER_SIZE.
	 *
	 * @param buf     the ByteBuf containing the incoming data
	 * @param tlvType the TlvType to check against
	 * @return the remaining length after TLV_HEADER_SIZE is subtracted
	 */
	static int checkTypeAndLength(ByteBuf buf, TlvType tlvType)
	{
		var readType = buf.readUnsignedShort();
		if (readType != tlvType.getValue())
		{
			throw new IllegalArgumentException("Type " + readType + " does not match " + tlvType);
		}
		var len = buf.readInt();
		if (len < TLV_HEADER_SIZE)
		{
			throw new IllegalArgumentException("Length " + len + " is smaller than the header size (6)");
		}
		return len - TLV_HEADER_SIZE;
	}

	/**
	 * Checks if the buffer contains the right TLV type and if the length is at least TLV_HEADER_SIZE.
	 * This function is needed in addition to the one above because Retroshare abuses some TLVs to store the service type in them.
	 *
	 * @param buf     the ByteBuf containing the incoming data
	 * @param tlvType the TLV type to check against, as an int
	 * @return the remaining length after TLV_HEADER_SIZE is subtracted
	 */
	static int checkTypeAndLength(ByteBuf buf, int tlvType)
	{
		var readType = buf.readUnsignedShort();
		if (readType != tlvType)
		{
			throw new IllegalArgumentException("Type " + readType + " does not match " + tlvType);
		}
		var len = buf.readInt();
		if (len < TLV_HEADER_SIZE)
		{
			throw new IllegalArgumentException("Length " + len + " is smaller than the header size (6)");
		}
		return len - TLV_HEADER_SIZE;
	}
}
