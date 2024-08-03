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

package io.xeres.app.xrs.service.turtle.item;

import io.netty.buffer.ByteBuf;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.app.xrs.serialization.TlvType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Used to do a regexp search for a file.
 */
public class TurtleRegExpSearchRequestItem extends TurtleFileSearchRequestItem implements RsSerializable
{
	private static final int MAX_TOKENS_LIMIT = 256;

	private List<Byte> tokens;

	private List<Integer> ints;

	private List<String> strings;

	@SuppressWarnings("unused")
	public TurtleRegExpSearchRequestItem()
	{
	}

	@Override
	public int getSubType()
	{
		return 9;
	}

	@Override
	public String getKeywords()
	{
		return "[NI]"; // XXX: implement (there are ops and stuff, etc...)
	}

	@Override
	public String toString()
	{
		return "TurtleRegExpSearchRequestItem{" +
				"requestId=" + getRequestId() +
				", depth=" + getDepth() +
				'}';
	}

	@Override
	public TurtleRegExpSearchRequestItem clone()
	{
		return (TurtleRegExpSearchRequestItem) super.clone();
	}

	@Override
	public int writeObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += Serializer.serializeAnnotatedFields(buf, this);

		size += Serializer.serialize(buf, tokens.size());
		size += tokens.stream()
				.mapToInt(value -> Serializer.serialize(buf, value))
				.sum();

		size += Serializer.serialize(buf, ints.size());
		size += ints.stream()
				.mapToInt(value -> Serializer.serialize(buf, value))
				.sum();

		size += Serializer.serialize(buf, strings.size());
		size += strings.stream()
				.mapToInt(value -> Serializer.serialize(buf, TlvType.STR_VALUE, value))
				.sum();

		return size;
	}

	@Override
	public void readObject(ByteBuf buf)
	{
		Serializer.deserializeAnnotatedFields(buf, this);

		var length = validateTokenLimit(Serializer.deserializeInt(buf));
		tokens = new ArrayList<>(length);
		for (int i = 0; i < length; i++)
		{
			tokens.add(Serializer.deserializeByte(buf));
		}

		length = validateTokenLimit(Serializer.deserializeInt(buf));
		ints = new ArrayList<>(length);
		for (int i = 0; i < length; i++)
		{
			ints.add(Serializer.deserializeInt(buf));
		}

		length = validateTokenLimit(Serializer.deserializeInt(buf));
		strings = new ArrayList<>(length);
		for (int i = 0; i < length; i++)
		{
			strings.add((String) Serializer.deserialize(buf, TlvType.STR_VALUE));
		}
	}

	private int validateTokenLimit(int size)
	{
		if (size >= MAX_TOKENS_LIMIT)
		{
			throw new IllegalArgumentException("Maximum search tokens exceeded");
		}
		return size;
	}
}
