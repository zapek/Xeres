/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.common.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.xeres.app.xrs.serialization.Serializer.TLV_HEADER_SIZE;
import static io.xeres.app.xrs.serialization.TlvType.BIN_IMAGE;
import static io.xeres.app.xrs.serialization.TlvType.IMAGE;

final class TlvImageSerializer
{
	private static final Logger log = LoggerFactory.getLogger(TlvImageSerializer.class);

	private TlvImageSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, Image image)
	{
		log.trace("Writing image {}", image.getType());

		var len = getSize(image);
		buf.ensureWritable(len);
		buf.writeShort(IMAGE.getValue());
		buf.writeInt(len);
		Serializer.serialize(buf, image.getType());
		TlvSerializer.serialize(buf, BIN_IMAGE, image.getData());

		return len;
	}

	static int getSize(Image image)
	{
		return TLV_HEADER_SIZE + 4 + TlvBinarySerializer.getSize(image.getData());
	}

	static Image deserialize(ByteBuf buf)
	{
		log.trace("Reading image");

		TlvUtils.checkTypeAndLength(buf, IMAGE);
		var type = Serializer.deserializeEnum(buf, Image.Type.class);
		var data = (byte[]) TlvSerializer.deserialize(buf, BIN_IMAGE);
		return new Image(type, data);
	}
}
