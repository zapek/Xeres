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
import io.xeres.app.database.model.gxs.GxsMetaAndData;
import io.xeres.common.id.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * Class to serialize data types into a format compatible with
 * Retroshare's wire protocol.
 */
public final class Serializer
{
	private static final Logger log = LoggerFactory.getLogger(Serializer.class);

	static public final int TLV_HEADER_SIZE = 6;

	private Serializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Serializes an integer.
	 *
	 * @param buf the buffer
	 * @param value the value to serialize
	 * @return the number of bytes taken to serialize
	 */
	public static int serialize(ByteBuf buf, int value)
	{
		return IntSerializer.serialize(buf, value);
	}

	/**
	 * Deserializes an integer.
	 *
	 * @param buf the buffer
	 * @return the value
	 */
	public static int deserializeInt(ByteBuf buf)
	{
		return IntSerializer.deserialize(buf);
	}

	/**
	 * Serializes a short.
	 *
	 * @param buf the buffer
	 * @param value the value to serialize
	 * @return the number of bytes taken to serialize
	 */
	public static int serialize(ByteBuf buf, short value)
	{
		return ShortSerializer.serialize(buf, value);
	}

	/**
	 * Deserializes a short.
	 *
	 * @param buf the buffer
	 * @return the value
	 */
	public static short deserializeShort(ByteBuf buf)
	{
		return ShortSerializer.deserialize(buf);
	}

	/**
	 * Serializes a byte.
	 *
	 * @param buf the buffer
	 * @param value the value to serialize
	 * @return the number of bytes taken to serialize
	 */
	public static int serialize(ByteBuf buf, byte value)
	{
		return ByteSerializer.serialize(buf, value);
	}

	/**
	 * Deserializes a byte.
	 *
	 * @param buf the buffer
	 * @return the value
	 */
	public static byte deserializeByte(ByteBuf buf)
	{
		return ByteSerializer.deserialize(buf);
	}

	/**
	 * Serializes a long.
	 *
	 * @param buf the buffer
	 * @param value the value to serialize
	 * @return the number of bytes taken to serialize
	 */
	public static int serialize(ByteBuf buf, long value)
	{
		return LongSerializer.serialize(buf, value);
	}

	/**
	 * Deserializes a long.
	 *
	 * @param buf the buffer
	 * @return the value
	 */
	public static long deserializeLong(ByteBuf buf)
	{
		return LongSerializer.deserialize(buf);
	}

	/**
	 * Serializes a float.
	 *
	 * @param buf the buffer
	 * @param value the value to serialize
	 * @return the number of bytes taken to serialize
	 */
	public static int serialize(ByteBuf buf, float value)
	{
		return FloatSerializer.serialize(buf, value);
	}

	/**
	 * Deserializes a float.
	 *
	 * @param buf the buffer
	 * @return the value
	 */
	public static float deserializeFloat(ByteBuf buf)
	{
		return FloatSerializer.deserialize(buf);
	}

	/**
	 * Serializes a double.
	 *
	 * @param buf the buffer
	 * @param value the value to serialize
	 * @return the number of bytes taken to serialize
	 */
	public static int serialize(ByteBuf buf, double value)
	{
		return DoubleSerializer.serialize(buf, value);
	}

	/**
	 * Deserializes a double.
	 *
	 * @param buf the buffer
	 * @return the value
	 */
	public static double deserializeDouble(ByteBuf buf)
	{
		return DoubleSerializer.deserialize(buf);
	}

	/**
	 * Serializes a boolean.
	 *
	 * @param buf  the buffer
	 * @param value the value to serialize
	 * @return the number of bytes taken to serialize
	 */
	public static int serialize(ByteBuf buf, boolean value)
	{
		return BooleanSerializer.serialize(buf, value);
	}

