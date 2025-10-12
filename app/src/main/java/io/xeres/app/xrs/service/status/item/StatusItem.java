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

package io.xeres.app.xrs.service.status.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.status.ChatStatus;

import java.time.Instant;

public class StatusItem extends Item
{
	@RsSerialized
	private int sendTime;

	@RsSerialized
	private ChatStatus status;

	@SuppressWarnings("unused")
	public StatusItem()
	{
	}

	public StatusItem(ChatStatus status)
	{
		sendTime = (int) Instant.now().getEpochSecond();
		this.status = status;
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.STATUS.getType();
	}

	@Override
	public int getSubType()
	{
		return 1;
	}

	public int getSendTime()
	{
		return sendTime;
	}

	public ChatStatus getStatus()
	{
		return status;
	}

	@Override
	public StatusItem clone()
	{
		return (StatusItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "StatusItem{" +
				"sendTime=" + sendTime +
				", status=" + status +
				'}';
	}
}
