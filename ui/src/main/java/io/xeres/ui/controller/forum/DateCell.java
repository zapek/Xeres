/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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

import io.xeres.common.message.forum.ForumMessage;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateCell extends TableCell<ForumMessage, Instant>
{
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
			.withZone(ZoneId.systemDefault());

	public DateCell(TableColumn<ForumMessage, Instant> forumMessageStringTableColumn)
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
			setText(formatter.format(item));
		}
	}
}