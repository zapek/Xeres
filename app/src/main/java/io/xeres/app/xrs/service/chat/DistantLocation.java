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

package io.xeres.app.xrs.service.chat;

import java.util.ArrayList;
import java.util.List;

import io.xeres.app.database.model.location.Location;
import io.xeres.common.id.GxsId;

class DistantLocation
{
	private final Location tunnelId;
	private final GxsId gxsId;
	private final List<String> messageList;

	public DistantLocation(Location tunnelId, GxsId gxsId)
	{
		this.tunnelId = tunnelId;
		this.gxsId = gxsId;
		messageList = new ArrayList<>();
	}

	public Location getTunnelId()
	{
		return tunnelId;
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	public boolean hasMessages()
	{
		return !messageList.isEmpty();
	}

	public void addMessage(String message)
	{
		messageList.add(message);
	}

	public String getAllMessages()
	{
		return String.join("", messageList);
	}

	public void clearMessages()
	{
		messageList.clear();
	}

	@Override
	public String toString()
	{
		return "DistantLocation{" +
				"tunnelId=" + tunnelId +
				", gxsId=" + gxsId +
				'}';
	}
}
