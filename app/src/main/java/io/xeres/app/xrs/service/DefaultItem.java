/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service;

import io.xeres.app.xrs.item.Item;

/**
 * An item that is not part of any service.
 * Is used when there's no service that maps to an item.
 * Will just be disposed by the pipeline.
 */
public class DefaultItem extends Item
{
	@Override
	public int getServiceType()
	{
		return RsServiceType.NONE.getType();
	}

	@Override
	public int getSubType()
	{
		return 0;
	}
}
