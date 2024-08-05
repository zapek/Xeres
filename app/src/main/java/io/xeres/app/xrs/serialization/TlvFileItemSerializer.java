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
import io.xeres.app.xrs.common.FileItem;
import io.xeres.common.id.Sha1Sum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

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
		// XXX: the following are optional if empty... (length = 0, "" or 0)
		if (!StringUtils.isEmpty(fileItem.name()))
		{
			TlvSerializer.serialize(buf, STR_NAME, fileItem.name());
		}
		if (!StringUtils.isEmpty(fileItem.path()))
		{
			TlvSerializer.serialize(buf, STR_PATH, fileItem.path());
		}
		if (fileItem.popularity() != 0)
		{
			TlvSerializer.serialize(buf, INT_POPULARITY, fileItem.popularity());
		}
		if (fileItem.age() != 0)
		{
			TlvSerializer.serialize(buf, INT_AGE, fileItem.age());
		}
		if (fileItem.pieceSize() != 0)
		{
			TlvSerializer.serialize(buf, INT_SIZE, fileItem.pieceSize());
		}
		if (!CollectionUtils.isEmpty((fileItem.chunkHashes())))
		{
			TlvSerializer.serialize(buf, SET_HASH, fileItem.chunkHashes());
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
				(fileItem.popularity() == 0 ? 0 : TlvUint32Serializer.getSize()) +
				(fileItem.age() == 0 ? 0 : TlvUint32Serializer.getSize()) +
				(fileItem.pieceSize() == 0 ? 0 : TlvUint32Serializer.getSize()) +
				(CollectionUtils.isEmpty(fileItem.chunkHashes()) ? 0 : TlvSetSerializer.getIdentifierSize(fileItem.chunkHashes()));
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
		var popularity = 0;
		var age = 0;
		var pieceSize = 0;
		Set<Sha1Sum> chunkHashes = Set.of();
		while (buf.readerIndex() < index + totalSize && (tlvType = TlvUtils.peekTlvType(buf)) != null) // XXX: how to detect when we are done??
		{
			switch (tlvType)
			{
				case STR_NAME -> name = Serializer.deserializeString(buf);
				case STR_PATH -> path = (String) TlvSerializer.deserialize(buf, STR_PATH);
				case INT_POPULARITY -> popularity = (int) TlvSerializer.deserialize(buf, INT_POPULARITY);
				case INT_AGE -> age = (int) TlvSerializer.deserialize(buf, INT_AGE);
				case INT_SIZE -> pieceSize = (int) TlvSerializer.deserialize(buf, INT_SIZE);
				case SET_HASH -> //noinspection unchecked
						chunkHashes = (Set<Sha1Sum>) TlvSerializer.deserialize(buf, SET_HASH);
				default -> TlvUtils.skipTlv(buf);
			}
		}
		return new FileItem(size, hash, name, path, popularity, age, pieceSize, chunkHashes);
	}
}
