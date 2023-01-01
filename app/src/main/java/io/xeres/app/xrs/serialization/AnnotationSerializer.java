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

package io.xeres.app.xrs.serialization;

import io.netty.buffer.ByteBuf;
import io.xeres.app.xrs.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

final class AnnotationSerializer
{
	private static final Logger log = LoggerFactory.getLogger(AnnotationSerializer.class);

	private AnnotationSerializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	static int serialize(ByteBuf buf, Object object)
	{
		var size = 0;

		for (var field : getAllFields(object.getClass(), isClassOrderReversed(object)))
		{
			log.trace("Serializing field {}, of type {}", field.getName(), field.getType().getSimpleName());
			size += Serializer.serialize(buf, field, object);
		}
		return size;
	}

	static Object deserialize(ByteBuf buf, Class<?> javaClass)
	{
		Object instanceObject;
		try
		{
			instanceObject = javaClass.getDeclaredConstructor().newInstance();
		}
		catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
		{
			throw new IllegalArgumentException("Cannot instantiate object of class " + javaClass.getSimpleName());
		}
		if (!deserialize(buf, instanceObject))
		{
			throw new IllegalArgumentException("Cannot deserialize object of class " + javaClass.getSimpleName());
		}
		return instanceObject;
	}

	static boolean deserialize(ByteBuf buf, Object object)
	{
		var allFields = getAllFields(object.getClass(), isClassOrderReversed(object));

		for (var field : allFields)
		{
			log.trace("Deserializing field {}, of type {}", field.getName(), field.getType().getSimpleName());
			Serializer.deserialize(buf, field, object, field.getAnnotation(RsSerialized.class));
		}
		return !allFields.isEmpty();
	}

	/**
	 * Search all fields annotated with @RsSerialized, starting with the
	 * first subclass of Item down to the last subclass.<br>
	 *
	 * @param javaClass the class
	 * @return all fields ordered from superclass to subclass
	 */
	private static List<Field> getAllFields(Class<?> javaClass, boolean reversed)
	{
		if (javaClass == null || javaClass == Item.class)
		{
			return Collections.emptyList();
		}

		List<Field> superFields = new ArrayList<>(getAllFields(javaClass.getSuperclass(), reversed));
		var classFields = Arrays.stream(javaClass.getDeclaredFields())
				.filter(field -> {
					field.setAccessible(true); // NOSONAR
					return field.isAnnotationPresent(RsSerialized.class);
				})
				.collect(Collectors.toCollection(ArrayList::new));

		if (reversed)
		{
			classFields.addAll(superFields);
			return classFields;
		}
		superFields.addAll(classFields);
		return superFields;
	}

	private static boolean isClassOrderReversed(Object object)
	{
		return object.getClass().getDeclaredAnnotation(RsClassSerializedReversed.class) != null;
	}
}
