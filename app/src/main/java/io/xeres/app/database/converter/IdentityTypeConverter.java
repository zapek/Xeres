package io.xeres.app.database.converter;

import io.xeres.common.identity.Type;
import jakarta.persistence.Converter;

@Converter
public class IdentityTypeConverter extends EnumConverter<Type>
{
	@Override
	Class<Type> getEnumClass()
	{
		return Type.class;
	}
}
