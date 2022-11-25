package io.xeres.app.database.converter;

import io.xeres.common.protocol.NetMode;
import jakarta.persistence.Converter;

@Converter
public class NetModeConverter extends EnumConverter<NetMode>
{
	@Override
	Class<NetMode> getEnumClass()
	{
		return NetMode.class;
	}
}
