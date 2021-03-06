package io.xeres.app.database.converter;

import io.xeres.common.pgp.Trust;

import javax.persistence.Converter;

@Converter
public class TrustConverter extends EnumConverter<Trust>
{
	@Override
	Class<Trust> getEnumClass()
	{
		return Trust.class;
	}
}
