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

package io.xeres.app.xrs.service.discovery.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.Id;

public class DiscoveryPgpKeyItem extends Item
{
	@RsSerialized
	private long pgpIdentifier;

	@RsSerialized
	private byte[] keyData;

	@SuppressWarnings("unused")
	public DiscoveryPgpKeyItem()
	{
	}

	public DiscoveryPgpKeyItem(long pgpIdentifier, byte[] keyData)
	{
		this.pgpIdentifier = pgpIdentifier;
		this.keyData = keyData;
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.GOSSIP_DISCOVERY.getType();
	}

	@Override
	public int getSubType()
	{
		return 9;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.BACKGROUND.getPriority();
	}

	public long getPgpIdentifier()
	{
		return pgpIdentifier;
	}

	public byte[] getKeyData()
	{
		return keyData;
	}

	@Override
	public DiscoveryPgpKeyItem clone()
	{
		return (DiscoveryPgpKeyItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "DiscoveryPgpKeyItem{" +
				"pgpIdentifier=" + Id.toString(pgpIdentifier) +
				'}';
	}
}
