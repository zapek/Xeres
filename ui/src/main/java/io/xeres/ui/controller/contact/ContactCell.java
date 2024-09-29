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

import io.xeres.common.rest.contact.Contact;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.AsyncImageView;
import javafx.scene.control.TableCell;
import javafx.scene.image.ImageView;

import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;

public class ContactCell extends TableCell<Contact, Contact>
{
	private final GeneralClient generalClient;

	public ContactCell(GeneralClient generalClient)
	{
		super();
		this.generalClient = generalClient;
	}

	@Override
	protected void updateItem(Contact item, boolean empty)
	{
		super.updateItem(item, empty);
		setText(empty ? null : item.name());
		setGraphic(empty ? null : updateContactImage((AsyncImageView) getGraphic(), item));
	}

	private ImageView updateContactImage(AsyncImageView imageView, Contact contact)
	{
		if (imageView == null)
		{
			imageView = new AsyncImageView();
			imageView.setFitWidth(32);
			imageView.setFitHeight(32);
		}
		imageView.setUrl(contact.identityId() != 0 ? (IDENTITIES_PATH + "/" + contact.identityId() + "/image") : null, url -> generalClient.getImage(url).block());
		return imageView;
	}
}
