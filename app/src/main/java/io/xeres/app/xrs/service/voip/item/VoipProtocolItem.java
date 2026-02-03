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

package io.xeres.app.xrs.service.voip.item;

import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;

import static io.xeres.app.xrs.service.voip.VoipRsService.FLAGS_AUDIO_DATA;

public class VoipProtocolItem extends Item
{
	public enum Protocol
	{
		/// Not used.
		NONE,

		/// Call/Ring.
		RING,

		/// Pickup/Acknowledge the call.
		ACKNOWLEDGE,

		/// Hangup/Close the call.
		CLOSE,

		/// Ask for the bandwidth.
		BANDWIDTH
	}

	@RsSerialized
	private Protocol protocol;

	@RsSerialized
	private int flags;

	@SuppressWarnings("unused")
	public VoipProtocolItem()
	{
	}

	public VoipProtocolItem(Protocol protocol)
	{
		flags = FLAGS_AUDIO_DATA;
		this.protocol = protocol;
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.VOIP.getType();
	}

	@Override
	public int getSubType()
	{
		return 3;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.REALTIME.getPriority();
	}

	public Protocol getProtocol()
	{
		return protocol;
	}

	@Override
	public VoipProtocolItem clone()
	{
		return (VoipProtocolItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "VoipProtocolItem{" +
				"protocol=" + protocol +
				", flags=" + flags +
				'}';
	}
}
