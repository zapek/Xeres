/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.voip.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;

public class VoipPingItem extends Item
{
	@RsSerialized
	private int sequenceNumber;

	@RsSerialized
	private long timestamp;

	@Override
	public int getServiceType()
	{
		return RsServiceType.PLUGIN_VOIP.getType();
	}

	@Override
	public int getSubType()
	{
		return 1;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.REALTIME.getPriority();
	}

	@Override
	public VoipPingItem clone()
	{
		return (VoipPingItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "VoipPingItem{" +
				"sequenceNumber=" + sequenceNumber +
				", timestamp=" + timestamp +
				'}';
	}
}
