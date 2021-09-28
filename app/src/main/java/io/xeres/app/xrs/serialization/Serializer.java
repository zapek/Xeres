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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to serialize data types into a format compatible with
 * Retroshare's wire protocol.
 */
public final class Serializer
{
	private static final Logger log = LoggerFactory.getLogger(Serializer.class);

	static final int TLV_HEADER_SIZE = 6;

	private Serializer()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Serializes an integer.
	 *
	 * @param buf the buffer
	 * @param i   the integer
	 * @return the number of bytes taken
	 */
	public static int serialize(ByteBuf buf, Integer i)
	{
		return IntSerializer.serialize(buf, i);
	}

	/**
	 * Deserializes an integer.
	 *
	 * @param buf the buffer
	 * @return the integer
	 */
	public static int deserializeInt(ByteBuf buf)
	{
		return IntSerializer.deserialize(buf);
	}

	/**
	 * Serializes a short.
	 *
	 * @param buf the buffer
	 * @param sh  the short
	 * @return the number of bytes taken
	 */
	public static int serialize(ByteBuf buf, Short sh)
	{
		return ShortSerializer.serialize(buf, sh);
	}

	/**
	 * Deserializes a short.
	 *
	 * @param buf the buffer
	 * @return the short
	 */
	public static short deserializeShort(ByteBuf buf)
	{
		return ShortSerializer.deserialize(buf);
	}

	/**
	 * Serializes a byte.
	 *
	 * @param buf the buffer
	 * @param b   the byte
	 * @return the number of bytes taken
	 */
	public static int serialize(ByteBuf buf, Byte b)
	{
		return ByteSerializer.serialize(buf, b);
	}

	/**
	 * Deserializes a byte.
	 *
	 * @param buf the buffer
	 * @return the byte
	 */
	public static byte deserializeByte(ByteBuf buf)
	{
		return ByteSerializer.deserialize(buf);
	}

	/**
	 * Serializes a long.
	 *
	 * @param buf the buffer
	 * @param l   the long
	 * @return the number of bytes taken
	 */
	public static int serialize(ByteBuf buf, Long l)
	{
		return LongSerializer.serialize(buf, l);
	}

	/**
	 * Deserializes a long.
	 *
	 * @param buf the buffer
	 * @return the long
	 */
	public static long deserializeLong(ByteBuf buf)
	{
		return LongSerializer.deserialize(buf);
	}

	/**
	 * Serializes a float.
	 *
	 * @param buf the buffer
	 * @param f   the float
	 * @return the number of bytes taken
	 */
	public static int serialize(ByteBuf buf, Float f)
	{
		return FloatSerializer.serialize(buf, f);
	}

	/**
	 * Deserializes a float.
	 *
	 * @param buf the buffer
	 * @return the float
	 */
	public static float deserializeFloat(ByteBuf buf)
	{
		return FloatSerializer.deserialize(buf);
	}

	/**
	 * Serializes a double.
	 *
	 * @param buf the buffer
	 * @param d   the double
	 * @return the number of bytes taken
	 */
	public static int serialize(ByteBuf buf, Double d)
	{
		return DoubleSerializer.serialize(buf, d);
	}

	/**
	 * Deserializes a double.
	 *
	 * @param buf the buffer
	 * @return the double
	 */
	public static double deserializeDouble(ByteBuf buf)
	{
		return DoubleSerializer.deserialize(buf);
	}

	/**
	 * Serializes a boolean.
	 *
	 * @param buf  the buffer
	 * @param bool the boolean
	 * @return the number of bytes taken
	 */
	public static int serialize(ByteBuf buf, Boolean bool)
	{
		return BooleanSerializer.serialize(buf, bool);
	}

	/**
	 * Deserializes a boolean.
	 *
	 * @param buf the buffer
	 * @return the boolean
	 */
	public static boolean deserializeBoolean(ByteBuf buf)
	{
		return BooleanSerializer.deserialize(buf);
	}

	/**
	 * Serializes a string.
	 *
	 * @param buf the buffer
	 * @param s   the string
	 * @return the number of bytes taken
	 */
	public static int serialize(ByteBuf buf, String s)
	{
		return StringSerializer.serialize(buf, s);
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
	 * @param identifier the identifier
	 * @return the number of bytes taken
	 */
	public static int serialize(ByteBuf buf, Identifier identifier)
	{
		return IdentifierSerializer.serialize(buf, identifier.getClass(), identifier);
	}

	// XXX: ponder about removing the one above as this one handles null identifiers
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
	 * @param a   the byte array
	 * @return the number of bytes taken
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
	 * @param map the map
	 * @return the number of bytes taken
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
	 * @param list the list
	 * @return the number of bytes taken
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
	 * @return the number of bytes taken
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
		return TlvBinarySerializer.serializer(buf, type, data);
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

	static int serialize(ByteBuf buf, Field field, Object object, RsSerialized annotation)
	{
		return serialize(buf, field.getType(), getField(field, object), annotation);
	}

	@SuppressWarnings("unchecked")
	static int serialize(ByteBuf buf, Class<?> javaClass, Object object, RsSerialized annotation)
	{
		var size = 0;

		log.trace("Serializing...");
		// XXX: don't forget to handle null values! ie. object can be null (and this can't be done for primitives...)

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
			size += IntSerializer.serialize(buf, (Integer) object);
		}
		else if (javaClass.equals(short.class) || javaClass.equals(Short.class))
		{
			size += ShortSerializer.serialize(buf, (Short) object);
		}
		else if (javaClass.equals(byte.class) || javaClass.equals(Byte.class))
		{
			size += ByteSerializer.serialize(buf, (Byte) object);
		}
		else if (javaClass.equals(long.class) || javaClass.equals(Long.class))
		{
			size += LongSerializer.serialize(buf, (Long) object);
		}
		else if (javaClass.equals(float.class) || javaClass.equals(Float.class))
		{
			size += FloatSerializer.serialize(buf, (Float) object);
		}
		else if (javaClass.equals(double.class) || javaClass.equals(Double.class))
		{
			size += DoubleSerializer.serialize(buf, (Double) object);
		}
		else if (javaClass.equals(boolean.class) || javaClass.equals(Boolean.class))
		{
			size += BooleanSerializer.serialize(buf, (Boolean) object);
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
	 * Checks that a class is allowed for serialization. Retroshare is C++ so compound types should be disallowed
	 * but they are used for lists and maps and we cannot check them here.
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