	/**
	 * Deserializes a boolean.
	 *
	 * @param buf the buffer
	 * @return the value
	 */
	public static boolean deserializeBoolean(ByteBuf buf)
	{
		return BooleanSerializer.deserialize(buf);
	}

	/**
	 * Serializes a string.
	 *
	 * @param buf the buffer
	 * @param value   the string
	 * @return the number of bytes taken to serialize
	 */
	public static int serialize(ByteBuf buf, String value)
	{
		return StringSerializer.serialize(buf, value);
	}

	/**
	 * Deserializes a string.
	 *
	 * @param buf the buffer
	 * @return the string
	 */
	public static String deserializeString(ByteBuf buf)
	{
		return StringSerializer.deserialize(buf);
	}

	/**
	 * Serializes an identifier.
	 *
	 * @param buf        the buffer
	 * @param identifier the identifier, can be null
	 * @return the number of bytes taken to serialize
	 */
	public static int serialize(ByteBuf buf, Identifier identifier)
	{
		return IdentifierSerializer.serialize(buf, identifier.getClass(), identifier);
	}

	/**
	 * Serializes an identifier.
	 *
	 * @param buf             the buffer
	 * @param identifier      the identifier, can be null
	 * @param identifierClass the identifier class
	 * @return the number of bytes taken to serialize
	 */
	public static int serialize(ByteBuf buf, Identifier identifier, Class<? extends Identifier> identifierClass)
	{
		return IdentifierSerializer.serialize(buf, identifierClass, identifier);
	}

	/**
	 * Deserializes an identifier.
	 *
	 * @param buf             the buffer
	 * @param identifierClass the class of the identifier
	 * @return the identifier
	 */
	public static Identifier deserializeIdentifier(ByteBuf buf, Class<?> identifierClass)
	{
		return IdentifierSerializer.deserialize(buf, identifierClass);
	}

	/**
	 * Serializes a byte array.
	 *
	 * @param buf the buffer
	 * @param a   the byte array, can be null
	 * @return the number of bytes taken to serialize
	 */
	public static int serialize(ByteBuf buf, byte[] a)
	{
		return ByteArraySerializer.serialize(buf, a);
	}

	/**
	 * Deserializes a byte array.
	 *
	 * @param buf the buffer
	 * @return the byte array
	 */
	public static byte[] deserializeByteArray(ByteBuf buf)
	{
		return ByteArraySerializer.deserialize(buf);
	}

	/**
	 * Serializes a map.
	 *
	 * @param buf the buffer
	 * @param map the map, can be null
	 * @return the number of bytes taken to serialize
	 */
	public static int serialize(ByteBuf buf, Map<Object, Object> map)
	{
		return MapSerializer.serialize(buf, map);
	}

	/**
	 * Deserializes a map.
	 *
	 * @param buf  the buffer
	 * @param type the map key type and the map entry type
	 * @return the map
	 */
	public static Map<Object, Object> deserializeMap(ByteBuf buf, ParameterizedType type)
	{
		return MapSerializer.deserialize(buf, null, type);
	}

	/**
	 * Serializes a list.
	 *
	 * @param buf  the buffer
	 * @param list the list, can be null
	 * @return the number of bytes taken to serialize
	 */
	public static int serialize(ByteBuf buf, List<Object> list)
	{
		return ListSerializer.serialize(buf, list);
	}

	/**
	 * Deserializes a list.
	 *
	 * @param buf  the buffer
	 * @param type the list type
	 * @return the list
	 */
	public static List<Object> deserializeList(ByteBuf buf, ParameterizedType type)
	{
		return ListSerializer.deserialize(buf, null, type);
	}

	/**
	 * Serializes an enum set.
	 *
	 * @param buf       the buffer
	 * @param enumSet   the enum set
	 * @param fieldSize the size of the enum set bitfield
	 * @return the number of bytes taken to serialize
	 */
	public static int serialize(ByteBuf buf, Set<? extends Enum<?>> enumSet, FieldSize fieldSize)
	{
		return EnumSetSerializer.serialize(buf, enumSet, fieldSize);
	}

