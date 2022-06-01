package io.xeres.app.database.converter;

import io.xeres.app.net.protocol.PeerAddress;

import javax.persistence.Converter;

@Converter
public class PeerAddressTypeConverter extends EnumConverter<PeerAddress.Type>
{
	@Override
	Class<PeerAddress.Type> getEnumClass()
	{
		return PeerAddress.Type.class;
	}
}
