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

package io.xeres.app.database.model.gxs;

import io.xeres.app.xrs.service.board.item.BoardMessageItem;
import io.xeres.common.id.GxsId;
import io.xeres.common.id.MsgId;
import io.xeres.testutils.IdFakes;
import org.apache.commons.lang3.RandomStringUtils;

public final class BoardMessageItemFakes
{
	private BoardMessageItemFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static BoardMessageItem createBoardMessageItem()
	{
		return createBoardMessageItem(IdFakes.createGxsId(), IdFakes.createMsgId(), RandomStringUtils.insecure().nextAlphabetic(8));
	}

	private static BoardMessageItem createBoardMessageItem(GxsId gxsId, MsgId msgId, String name)
	{
		return new BoardMessageItem(gxsId, msgId, name);
	}
}
