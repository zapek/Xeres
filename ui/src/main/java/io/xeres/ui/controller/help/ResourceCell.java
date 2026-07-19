/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.help;

import javafx.scene.control.TreeCell;
import org.springframework.core.io.Resource;

class ResourceCell extends TreeCell<Resource>
{
	@Override
	protected void updateItem(Resource resource, boolean empty)
	{
		super.updateItem(resource, empty);
		if (empty || resource == null)
		{
			setText(null);
		}
		else
		{
			setText(prettify(resource.getFilename()));
		}
	}

	private static String prettify(String fileName)
	{
		if (fileName == null)
		{
			return "???";
		}
		var cutOff = fileName.indexOf(".") == 3 ? 4 : 3;
		return fileName.substring(cutOff, fileName.length() - 3);
	}
}
