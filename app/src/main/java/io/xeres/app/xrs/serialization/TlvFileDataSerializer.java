/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.common.FileData;
import io.xeres.app.xrs.common.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvType.*;

final class TlvFileDataSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvFileDataSerializer.class);

	private TlvFileDataSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, FileData fileData)
	{
		log.trace("Writing TlvFileData");

		var len = getSize(fileData);
		buf.ensureWritable(len);
		buf.writeShort(FILE_DATA.getValue());
		buf.writeInt(len);
		TlvFileItemSerializer.serialize(buf, fileData.fileItem());
		TlvSerializer.serialize(buf, LONG_OFFSET, fileData.offset());
		TlvBinarySerializer.serialize(buf, BIN_FILE_DATA, fileData.data());
		return len;
	}

	static int getSize(FileData fileData)
	{
		return TLV_HEADER_SIZE +
				TlvFileItemSerializer.getSize(fileData.fileItem()) +
				TlvUint64Serializer.getSize() +
				TlvBinarySerializer.getSize(fileData.data());
	}

	static FileData deserialize(ByteBuf buf)
	{
		log.trace("Reading TlvFileData");

		TlvUtils.checkTypeAndLength(buf, FILE_DATA);

		var fileItem = (FileItem) TlvSerializer.deserialize(buf, FILE_ITEM);
		var offset = (long) TlvSerializer.deserialize(buf, LONG_OFFSET);
		var data = TlvBinarySerializer.deserialize(buf, BIN_FILE_DATA);
		return new FileData(fileItem, offset, data);
	}
}
