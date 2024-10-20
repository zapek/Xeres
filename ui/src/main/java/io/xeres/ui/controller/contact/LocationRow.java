/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.contact;

import io.xeres.ui.model.location.Location;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.scene.control.TableRow;

import java.util.regex.Pattern;

class LocationRow extends TableRow<Location>
{
	private static final Pattern RETROSHARE_VERSION_DETECTOR = Pattern.compile("^\\d.*$");

	@Override
	protected void updateItem(Location item, boolean empty)
	{
		super.updateItem(item, empty);
		if (empty)
		{
			TooltipUtils.uninstall(this);
		}
		else
		{
			var sb = new StringBuilder();
			sb.append("Location ID: ");
			sb.append(item.getLocationId().toString());
			if (item.hasVersion())
			{
				sb.append("\nVersion: ");
				// Retroshare only sends the version so we prefix it with its name
				if (RETROSHARE_VERSION_DETECTOR.matcher(item.getVersion()).matches())
				{
					sb.append("Retroshare ");
				}
				sb.append(item.getVersion());
			}
			TooltipUtils.install(this, sb.toString());
		}
	}
}
