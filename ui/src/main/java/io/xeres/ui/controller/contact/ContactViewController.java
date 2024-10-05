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

import io.xeres.common.id.Id;
import io.xeres.common.protocol.HostPort;
import io.xeres.common.rest.contact.Contact;
import io.xeres.ui.client.ContactClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.IdentityClient;
import io.xeres.ui.client.ProfileClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.custom.AsyncImageView;
import io.xeres.ui.model.location.Location;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;

@Component
@FxmlView(value = "/view/contact/contactview.fxml")
public class ContactViewController implements Controller
{
	private static final Logger log = LoggerFactory.getLogger(ContactViewController.class);

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") // XXX: put that in some utility class. has to be used everywhere
			.withZone(ZoneId.systemDefault());

	@FXML
	private TableView<Contact> contactTableView;

	@FXML
	private TableColumn<Contact, Contact> contactTableNameColumn;

	@FXML
	private TableColumn<Contact, String> contactTablePresenceColumn;

	@FXML
	private TextField searchTextField;

	@FXML
	private StackPane contactImagePane;

	@FXML
	private AsyncImageView contactImageView;

	@FXML
	private Label nameLabel;

	@FXML
	private Label idLabel;

	@FXML
	private Label typeLabel;

	@FXML
	private Label updatedLabel;

	@FXML
	private FontIcon contactIcon;

	@FXML
	private Label acceptedLabel;

	@FXML
	private Label trustLabel;

	@FXML
	private GridPane profilePane;

	@FXML
	private TableView<Location> locationTableView;

	@FXML
	private TableColumn<Location, String> locationTableNameColumn;

	@FXML
	private TableColumn<Location, String> locationTableIPColumn;

	@FXML
	private TableColumn<Location, String> locationTablePortColumn;

	@FXML
	private TableColumn<Location, String> locationTableLastConnectedColumn;

	private final ContactClient contactClient;
	private final GeneralClient generalClient;
	private final ProfileClient profileClient;
	private final IdentityClient identityClient;

	public ContactViewController(ContactClient contactClient, GeneralClient generalClient, ProfileClient profileClient, IdentityClient identityClient)
	{
		this.contactClient = contactClient;
		this.generalClient = generalClient;
		this.profileClient = profileClient;
		this.identityClient = identityClient;
	}

	@Override
	public void initialize() throws IOException
	{
		contactImageView.setLoader(url -> generalClient.getImage(url).block());
		contactImageView.setOnFailure(() -> contactImagePane.getChildren().getFirst().setVisible(true));

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

		locationTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

		locationTableNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		locationTableIPColumn.setCellValueFactory(param -> {
			var hostPort = getConnectedAddress(param.getValue());
			return new SimpleStringProperty(hostPort != null ? hostPort.host() : "-");
		});
		locationTablePortColumn.setCellValueFactory(param -> {
			var hostPort = getConnectedAddress(param.getValue());
			return new SimpleStringProperty(hostPort != null ? String.valueOf(hostPort.port()) : "-");
		});
		locationTableLastConnectedColumn.setCellValueFactory(param -> new SimpleStringProperty(getLastConnection(param.getValue())));

		// Workaround for https://github.com/mkpaz/atlantafx/issues/31
		contactIcon.iconSizeProperty()
				.addListener((observable, oldValue, newValue) -> contactIcon.setIconSize(128));
	}

	private HostPort getConnectedAddress(Location location)
	{
		if (location.isConnected() && !location.getConnections().isEmpty())
		{
			var connection = location.getConnections().getFirst();
			return HostPort.parse(connection.getAddress());
		}
		return null;
	}

	private String getLastConnection(Location location)
	{
		if (location.isConnected())
		{
			return "Now";
		}
		else
		{
			var lastConnected = location.getLastConnected();
			if (lastConnected == null)
			{
				return "Never";
			}
			else
			{
				return DATE_TIME_FORMATTER.format(lastConnected);
			}
		}
	}

	private void changeSelectedContact(Contact contact)
	{
		if (contact == null)
		{
			nameLabel.setText(null);
			idLabel.setText(null);
			typeLabel.setText(null);
			updatedLabel.setText(null);
			contactImageView.setImage(null);
			contactImagePane.getChildren().getFirst().setVisible(true);
			profilePane.setVisible(false);
			hideTableLocations();
			return;
		}

		log.debug("Set contact to {}", contact);
		nameLabel.setText(contact.name());
		if (contact.profileId() != 0L)
		{
			profileClient.findById(contact.profileId())
					.doOnSuccess(profile -> Platform.runLater(() -> {
						idLabel.setText(Id.toString(profile.getPgpIdentifier()));
						updatedLabel.setText(null); // XXX: for now...
						acceptedLabel.setText(profile.isAccepted() ? "yes" : "no");
						trustLabel.setText(profile.getTrust().toString());
						profilePane.setVisible(true);
						showTableLocations(profile.getLocations());
					}))
					.subscribe();

			contactImagePane.getChildren().getFirst().setVisible(true);
			contactImageView.setUrl(null);
			typeLabel.setText("Profile");
		}
		else if (contact.identityId() != 0L)
		{
			profilePane.setVisible(false);
			identityClient.findById(contact.identityId())
					.doOnSuccess(identity -> Platform.runLater(() -> {
						idLabel.setText(Id.toString(identity.getGxsId()));
						updatedLabel.setText(DATE_TIME_FORMATTER.format(identity.getUpdated()));
					}))
					.subscribe();

			contactImagePane.getChildren().getFirst().setVisible(false);
			contactImageView.setUrl(IDENTITIES_PATH + "/" + contact.identityId() + "/image");
			typeLabel.setText("Contact");
			hideTableLocations();
		}
	}

	private void showTableLocations(List<Location> locations)
	{
		if (locations.isEmpty())
		{
			hideTableLocations();
		}
		else
		{
			locationTableView.getItems().setAll(locations);
			locationTableView.setVisible(true);
		}
	}

	private void hideTableLocations()
	{
		locationTableView.getItems().clear();
		locationTableView.setVisible(false);
	}
}