	/**
	 * Deserializes an enum set.
	 *
	 * @param buf       the buffer
	 * @param e         the enum class
	 * @param fieldSize the size of the enum set bitfield
	 * @return the enum set
	 */
	public static <E extends Enum<E>> Set<E> deserializeEnumSet(ByteBuf buf, Class<E> e, FieldSize fieldSize)
	{
		return EnumSetSerializer.deserialize(buf, e, fieldSize);
	}

	/**
	 * Serializes an enum.
	 *
	 * @param buf the buffer
	 * @param e   the enum
	 * @return the number of bytes taken
	 */
	public static int serialize(ByteBuf buf, Enum<?> e)
	{
		return EnumSerializer.serialize(buf, e);
	}

	/**
	 * Deserializes an enum.
	 *
	 * @param buf the buffer
	 * @param e   the enum class
	 * @return the enum
	 */
	public static <E extends Enum<E>> E deserializeEnum(ByteBuf buf, Class<E> e)
	{
		return EnumSerializer.deserialize(buf, e);
	}

	/**
	 * Serializes a TLV.
	 *
	 * @param buf   the buffer
	 * @param type  the type of the TLV
	 * @param value the value
	 * @return the number of bytes taken
	 */
	public static int serialize(ByteBuf buf, TlvType type, Object value)
	{
		return TlvSerializer.serialize(buf, type, value);
	}

	/**
	 * Deserializes a TLV.
	 *
	 * @param buf  the buffer
	 * @param type the type of the TLV
	 * @return the value
	 */
	public static Object deserialize(ByteBuf buf, TlvType type)
	{
		return TlvSerializer.deserialize(buf, type);
	}

	/**
	 * Serializes a TLV binary with a defined type (needed for GXS)
	 *
	 * @param buf  the buffer
	 * @param type the type (usually abused to be a service)
	 * @param data the byte array
	 * @return the number of bytes taken
	 */
	public static int serializeTlvBinary(ByteBuf buf, int type, byte[] data)
	{
		return TlvBinarySerializer.serialize(buf, type, data);
	}

	/**
	 * Deserializes a TLV binary with a defined type (needed for GXS)
	 *
	 * @param buf  the buffer
	 * @param type the type (usually abused to be a service)
	 * @return the byte array
	 */
	public static byte[] deserializeTlvBinary(ByteBuf buf, int type)
	{
		return TlvBinarySerializer.deserialize(buf, type);
	}

	/**
	 * Serializes all the annotated fields of an object.
	 *
	 * @param buf    the buffer
	 * @param object the object with the annotated fields
	 * @return the number of bytes taken
	 */
	public static int serializeAnnotatedFields(ByteBuf buf, Object object)
	{
		return AnnotationSerializer.serialize(buf, object);
	}

	/**
	 * Deserializes all the annotated fields of an object.
	 *
	 * @param buf    the buffer
	 * @param object the object with the annotated fields
	 * @return true if at least one field was deserialized
	 */
	public static boolean deserializeAnnotatedFields(ByteBuf buf, Object object)
	{
		return AnnotationSerializer.deserialize(buf, object);
	}

	public static int serializeRsSerializable(ByteBuf buf, RsSerializable rsSerializable, Set<SerializationFlags> flags)
	{
		return RsSerializableSerializer.serialize(buf, rsSerializable, flags);
	}

	public static void deserializeRsSerializable(ByteBuf buf, RsSerializable rsSerializable)
	{
		RsSerializableSerializer.deserialize(buf, rsSerializable);
	}

	public static int serializeGxsMetaAndDataItem(ByteBuf buf, GxsMetaAndData gxsMetaAndData, Set<SerializationFlags> flags, GxsMetaAndDataResult result)
	{
		return GxsMetaAndDataSerializer.serialize(buf, gxsMetaAndData, flags, result);
	}

