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

package io.xeres.app.xrs.service.chat.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.LocationId;

import static io.xeres.app.xrs.serialization.TlvType.STR_MSG;

public class PrivateChatMessageConfigItem extends Item
{
	@RsSerialized
	private LocationId locationId;

	@RsSerialized
	private int chatFlags; // XXX: enumsets

	@RsSerialized
	private int configFlags; // XXX: use an enumSet

	@RsSerialized
	private int sendTime;

	@RsSerialized(tlvType = STR_MSG)
	private String message;

	@RsSerialized
	int receiveTime;

	@Override
	public int getServiceType()
	{
		return RsServiceType.CHAT.getType();
	}

	@Override
	public int getSubType()
	{
		return 5;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.INTERACTIVE.getPriority();
	}

	public LocationId getLocationId()
	{
		return locationId;
	}

	public int getChatFlags()
	{
		return chatFlags;
	}

	public int getConfigFlags()
	{
		return configFlags;
	}

	public int getSendTime()
	{
		return sendTime;
	}

	public String getMessage()
	{
		return message;
	}

	public int getReceiveTime()
	{
		return receiveTime;
	}
}
