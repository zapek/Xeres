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

package io.xeres.common.message.chat;

import io.xeres.common.id.GxsId;

public class ChatRoomTimeoutEvent
{
	private GxsId gxsId;
	private boolean split;

	public ChatRoomTimeoutEvent()
	{
		// Needed for JSON
	}

	public ChatRoomTimeoutEvent(GxsId gxsId, boolean split)
	{
		this.gxsId = gxsId;
		this.split = split;
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	public void setGxsId(GxsId gxsId)
	{
		this.gxsId = gxsId;
	}

	public boolean isSplit()
	{
		return split;
	}

	public void setSplit(boolean split)
	{
		this.split = split;
	}
}
