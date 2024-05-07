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
		TlvSerializer.serialize(buf, STR_NAME, fileItem.name());
		TlvSerializer.serialize(buf, STR_PATH, fileItem.path());
		TlvSerializer.serialize(buf, INT_POPULARITY, fileItem.popularity());
		TlvSerializer.serialize(buf, INT_AGE, fileItem.age());
		TlvSerializer.serialize(buf, INT_SIZE, fileItem.pieceSize());
		TlvSerializer.serialize(buf, SET_HASH, fileItem.chunkHashes());
		return len;
	}

	static int getSize(FileItem fileItem)
	{
		return TLV_HEADER_SIZE +
				8 +
				Sha1Sum.LENGTH +
				TlvSerializer.getSize(STR_NAME, fileItem.name()) +
				TlvSerializer.getSize(STR_PATH, fileItem.path()) +
				TlvSerializer.getSize(TlvType.INT_POPULARITY, fileItem.popularity()) +
				TlvSerializer.getSize(TlvType.INT_AGE, fileItem.age()) +
				TlvSerializer.getSize(TlvType.INT_SIZE, fileItem.pieceSize()) +
				TlvSerializer.getSize(TlvType.SET_HASH, fileItem.chunkHashes());
	}

	static FileItem deserialize(ByteBuf buf)
	{
		log.trace("Reading TlvFileItem");

		TlvUtils.checkTypeAndLength(buf, FILE_ITEM);
		var size = Serializer.deserializeLong(buf);
		var hash = (Sha1Sum) Serializer.deserializeIdentifier(buf, Sha1Sum.class);
		var name = (String) TlvSerializer.deserialize(buf, STR_NAME);
		var path = (String) TlvSerializer.deserialize(buf, STR_PATH);
		var popularity = (int) TlvSerializer.deserialize(buf, INT_POPULARITY);
		var age = (int) TlvSerializer.deserialize(buf, INT_AGE);
		var pieceSize = (int) TlvSerializer.deserialize(buf, INT_SIZE);
		@SuppressWarnings("unchecked") var chunkHashes = (Set<Sha1Sum>) TlvSerializer.deserialize(buf, TlvType.SET_HASH);
		return new FileItem(size, hash, name, path, popularity, age, pieceSize, chunkHashes);
	}
}
