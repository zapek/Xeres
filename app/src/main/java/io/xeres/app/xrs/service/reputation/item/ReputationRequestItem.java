/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.reputation.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.common.protocol.xrs.RsServiceType;

public class ReputationRequestItem extends Item
{
	@RsSerialized
	private int lastUpdate;

	@SuppressWarnings("unused")
	public ReputationRequestItem()
	{
	}

	public ReputationRequestItem(int lastUpdate)
	{
		this.lastUpdate = lastUpdate;
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.GXS_REPUTATION.getType();
	}

	@Override
	public int getSubType()
	{
		return 4;
	}

	public int getLastUpdate()
	{
		return lastUpdate;
	}

	@Override
	public ReputationRequestItem clone()
	{
		return (ReputationRequestItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "ReputationRequestItem{" +
				"lastUpdate=" + lastUpdate +
				'}';
	}
}
