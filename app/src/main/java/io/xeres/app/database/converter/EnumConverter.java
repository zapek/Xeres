package io.xeres.app.database.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

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
