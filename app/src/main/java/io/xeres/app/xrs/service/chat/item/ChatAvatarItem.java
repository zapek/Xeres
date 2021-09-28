/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.common.id.Id;

public class ChatAvatarItem extends Item
{
	@RsSerialized
	private byte[] imageData;

	@SuppressWarnings("unused")
	public ChatAvatarItem()
	{
		// Required
	}

	@Override
	public int getPriority()
	{
		return 2;
	}

	public byte[] getImageData()
	{
		return imageData;
	}

	@Override
	public String toString()
	{
		return "ChatAvatarItem{" +
				"imageData=" + Id.toString(imageData) +
				'}';
	}
}
