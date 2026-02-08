/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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
			var nullIdentifierArray = getNullIdentifierArray(identifierClass);
			buf.ensureWritable(nullIdentifierArray.length);
			buf.writeBytes(nullIdentifierArray);
			return nullIdentifierArray.length;
		}
		else
		{
			buf.ensureWritable(identifier.getLength());
			buf.writeBytes(identifier.getBytes());
			return identifier.getLength();
		}
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

	static Identifier deserializeWithSize(ByteBuf buf, Class<?> identifierClass, int size)
	{
		try
		{
			//noinspection PrimitiveArrayArgumentToVarargsMethod
			var identifier = (Identifier) identifierClass.getDeclaredConstructor(byte[].class).newInstance(ByteArraySerializer.deserialize(buf, size));
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

	private static byte[] getNullIdentifierArray(Class<?> identifierClass)
	{
		// Try finding a static field called "NULL_IDENTIFIER";
		try
		{
			var field = identifierClass.getDeclaredField(Identifier.NULL_FIELD_NAME);
			return (byte[]) field.get(null);
		}
		catch (NoSuchFieldException | IllegalAccessException _)
		{
			// No? Create an identifier instance then a null identifier. This requires
			// more resources but is the only way for identifiers that have a dynamic length.
			log.warn("Using slow path to create a null identifier for {}, consider adding a static field called {} with a null instance in it", identifierClass.getSimpleName(), Identifier.NULL_FIELD_NAME);
			try
			{
				var identifier = (Identifier) identifierClass.getDeclaredConstructor().newInstance();
				return identifier.getNullIdentifier();
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
			{
				throw new IllegalStateException(e.getMessage());
			}
		}
	}
}
