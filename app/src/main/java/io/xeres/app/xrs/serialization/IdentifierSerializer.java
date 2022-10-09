/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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
import io.xeres.common.id.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;

final class IdentifierSerializer
{
	private static final Logger log = LoggerFactory.getLogger(IdentifierSerializer.class);

	private IdentifierSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, Class<?> identifierClass, Identifier identifier)
	{
		log.trace("Writing identifier: {}", identifier);
		if (identifier == null)
		{
			try
			{
				identifier = (Identifier) identifierClass.getDeclaredConstructor().newInstance();
				buf.ensureWritable(identifier.getLength());
				buf.writeBytes(identifier.getNullIdentifier());
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
			{
				throw new IllegalStateException(e.getMessage());
			}
		}
		else
		{
			buf.ensureWritable(identifier.getLength());
			buf.writeBytes(identifier.getBytes());
		}
		return identifier.getLength();
	}

	static Identifier deserialize(ByteBuf buf, Class<?> identifierClass)
	{
		try
		{
			//noinspection PrimitiveArrayArgumentToVarargsMethod
			var identifier = (Identifier) identifierClass.getDeclaredConstructor(byte[].class).newInstance(ByteArraySerializer.deserialize(buf, getIdentifierLength(identifierClass)));
			if (Arrays.equals(identifier.getNullIdentifier(), identifier.getBytes()))
			{
				return null;
			}
			return identifier;
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
		{
			throw new IllegalStateException(e.getMessage());
		}
	}

	static int getIdentifierLength(Class<?> identifierClass)
	{
		try
		{
			return (int) Arrays.stream(identifierClass.getDeclaredFields())
					.filter(field -> Modifier.isStatic(field.getModifiers()) && field.getName().equals("LENGTH"))
					.findFirst().orElseThrow(() -> new IllegalArgumentException("Missing LENGTH static field in " + identifierClass.getSimpleName()))
					.get(null);
		}
		catch (IllegalAccessException e)
		{
			throw new IllegalStateException(e.getMessage());
		}
	}
}
