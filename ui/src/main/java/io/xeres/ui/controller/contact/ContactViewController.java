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

import atlantafx.base.controls.Tile;
import io.xeres.common.rest.contact.Contact;
import io.xeres.ui.client.ContactClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.custom.AsyncImageView;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;

@Component
@FxmlView(value = "/view/contact/contactview.fxml")
public class ContactViewController implements Controller
{
	private static final Logger log = LoggerFactory.getLogger(ContactViewController.class);

	@FXML
	private TableView<Contact> contactTableView;

	@FXML
	private TableColumn<Contact, Contact> contactTableNameColumn;

	@FXML
	private TableColumn<Contact, String> contactTablePresenceColumn;

	@FXML
	private TextField searchTextField;

	@FXML
	private Tile tileView;

	@FXML
	private AsyncImageView contactImageView;

	@FXML
	private Label type;

	private final ContactClient contactClient;
	private final GeneralClient generalClient;

	public ContactViewController(ContactClient contactClient, GeneralClient generalClient)
	{
		this.contactClient = contactClient;
		this.generalClient = generalClient;
	}

	@Override
	public void initialize() throws IOException
	{
		contactTableNameColumn.setCellFactory(param -> new ContactCell(generalClient));
		contactTableNameColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));

		var filteredList = new FilteredList<>(contactTableView.getItems());

		contactClient.getContacts().collectList()
				.doOnSuccess(contacts -> Platform.runLater(() -> {
					// Add all contacts
					contactTableView.getItems().addAll(contacts);

					// Sort by name
					contactTableView.getSortOrder().add(contactTableNameColumn);
					contactTableNameColumn.setSortType(TableColumn.SortType.ASCENDING);
					contactTableNameColumn.setSortable(true);
				}))
				.subscribe();

		searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.isEmpty())
			{
				//noinspection unchecked
				contactTableView.setItems((ObservableList<Contact>) filteredList.getSource());
			}
			else
			{
				if (!(contactTableView.getItems() instanceof FilteredList<?>))
				{
					contactTableView.setItems(filteredList);
				}
				filteredList.setPredicate(s -> s.name().toLowerCase(Locale.ROOT).contains(newValue.toLowerCase(Locale.ROOT)));
			}
		});

		contactTableView.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> changeSelectedContact(newValue));
	}

	private void changeSelectedContact(Contact contact)
	{
		if (contact == null)
		{
			tileView.setTitle(null);
			tileView.setDescription(null);
			contactImageView.setImage(null);
			return;
		}

		log.debug("Set contact to {}", contact);
		tileView.setTitle(contact.name());
		tileView.setDescription(contact.name());
		// XXX: use a default avatar when it's not here! pretty much like in chat... need to use a stackpane or so...
		contactImageView.setUrl(contact.identityId() != 0L ? (IDENTITIES_PATH + "/" + contact.identityId() + "/image") : null, url -> generalClient.getImage(url).block());
		type.setText(contact.identityId() != 0L ? "Contact" : "Profile");
	}
}
