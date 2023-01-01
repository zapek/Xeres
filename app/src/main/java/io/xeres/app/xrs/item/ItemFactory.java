/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.item;

import io.xeres.app.xrs.service.RsServiceRegistry;

import java.lang.reflect.InvocationTargetException;

public final class ItemFactory
{
	private ItemFactory()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static Item create(int type, int subType)
	{
		var service = RsServiceRegistry.getServiceFromType(type);
		if (service != null)
		{
			try
			{
				var item = service.createItem(subType);
				item.setService(service);
				return item;
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
			{
				throw new IllegalArgumentException("Couldn't create item in service " + service + " : " + e.getMessage());
			}
		}
		else
		{
			throw new IllegalArgumentException("Couldn't create item for service " + type + ": no service found");
		}
	}
}