	static int serialize(ByteBuf buf, Field field, Object object)
	{
		return serialize(buf, field.getType(), getField(field, object), field.getAnnotation(RsSerialized.class));
	}

	@SuppressWarnings("unchecked")
	static int serialize(ByteBuf buf, Class<?> javaClass, Object object, RsSerialized annotation)
	{
		var size = 0;

		log.trace("Serializing...");

		if (annotation != null && annotation.tlvType() != TlvType.NONE)
		{
			size += TlvSerializer.serialize(buf, annotation.tlvType(), object);
		}
		else if (Map.class.isAssignableFrom(javaClass))
		{
			size += MapSerializer.serialize(buf, (Map<Object, Object>) object);
		}
		else if (List.class.isAssignableFrom(javaClass))
		{
			size += ListSerializer.serialize(buf, (List<Object>) object);
		}
		else if (EnumSet.class.isAssignableFrom(javaClass) || Set.class.isAssignableFrom(javaClass))
		{
			size += EnumSetSerializer.serialize(buf, (EnumSet<?>) object, annotation);
		}
		else if (Enum.class.isAssignableFrom(javaClass))
		{
			size += EnumSerializer.serialize(buf, (Enum<?>) object);
		}
		else if (javaClass.equals(int.class) || javaClass.equals(Integer.class))
		{
			Objects.requireNonNull(object, "Null integers not supported");
			size += IntSerializer.serialize(buf, (int) object);
		}
		else if (javaClass.equals(short.class) || javaClass.equals(Short.class))
		{
			Objects.requireNonNull(object, "Null shorts not supported");
			size += ShortSerializer.serialize(buf, (short) object);
		}
		else if (javaClass.equals(byte.class) || javaClass.equals(Byte.class))
		{
			Objects.requireNonNull(object, "Null bytes not supported");
			size += ByteSerializer.serialize(buf, (byte) object);
		}
		else if (javaClass.equals(long.class) || javaClass.equals(Long.class))
		{
			Objects.requireNonNull(object, "Null longs not supported");
			size += LongSerializer.serialize(buf, (long) object);
		}
		else if (javaClass.equals(float.class) || javaClass.equals(Float.class))
		{
			Objects.requireNonNull(object, "Null floats not supported");
			size += FloatSerializer.serialize(buf, (float) object);
		}
		else if (javaClass.equals(double.class) || javaClass.equals(Double.class))
		{
			Objects.requireNonNull(object, "Null doubles not supported");
			size += DoubleSerializer.serialize(buf, (double) object);
		}
		else if (javaClass.equals(boolean.class) || javaClass.equals(Boolean.class))
		{
			Objects.requireNonNull(object, "Null booleans not supported");
			size += BooleanSerializer.serialize(buf, (boolean) object);
		}
		else if (javaClass.equals(String.class))
		{
			size += StringSerializer.serialize(buf, (String) object);
		}
		else if (javaClass.isArray())
		{
			size += ArraySerializer.serialize(buf, javaClass, object);
		}
		else if (Identifier.class.isAssignableFrom(javaClass))
		{
			size += IdentifierSerializer.serialize(buf, javaClass, (Identifier) object);
		}
		else if (RsSerializable.class.isAssignableFrom(javaClass))
		{
			size += RsSerializableSerializer.serialize(buf, (RsSerializable) object);
		}
		else
		{
			checkForNonAllowedType(javaClass);
			size += AnnotationSerializer.serialize(buf, object);
		}
		return size;
	}

	static void deserialize(ByteBuf buf, Field field, Object object, RsSerialized annotation)
	{
		setField(field, object, deserialize(buf, field.getType(), field, object, annotation));
	}

	static Object deserialize(ByteBuf buf, Class<?> javaClass)
	{
		return deserialize(buf, javaClass, null, null, null);
	}

