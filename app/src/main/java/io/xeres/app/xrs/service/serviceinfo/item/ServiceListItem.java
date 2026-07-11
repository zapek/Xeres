/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.RsSerializable;
import io.xeres.app.xrs.serialization.SerializationFlags;
import io.xeres.app.xrs.serialization.TlvSerializer;
import io.xeres.app.xrs.serialization.TlvType;
import io.xeres.common.protocol.xrs.RsServiceType;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServiceListItem extends Item implements RsSerializable
{
	private Map<Integer, ServiceInfo> services = new HashMap<>();

	@SuppressWarnings("unused")
	public ServiceListItem()
	{
	}

	public ServiceListItem(Map<Integer, ServiceInfo> services)
	{
		this.services = services;
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.SERVICE_INFO.getType();
	}

	@Override
	public int getSubType()
	{
		return 1;
	}

	@Override
	public int getPriority()
	{
		return 7;
	}

	public Map<Integer, ServiceInfo> getServices()
	{
		return services;
	}

	@Override
	public int writeObject(ByteBuf buf, Set<SerializationFlags> serializationFlags)
	{
		var size = 0;

		size += TlvSerializer.serializeTlvMap(buf, TlvType.TLV_ONE, TlvType.TLV_ONE, TlvType.TLV_ONE, TlvType.TLV_ONE, services);

		return size;
	}

	@Override
	public void readObject(ByteBuf buf)
	{
		//noinspection unchecked
		services = (Map<Integer, ServiceInfo>) TlvSerializer.deserializeTlvMap(buf, TlvType.TLV_ONE, TlvType.TLV_ONE, TlvType.TLV_ONE, TlvType.TLV_ONE, new ParameterizedType()
		{
			@Override
			public Type @NonNull [] getActualTypeArguments()
			{
				return new Type[]{Integer.class, ServiceInfo.class};
			}

			@Override
			public @NonNull Type getRawType()
			{
				return Map.class;
			}

			@Override
			public Type getOwnerType()
			{
				return null;
			}
		});
	}

	@Override
	public ServiceListItem clone()
	{
		return (ServiceListItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "ServiceListItem{" +
				"map=" + services.values() +
				'}';
	}
}
