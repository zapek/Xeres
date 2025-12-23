/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.common.FileSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvType.*;

final class TlvFileSetSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvFileSetSerializer.class);

	private TlvFileSetSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, FileSet fileSet)
	{
		log.trace("Writing TlvFileSet");

		var len = getSize(fileSet);
		buf.ensureWritable(len);
		buf.writeShort(FILE_SET.getValue());
		buf.writeInt(len);
		fileSet.fileItems()
				.forEach(fileItem -> TlvFileItemSerializer.serialize(buf, fileItem));
		if (StringUtils.isNotEmpty(fileSet.title()))
		{
			TlvSerializer.serialize(buf, STR_TITLE, fileSet.title());
		}
		if (StringUtils.isNotEmpty(fileSet.comment()))
		{
			TlvSerializer.serialize(buf, STR_COMMENT, fileSet.comment());
		}
		return len;
	}

	static int getSize(FileSet fileSet)
	{
		return TLV_HEADER_SIZE +
				fileSet.fileItems().stream().mapToInt(TlvFileItemSerializer::getSize).sum() +
				(StringUtils.isEmpty(fileSet.title()) ? 0 : TlvStringSerializer.getSize(fileSet.title())) +
				(StringUtils.isEmpty(fileSet.comment()) ? 0 : TlvStringSerializer.getSize(fileSet.comment()));
	}

	static FileSet deserialize(ByteBuf buf)
	{
		log.trace("Reading TlvFileSet");

		var totalSize = TlvUtils.checkTypeAndLength(buf, FILE_SET);
		var index = buf.readerIndex();

		TlvType tlvType;
		String title = null;
		String comment = null;
		List<FileItem> fileItems = new ArrayList<>();
		while (buf.readerIndex() < index + totalSize && (tlvType = TlvUtils.peekTlvType(buf)) != null)
		{
			switch (tlvType)
			{
				case FILE_ITEM -> fileItems.add(TlvFileItemSerializer.deserialize(buf));
				case STR_TITLE -> title = (String) TlvSerializer.deserialize(buf, STR_TITLE);
				case STR_COMMENT -> comment = (String) TlvSerializer.deserialize(buf, STR_COMMENT);
				default -> TlvUtils.skipTlv(buf);
			}
		}
		return new FileSet(fileItems, title, comment);
	}
}
