/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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

package io.xeres.common.message.voip;

public class VoipMessage
{
	private VoipAction action;

	@SuppressWarnings("unused") // Needed for JSON
	public VoipMessage()
	{
	}

	public VoipMessage(VoipAction action)
	{
		this.action = action;
	}

	public VoipAction getAction()
	{
		return action;
	}

	public void setAction(VoipAction action)
	{
		this.action = action;
	}

	@Override
	public String toString()
	{
		return "VoipMessage{" +
				"action=" + action +
				'}';
	}
}
