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

package io.xeres.app.xrs.service.gxs.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;

public abstract class GxsExchange extends Item
{
	@RsSerialized
	private int transactionId;

	private int serviceType;

	@Override
	public int getServiceType()
	{
		return serviceType;
	}

	/**
	 * GxsExchange items are shared between GxsServices. Make sure this is set by whatever creates the item.
	 *
	 * @param serviceType the service type
	 */
	public void setServiceType(int serviceType)
	{
		this.serviceType = serviceType;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.HIGH.getPriority();
	}

	public int getTransactionId()
	{
		return transactionId;
	}

	public void setTransactionId(int transactionId)
	{
		this.transactionId = transactionId;
	}

	@Override
	public String toString()
	{
		return "GxsExchange{" +
				"transactionId=" + transactionId +
				'}';
	}
}
