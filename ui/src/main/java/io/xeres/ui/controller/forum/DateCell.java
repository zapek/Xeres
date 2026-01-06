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

package io.xeres.ui.controller.forum;

import io.xeres.ui.model.forum.ForumMessage;
import javafx.scene.control.TreeTableCell;

import java.time.Instant;

import static io.xeres.ui.support.util.DateUtils.DATE_TIME_FORMAT;

class DateCell extends TreeTableCell<ForumMessage, Instant>
{
	public DateCell()
	{
		super();
	}

	@Override
	protected void updateItem(Instant item, boolean empty)
	{
		super.updateItem(item, empty);
		if (empty)
		{
			setText(null);
		}
		else
		{
			setText(DATE_TIME_FORMAT.format(item));
		}
	}
}