	@SuppressWarnings("unchecked")
	private static Object deserialize(ByteBuf buf, Class<?> javaClass, Field field, Object object, RsSerialized annotation)
	{
		if (annotation != null && annotation.tlvType() != TlvType.NONE)
		{
			return TlvSerializer.deserialize(buf, annotation.tlvType());
		}
		else if (javaClass.equals(int.class) || javaClass.equals(Integer.class))
		{
			return IntSerializer.deserialize(buf);
		}
		else if (javaClass.equals(short.class) || javaClass.equals(Short.class))
		{
			return ShortSerializer.deserialize(buf);
		}
		else if (javaClass.equals(byte.class) || javaClass.equals(Byte.class))
		{
			return ByteSerializer.deserialize(buf);
		}
		else if (javaClass.equals(long.class) || javaClass.equals(Long.class))
		{
			return LongSerializer.deserialize(buf);
		}
		else if (javaClass.equals(float.class) || javaClass.equals(Float.class))
		{
			return FloatSerializer.deserialize(buf);
		}
		else if (javaClass.equals(double.class) || javaClass.equals(Double.class))
		{
			return DoubleSerializer.deserialize(buf);
		}
		else if (javaClass.equals(boolean.class) || javaClass.equals(Boolean.class))
		{
			return BooleanSerializer.deserialize(buf);
		}
		else if (javaClass.equals(String.class))
		{
			return StringSerializer.deserialize(buf);
		}
		else if (Identifier.class.isAssignableFrom(javaClass))
		{
			return IdentifierSerializer.deserialize(buf, javaClass);
		}
		else if (RsSerializable.class.isAssignableFrom(javaClass))
		{
			return RsSerializableSerializer.deserialize(buf, javaClass);
		}
		else if (javaClass.isArray())
		{
			return ArraySerializer.deserialize(buf, javaClass);
		}
		else if (Map.class.isAssignableFrom(javaClass))
		{
			return MapSerializer.deserialize(buf, (Map<Object, Object>) getField(field, object), (ParameterizedType) field.getGenericType());
		}
		else if (List.class.isAssignableFrom(javaClass))
		{
			return ListSerializer.deserialize(buf, (List<Object>) getField(field, object), (ParameterizedType) field.getGenericType());
		}
		else if (EnumSet.class.isAssignableFrom(javaClass) || Set.class.isAssignableFrom(javaClass))
		{
			return EnumSetSerializer.deserialize(buf, (ParameterizedType) field.getGenericType(), annotation);
		}
		else if (Enum.class.isAssignableFrom(javaClass))
		{
			return EnumSerializer.deserialize(buf, javaClass);
		}
		else
		{
			checkForNonAllowedType(javaClass);
			return AnnotationSerializer.deserialize(buf, javaClass);
		}
	}

	private static Object getField(Field field, Object object)
	{
		try
		{
			return field.get(object);
		}
		catch (IllegalAccessException e)
		{
			throw new IllegalStateException("Can't access field " + field + ": " + e.getMessage(), e);
		}
	}

	@SuppressWarnings("java:S3011") // Accessibility bypass
	private static void setField(Field field, Object object, Object value)
	{
		try
		{
			field.set(object, value);
		}
		catch (IllegalAccessException e)
		{
			throw new IllegalStateException("Can't set field " + field + ": " + e.getMessage(), e);
		}
	}

	/**
	 * Checks that a class is allowed for serialization. Retroshare is C++ so compound types should be disallowed; but they are used for lists and maps, and we cannot check them here.
	 *
	 * @param javaClass the class to check for support, an IllegalArgumentException is thrown if it is not supported
	 */
	private static void checkForNonAllowedType(Class<?> javaClass)
	{
		if (javaClass.equals(Character.class)
				|| javaClass.equals(char.class))
		{
			throw new IllegalArgumentException("Class " + javaClass.getSimpleName() + " is not allowed for serialization");
		}
	}
}
