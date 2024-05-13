/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.serialization;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemUtils;
import io.xeres.app.xrs.service.RsService;

import java.util.HashMap;
import java.util.Map;

/**
 * This class allows to speed up the case when the size of a serialized item is needed frequently.
 * Only use it with items that cannot have a varying size depending on the item's field values.
 */
public final class SerializerSizeCache
{
	private static final Map<Class<? extends Item>, Integer> cache = new HashMap<>();

	private SerializerSizeCache()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Gets the size of an item once it's serialized.
	 *
	 * @param item the item
	 * @return the size of the item after serialization, header included
	 */
	public static int getItemSize(Item item, RsService service)
	{
		return cache.computeIfAbsent(item.getClass(), aClass -> ItemUtils.getItemSerializedSize(item, service));
	}
}
