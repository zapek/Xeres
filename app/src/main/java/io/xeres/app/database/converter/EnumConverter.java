package io.xeres.app.database.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * This class is needed because Hibernate uses the ordinal value of enums to save them in the database and
 * some smartass changed enums in H2 to start from 1 instead of 0. Of course this breaks everything.
 */
@Converter
public abstract class EnumConverter<E extends Enum<E>> implements AttributeConverter<E, Integer>
{
	abstract Class<E> getEnumClass();

	@Override
	public Integer convertToDatabaseColumn(E attribute)
	{
		if (attribute == null)
		{
			return null;
		}
		return attribute.ordinal() + 1;
	}

	@Override
	public E convertToEntityAttribute(Integer value)
	{
		if (value == null)
		{
			return null;
		}

		var e = getEnumClass();

		for (var enumConstant : e.getEnumConstants())
		{
			if (value == enumConstant.ordinal() + 1)
			{
				return enumConstant;
			}
		}
		throw new IllegalArgumentException("Ordinal value " + value + " doesn't exist for enum " + e);
	}
}
