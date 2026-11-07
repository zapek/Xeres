/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

package io.xeres.common.mail;

import io.xeres.common.i18n.I18nEnum;
import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.id.GxsId;

import java.util.ResourceBundle;

public enum MailType implements I18nEnum
{
	INBOX(GxsId.fromString("00000000000000000000000000000001")),
	DRAFT(GxsId.fromString("01000000000000000000000000000001")),
	OUTBOX(GxsId.fromString("02000000000000000000000000000001")),
	SENT(GxsId.fromString("03000000000000000000000000000001")),
	TRASH(GxsId.fromString("04000000000000000000000000000001"));

	private final GxsId gxsId;

	private final ResourceBundle bundle = I18nUtils.getBundle();

	MailType(GxsId gxsId)
	{
		this.gxsId = gxsId;
	}

	public static MailType fromGroupId(long groupId)
	{
		return MailType.values()[(int) groupId];
	}

	public GxsId getGxsId()
	{
		return gxsId;
	}

	@Override
	public String toString()
	{
		return bundle.getString(getMessageKey(this));
	}
}
