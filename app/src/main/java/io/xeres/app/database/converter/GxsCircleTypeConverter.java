package io.xeres.app.database.converter;

import io.xeres.app.database.model.gxs.GxsCircleType;
import jakarta.persistence.Converter;

@Converter
public class GxsCircleTypeConverter extends EnumConverter<GxsCircleType>
{
	@Override
	Class<GxsCircleType> getEnumClass()
	{
		return GxsCircleType.class;
	}
}
