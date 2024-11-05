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
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import javafx.scene.control.TreeTableCell;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;

import static io.xeres.common.dto.identity.IdentityConstants.NO_IDENTITY_ID;
import static io.xeres.common.dto.profile.ProfileConstants.OWN_PROFILE_ID;
import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;

class ContactCellName extends TreeTableCell<Contact, Contact>
{
	private static final int CONTACT_WIDTH = 32;
	private static final int CONTACT_HEIGHT = 32;

	private final GeneralClient generalClient;
	private final ImageCache imageCache;

	public ContactCellName(GeneralClient generalClient, ImageCache imageCache)
	{
		super();
		this.generalClient = generalClient;
		this.imageCache = imageCache;
	}

	@Override
	protected void updateItem(Contact item, boolean empty)
	{
		super.updateItem(item, empty);
		setText(empty ? null : item.name());
		setGraphic(empty ? null : updateContact((StackPane) getGraphic(), item));
	}

	private StackPane updateContact(StackPane stackPane, Contact contact)
	{
		if (stackPane == null)
		{
			stackPane = new StackPane();
			stackPane.setPrefWidth(CONTACT_WIDTH);
			stackPane.setPrefHeight(CONTACT_HEIGHT);
			stackPane.getChildren().add(new FontIcon(MaterialDesignA.ACCOUNT));
			var finalStackPane = stackPane;
			var imageView = new AsyncImageView(
					url -> generalClient.getImage(url).block(),
					() -> finalStackPane.getChildren().getFirst().setVisible(false),
					imageCache);
			imageView.setFitWidth(CONTACT_WIDTH);
			imageView.setFitHeight(CONTACT_HEIGHT);
			stackPane.getChildren().add(imageView);
		}
		stackPane.getChildren().getFirst().setVisible(true);

		if (contact.identityId() != NO_IDENTITY_ID)
		{
			((AsyncImageView) stackPane.getChildren().get(1)).setUrl(getIdentityImageUrl(contact));
		}
		else
		{
			((AsyncImageView) stackPane.getChildren().get(1)).setUrl(null);
		}
		if (contact.profileId() == OWN_PROFILE_ID)
		{
			setStyle("-fx-font-weight: bold");
		}
		else if (!contact.accepted())
		{
			setStyle("-fx-text-fill: -color-fg-subtle");
		}
		else
		{
			setStyle("");
		}
		return stackPane;
	}

	public static String getIdentityImageUrl(Contact contact)
	{
		return RemoteUtils.getControlUrl() + IDENTITIES_PATH + "/" + contact.identityId() + "/image";
	}
}
