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

import io.xeres.common.location.Availability;
import io.xeres.common.rest.contact.Contact;
import javafx.scene.control.TableCell;
import org.kordamp.ikonli.javafx.FontIcon;

public class ContactCellStatus extends TableCell<Contact, Availability>
{
	@Override
	protected void updateItem(Availability item, boolean empty)
	{
		super.updateItem(item, empty);
		setGraphic(empty ? null : updateContact((FontIcon) getGraphic(), item));
	}

	private FontIcon updateContact(FontIcon icon, Availability availability)
	{
		if (icon == null)
		{
			icon = new FontIcon();
		}
		icon.getStyleClass().removeAll("success", "warning", "danger");
		switch (availability)
		{
			case AVAILABLE ->
			{
				icon.setIconLiteral("fas-circle");
				icon.getStyleClass().add("success");
				icon.setVisible(true);
			}
			case AWAY ->
			{
				icon.setIconLiteral("fas-clock");
				icon.getStyleClass().add("warning");
				icon.setVisible(true);
			}
			case BUSY ->
			{
				icon.setIconLiteral("fas-minus-circle");
				icon.getStyleClass().add("danger");
				icon.setVisible(true);
			}
			case OFFLINE -> icon.setVisible(false);
		}
		return icon;
	}
}
