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

package io.xeres.app.xrs.service.gxs.item;

import io.xeres.app.database.model.gxs.GxsMessageItem;
import io.xeres.app.xrs.serialization.RsSerialized;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MsgId;

public class GxsSyncMessageItem extends GxsExchange
{
	public static final byte REQUEST = 0x1;
	public static final byte RESPONSE = 0x2;

	@RsSerialized
	private byte flags;

	@RsSerialized
	private GxsId gxsId;

	@RsSerialized
	private MsgId msgId;

	@RsSerialized
	private GxsId authorGxsId;

	@SuppressWarnings("unused")
	public GxsSyncMessageItem()
	{
	}

	public GxsSyncMessageItem(byte flags, GxsMessageItem messageItem, int transactionId)
	{
		this.flags = flags;
		gxsId = messageItem.getGxsId();
		msgId = messageItem.getMsgId();
		authorGxsId = messageItem.getAuthorGxsId();
		setTransactionId(transactionId);
	}

	public GxsSyncMessageItem(byte flags, GxsId gxsId, MsgId msgId, int transactionId)
	{
		this.flags = flags;
		this.gxsId = gxsId;
		this.msgId = msgId;
		setTransactionId(transactionId);
	}

	@Override
	public int getSubType()
	{
		return 8;
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	public MsgId getMsgId()
	{
		return msgId;
	}

	@Override
	public GxsSyncMessageItem clone()
	{
		return (GxsSyncMessageItem) super.clone();
	}

	@Override
	public String toString()
	{
		return "GxsSyncMessageItem{" +
				"flags=" + flags +
				", gxsId=" + gxsId +
				", msgId=" + msgId +
				", authorGxsId=" + authorGxsId +
				'}';
	}
}
