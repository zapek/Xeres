/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.common.FileItem;
import io.xeres.common.id.Sha1Sum;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvType.*;

final class TlvFileItemSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvFileItemSerializer.class);

	private TlvFileItemSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, FileItem fileItem)
	{
		log.trace("Writing TlvFileItem");

		var len = getSize(fileItem);
		buf.ensureWritable(len);
		buf.writeShort(FILE_ITEM.getValue());
		buf.writeInt(len);
		buf.writeLong(fileItem.size());
		Serializer.serialize(buf, fileItem.hash());
		if (StringUtils.isNotEmpty(fileItem.name()))
		{
			TlvSerializer.serialize(buf, STR_NAME, fileItem.name());
		}
		if (StringUtils.isNotEmpty(fileItem.path()))
		{
			TlvSerializer.serialize(buf, STR_PATH, fileItem.path());
		}
		if (fileItem.age() != 0)
		{
			TlvSerializer.serialize(buf, INT_AGE, fileItem.age());
		}
		return len;
	}

	static int getSize(FileItem fileItem)
	{
		return TLV_HEADER_SIZE +
				8 +
				Sha1Sum.LENGTH +
				(StringUtils.isEmpty(fileItem.name()) ? 0 : TlvStringSerializer.getSize(fileItem.name())) +
				(StringUtils.isEmpty(fileItem.path()) ? 0 : TlvStringSerializer.getSize(fileItem.path())) +
				(fileItem.age() == 0 ? 0 : TlvUint32Serializer.getSize());
	}

	static FileItem deserialize(ByteBuf buf)
	{
		log.trace("Reading TlvFileItem");

		var totalSize = TlvUtils.checkTypeAndLength(buf, FILE_ITEM);
		var index = buf.readerIndex();
		var size = Serializer.deserializeLong(buf);
		var hash = (Sha1Sum) Serializer.deserializeIdentifier(buf, Sha1Sum.class);

		TlvType tlvType;
		String name = null;
		String path = null;
		var age = 0;
		while (buf.readerIndex() < index + totalSize && (tlvType = TlvUtils.peekTlvType(buf)) != null)
		{
			switch (tlvType)
			{
				case STR_NAME -> name = (String) TlvSerializer.deserialize(buf, STR_NAME);
				case STR_PATH -> path = (String) TlvSerializer.deserialize(buf, STR_PATH);
				case INT_POPULARITY -> TlvSerializer.deserialize(buf, INT_POPULARITY);
				case INT_AGE -> age = (int) TlvSerializer.deserialize(buf, INT_AGE);
				case INT_SIZE -> TlvSerializer.deserialize(buf, INT_SIZE);
				case SET_HASH -> TlvSerializer.deserialize(buf, SET_HASH);
				default -> TlvUtils.skipTlv(buf);
			}
		}
		return new FileItem(size, hash, name, path, age);
	}
}
