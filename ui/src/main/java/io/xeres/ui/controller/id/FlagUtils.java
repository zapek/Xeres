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

package io.xeres.ui.controller.id;

import io.xeres.common.geoip.Country;
import io.xeres.ui.support.util.TooltipUtils;
import javafx.scene.Node;
import javafx.scene.control.Cell;
import javafx.scene.image.Image;

import java.util.Locale;
import java.util.Objects;

public final class FlagUtils
{
	private FlagUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static Image getFlag(Node node, Country country)
	{
		if (country != null)
		{
			var flagPath = FlagUtils.class.getResourceAsStream("/image/flags/" + country.name().toLowerCase(Locale.ROOT) + ".png");
			if (flagPath != null)
			{
				if (node instanceof Cell<?> cell)
				{
					TooltipUtils.install(cell, country::toString, null);
				}
				else
				{
					TooltipUtils.install(node, country.toString());
				}
				return new Image(flagPath);
			}
		}
		return new Image(Objects.requireNonNull(FlagUtils.class.getResourceAsStream("/image/flags/_unknown.png")));
	}
}
