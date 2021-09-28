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

package io.xeres.app.xrs.service.serviceinfo.item;

import io.netty.buffer.ByteBuf;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;

import java.util.Set;

import static io.xeres.app.xrs.serialization.Serializer.*;

public class ServiceInfo implements RsSerializable
{
	private String name;
	private int serviceType;
	private short versionMajor;
	private short versionMinor;
	private short minVersionMajor;
	private short minVersionMinor;

	public ServiceInfo()
	{
	}

	public ServiceInfo(String name, int serviceType, short versionMajor, short versionMinor)
	{
		this.name = name;
		this.serviceType = serviceType;
		this.versionMajor = versionMajor;
		this.versionMinor = versionMinor;
		minVersionMajor = versionMajor;
		minVersionMinor = versionMinor;
	}

	@Override
	public int writeObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += serialize(buf, name);
		size += serialize(buf, serviceType);
		size += serialize(buf, versionMajor);
		size += serialize(buf, versionMinor);
		size += serialize(buf, minVersionMajor);
		size += serialize(buf, minVersionMinor);
		return size;
	}

	@Override
	public void readObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		name = deserializeString(buf);
		serviceType = deserializeInt(buf);
		versionMajor = deserializeShort(buf);
		versionMinor = deserializeShort(buf);
		minVersionMajor = deserializeShort(buf);
		minVersionMinor = deserializeShort(buf);
	}

	public String getName()
	{
		return name;
	}

	public int getServiceType()
	{
		return serviceType;
	}

	public int getType()
	{
		return (serviceType >> 8) & 0xffff;
	}

	public short getVersionMajor()
	{
		return versionMajor;
	}

	public short getVersionMinor()
	{
		return versionMinor;
	}

	public short getMinVersionMajor()
	{
		return minVersionMajor;
	}

	public short getMinVersionMinor()
	{
		return minVersionMinor;
	}

	@Override
	public String toString()
	{
		return "ServiceInfo{" +
				"name='" + name + '\'' +
				", type=" + serviceType +
				", version=" + versionMajor + "." + versionMinor +
				", min=" + minVersionMajor + "." + minVersionMinor +
				'}';
	}
}
