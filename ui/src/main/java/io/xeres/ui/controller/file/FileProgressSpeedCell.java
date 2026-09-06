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

package io.xeres.ui.controller.file;

import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.util.ByteUnitUtils;
import javafx.scene.control.TableCell;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public class FileProgressSpeedCell extends TableCell<FileProgressDisplay, Long>
{
	private static final ResourceBundle bundle = I18nUtils.getBundle();

	@Override
	protected void updateItem(Long value, boolean empty)
	{
		super.updateItem(value, empty);
		setText(empty ? null : computeValue(value));
	}

	private static String computeValue(long value)
	{
		if (value == 0)
		{
			return "";
		}
		else
		{
			return MessageFormat.format(bundle.getString("download-view.cell.speed"), ByteUnitUtils.fromBytes(value));
		}
	}
}
