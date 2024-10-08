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
import io.xeres.app.util.expression.Expression;
import io.xeres.app.util.expression.ExpressionMapper;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.Serializer;
import io.xeres.app.xrs.serialization.TlvType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Used to do a regexp search for a file.
 */
public class TurtleRegExpSearchRequestItem extends TurtleFileSearchRequestItem implements RsSerializable
{
	private static final int MAX_TOKENS_LIMIT = 256;

	private List<Byte> tokens;

	private List<Integer> ints;

	private List<String> strings;

	private String keywords; // Not serialized
	private List<Expression> expressions; // Not serialized

	@SuppressWarnings("unused")
	public TurtleRegExpSearchRequestItem()
	{
	}

	public TurtleRegExpSearchRequestItem(List<Byte> tokens, List<Integer> ints, List<String> strings)
	{
		this.tokens = tokens;
		this.ints = ints;
		this.strings = strings;
	}

	@Override
	public int getSubType()
	{
		return 9;
	}

	public List<Byte> getTokens()
	{
		return tokens;
	}

	public List<Integer> getInts()
	{
		return ints;
	}

	public List<String> getStrings()
	{
		return strings;
	}

	public List<Expression> getExpressions()
	{
		buildExpressionsIfNeeded();
		return expressions;
	}

	@Override
	public String getKeywords()
	{
		if (keywords == null)
		{
			buildExpressionsIfNeeded();
			keywords = expressions.stream()
					.map(Object::toString)
					.collect(Collectors.joining(" "));
		}
		return keywords;
	}

	private void buildExpressionsIfNeeded()
	{
		if (expressions == null)
		{
			expressions = ExpressionMapper.toExpressions(this);
		}
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
		for (var i = 0; i < length; i++)
		{
			tokens.add(Serializer.deserializeByte(buf));
		}

		length = validateTokenLimit(Serializer.deserializeInt(buf));
		ints = new ArrayList<>(length);
		for (var i = 0; i < length; i++)
		{
			ints.add(Serializer.deserializeInt(buf));
		}

		length = validateTokenLimit(Serializer.deserializeInt(buf));
		strings = new ArrayList<>(length);
		for (var i = 0; i < length; i++)
		{
			strings.add((String) Serializer.deserialize(buf, TlvType.STR_VALUE));
		}
	}

	private static int validateTokenLimit(int size)
	{
		if (size >= MAX_TOKENS_LIMIT)
		{
			throw new IllegalArgumentException("Maximum search tokens exceeded");
		}
		return size;
	}
}
