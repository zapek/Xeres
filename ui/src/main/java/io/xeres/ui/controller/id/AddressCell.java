/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Locale;
import java.util.Objects;

public class AddressCell extends ListCell<AddressCountry>
{
	public AddressCell(ListView<AddressCountry> listView)
	{
		super();
	}

	@Override
	protected void updateItem(AddressCountry item, boolean empty)
	{
		super.updateItem(item, empty);
		setText(empty ? null : item.address());
		setGraphic(empty ? null : getFlag(item.country()));
	}

	private ImageView getFlag(Country country)
	{
		if (country != null)
		{
			var flagPath = getClass().getResourceAsStream("/image/flags/" + country.name().toLowerCase(Locale.ROOT) + ".png");
			if (flagPath != null)
			{
				TooltipUtils.install(this, country::toString, null);
				return new ImageView(new Image(flagPath));
			}
		}
		return new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/image/flags/_unknown.png"))));
	}
}
