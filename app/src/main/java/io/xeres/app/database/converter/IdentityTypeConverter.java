package io.xeres.app.database.converter;

import io.xeres.common.identity.Type;
import io.xeres.ui.model.identity.Identity;

import javax.persistence.Converter;

@Converter
public class IdentityTypeConverter extends EnumConverter<Type>
{
	@Override
	Class<Type> getEnumClass()
	{
		return Type.class;
	}
}
