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

package io.xeres.app.xrs.service.chat.item;

import io.xeres.app.crypto.hash.chat.ChatChallenge;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.ItemPriority;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.common.id.LocationIdentifier;

public class ChatRoomConnectChallengeItem extends Item
{
	@RsSerialized
	private long challengeCode;

	@SuppressWarnings("unused")
	public ChatRoomConnectChallengeItem()
	{
	}

	public ChatRoomConnectChallengeItem(LocationIdentifier locationIdentifier, long chatRoomId, long messageId)
	{
		challengeCode = ChatChallenge.code(locationIdentifier, chatRoomId, messageId);
	}

	@Override
	public int getServiceType()
	{
		return RsServiceType.CHAT.getType();
	}

	@Override
	public int getSubType()
	{
		return 9;
	}

	@Override
	public int getPriority()
	{
		return ItemPriority.INTERACTIVE.getPriority();
	}

	public long getChallengeCode()
	{
		return challengeCode;
	}

	@Override
	public ChatRoomConnectChallengeItem clone()
	{
		return (ChatRoomConnectChallengeItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "ChatRoomConnectChallengeItem{" +
				"challengeCode=" + Long.toUnsignedString(challengeCode) +
				'}';
	}
}
