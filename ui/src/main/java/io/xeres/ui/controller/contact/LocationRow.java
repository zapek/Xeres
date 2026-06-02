/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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

import io.xeres.common.i18n.I18nUtils;
import io.xeres.ui.model.connection.Connection;
import io.xeres.ui.model.location.Location;
import io.xeres.ui.support.util.DateUtils;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.scene.control.TableRow;

import java.util.Comparator;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

class LocationRow extends TableRow<Location>
{
	private static final Pattern RETROSHARE_VERSION_DETECTOR = Pattern.compile("^\\d.*$");

	private static final ResourceBundle bundle = I18nUtils.getBundle();

	@Override
	protected void updateItem(Location location, boolean empty)
	{
		super.updateItem(location, empty);
		if (empty)
		{
			TooltipUtils.uninstall(this);
		}
		else
		{
			var sb = new StringBuilder();
			sb.append(bundle.getString("contact-view.information.location.id"));
			sb.append(" ");
			sb.append(location.getLocationIdentifier().toString());
			if (location.hasVersion())
			{
				sb.append("\n");
				sb.append(bundle.getString("contact-view.information.location.version"));
				sb.append(" ");
				// Retroshare only sends the version, so we prefix it with its name
				if (RETROSHARE_VERSION_DETECTOR.matcher(location.getVersion()).matches())
				{
					sb.append("Retroshare ");
				}
				sb.append(location.getVersion());
			}
			if (location.hasConnections())
			{
				sb.append("\n");
				sb.append(bundle.getString("contact-view.information.location.connections"));
				sb.append("\n");
				location.getConnections().stream()
						.sorted(Comparator.comparing(Connection::getLastConnected, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
						.limit(16)
						.forEach(connection -> {
							sb.append(" ");
							sb.append(connection.getAddress());
							sb.append(" ");
							sb.append(DateUtils.formatDateTime(connection.getLastConnected(), ""));
							sb.append("\n");
						});
			}
			TooltipUtils.install(this, sb.toString());
		}
	}
}
