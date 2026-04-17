/*
 * Copyright (c) 2023-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.database.model.gxs;

import io.xeres.app.database.model.forum.ForumMessageItemSummary;
import io.xeres.app.xrs.service.forum.item.ForumMessageItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MsgId;
import io.xeres.testutils.IdFakes;
import io.xeres.testutils.StringFakes;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Instant;

public final class ForumMessageItemFakes
{
	private ForumMessageItemFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ForumMessageItem createForumMessageItem()
	{
		return createForumMessageItem(IdFakes.createGxsId(), IdFakes.createMsgId(), RandomStringUtils.insecure().nextAlphabetic(8));
	}

	private static ForumMessageItem createForumMessageItem(GxsId gxsId, MsgId msgId, String name)
	{
		return new ForumMessageItem(gxsId, msgId, name);
	}

	public static ForumMessageItemSummary createForumMessageItemSummary()
	{
		return new ForumMessageItemSummaryFake(IdFakes.createLong(), StringFakes.createNickname(), IdFakes.createGxsId(), IdFakes.createMsgId(), IdFakes.createMsgId(), IdFakes.createMsgId(), IdFakes.createGxsId(), Instant.now(), false);
	}

	public static ForumMessageItemSummary createForumMessageItemSummary(MsgId msgId, GxsId authorGxsId, MsgId parentMsgId)
	{
		return new ForumMessageItemSummaryFake(IdFakes.createLong(), StringFakes.createNickname(), IdFakes.createGxsId(), msgId, IdFakes.createMsgId(), parentMsgId, authorGxsId, Instant.now(), false);
	}
}
