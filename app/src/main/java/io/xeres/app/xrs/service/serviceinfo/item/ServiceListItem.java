/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;

import java.util.HashMap;
import java.util.Map;

public class ServiceListItem extends Item
{
	@RsSerialized
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
		return RsServiceType.SERVICEINFO.getType();
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
