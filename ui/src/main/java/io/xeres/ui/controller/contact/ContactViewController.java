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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.xeres.common.id.Id;
import io.xeres.common.protocol.HostPort;
import io.xeres.common.rest.contact.Contact;
import io.xeres.common.rest.notification.contact.AddContacts;
import io.xeres.common.rest.notification.contact.RemoveContacts;
import io.xeres.ui.OpenUriEvent;
import io.xeres.ui.client.*;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.custom.AsyncImageView;
import io.xeres.ui.model.location.Location;
import io.xeres.ui.support.uri.IdentityUri;
import io.xeres.ui.support.util.DateUtils;
import io.xeres.ui.support.util.UiUtils;
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
import javafx.scene.layout.VBox;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;
import static io.xeres.ui.support.util.DateUtils.DATE_TIME_DISPLAY;
import static javafx.scene.control.Alert.AlertType.WARNING;

@Component
@FxmlView(value = "/view/contact/contactview.fxml")
public class ContactViewController implements Controller
{
	private static final Logger log = LoggerFactory.getLogger(ContactViewController.class);

	private enum Information
	{
		PROFILE,
		IDENTITY,
		MERGED
	}

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
	private Label createdOrUpdated;

	@FXML
	private Label createdLabel;

	@FXML
	private FontIcon contactIcon;

	@FXML
	private Label acceptedLabel;

	@FXML
	private Label trustLabel;

	@FXML
	private VBox detailsView;

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
	private final NotificationClient notificationClient;
	private final ObjectMapper objectMapper;

	private Disposable notificationDisposable;

	public ContactViewController(ContactClient contactClient, GeneralClient generalClient, ProfileClient profileClient, IdentityClient identityClient, NotificationClient notificationClient, ObjectMapper objectMapper)
	{
		this.contactClient = contactClient;
		this.generalClient = generalClient;
		this.profileClient = profileClient;
		this.identityClient = identityClient;
		this.notificationClient = notificationClient;
		this.objectMapper = objectMapper;
	}

	@Override
	public void initialize() throws IOException
	{
		contactImageView.setLoader(url -> generalClient.getImage(url).block());
		contactImageView.setOnFailure(() -> contactImagePane.getChildren().getFirst().setVisible(true));

		contactTableNameColumn.setCellFactory(param -> new ContactCell(generalClient));
		contactTableNameColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));

		var filteredList = new FilteredList<>(contactTableView.getItems());

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

		setupContactNotifications();

		getContacts();
	}

	private void getContacts()
	{
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
	}

	private void setupContactNotifications()
	{
		notificationDisposable = notificationClient.getContactNotifications()
				.doOnError(UiUtils::showAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					if (sse.data() != null)
					{
						var idName = Objects.requireNonNull(sse.id());

						if (idName.equals(AddContacts.class.getSimpleName()))
						{
							var action = objectMapper.convertValue(sse.data().action(), AddContacts.class);
							addContacts(action.contacts());
						}
						else if (idName.equals(RemoveContacts.class.getSimpleName()))
						{
							var action = objectMapper.convertValue(sse.data().action(), RemoveContacts.class);
							action.contacts().forEach(this::removeContact);
						}
						else
						{
							log.debug("Unknown contact notification");
						}
					}
				}))
				.subscribe();
	}

	private void addContacts(List<Contact> contacts)
	{
		var added = false;

		for (Contact contact : contacts)
		{
			if (addContact(contact))
			{
				added = true;
			}
		}

		if (added)
		{
			contactTableView.sort();
		}
	}

	private boolean addContact(Contact contact)
	{
		log.debug("Adding contact {}", contact);
		// XXX: should we have a map to speed up all this?
		if (contact.identityId() != 0L)
		{
			if (contactTableView.getItems().stream()
					.noneMatch(existingContact -> existingContact.identityId() == contact.identityId()))
			{
				contactTableView.getItems().add(contact);
				return true;
			}
		}
		else if (contact.profileId() != 0L)
		{
			if (contactTableView.getItems().stream()
					.noneMatch(existingContact -> existingContact.profileId() == contact.profileId() && existingContact.name().equals(contact.name())))
			{
				contactTableView.getItems().add(contact);
				return true;
			}
		}
		else
		{
			throw new IllegalStateException("Empty contact (identity == 0L and profile == 0L). Shouldn't happen.");
		}
		return false;
	}

	private void removeContact(Contact contact)
	{
		log.debug("Removing contact {}", contact);
		contactTableView.getItems().removeIf(existingContact -> existingContact.identityId() == contact.identityId());
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
				return DATE_TIME_DISPLAY.format(lastConnected);
			}
		}
	}

	private void changeSelectedContact(Contact contact)
	{
		if (contact == null)
		{
			detailsView.setVisible(false);
			nameLabel.setText(null);
			idLabel.setText(null);
			typeLabel.setText(null);
			createdLabel.setText(null);
			contactImageView.setImage(null);
			contactImagePane.getChildren().getFirst().setVisible(true);
			profilePane.setVisible(false);
			hideTableLocations();
			return;
		}

		detailsView.setVisible(true);
		nameLabel.setText(contact.name());
		if (contact.profileId() != 0L && contact.identityId() != 0L)
		{
			contactImagePane.getChildren().getFirst().setVisible(false);
			contactImageView.setUrl(IDENTITIES_PATH + "/" + contact.identityId() + "/image");
			typeLabel.setText("Contact linked to profile");

			fetchProfile(contact.profileId(), Information.MERGED);
			fetchContact(contact.identityId(), Information.MERGED);
		}
		else if (contact.profileId() != 0L)
		{
			contactImagePane.getChildren().getFirst().setVisible(true);
			contactImageView.setUrl(null);
			typeLabel.setText("Profile");

			fetchProfile(contact.profileId(), Information.PROFILE);
		}
		else if (contact.identityId() != 0L)
		{
			profilePane.setVisible(false);

			contactImagePane.getChildren().getFirst().setVisible(false);
			contactImageView.setUrl(IDENTITIES_PATH + "/" + contact.identityId() + "/image");
			typeLabel.setText("Contact");
			hideTableLocations();

			fetchContact(contact.identityId(), Information.IDENTITY);
		}
	}

	private void fetchProfile(long profileId, Information information)
	{
		profileClient.findById(profileId)
				.doOnSuccess(profile -> Platform.runLater(() -> {
					if (information == Information.PROFILE)
					{
						idLabel.setText(Id.toString(profile.getPgpIdentifier()));
					}
					if (information == Information.PROFILE || information == Information.MERGED)
					{
						createdOrUpdated.setText("Created");
						createdLabel.setText(profile.getCreated() != null ? DateUtils.DATE_TIME_DISPLAY.format(profile.getCreated()) : "unknown");
						if (information == Information.MERGED)
						{
							typeLabel.setText("Contact linked to profile " + Id.toString(profile.getPgpIdentifier()));
						}
					}
					acceptedLabel.setText(profile.isAccepted() ? "yes" : "no");
					trustLabel.setText(profile.getTrust().toString());
					profilePane.setVisible(true);
					showTableLocations(profile.getLocations());
				}))
				.subscribe();
	}

	private void fetchContact(long identityId, Information information)
	{
		identityClient.findById(identityId)
				.doOnSuccess(identity -> Platform.runLater(() -> {
					if (information == Information.IDENTITY || information == Information.MERGED)
					{
						idLabel.setText(Id.toString(identity.getGxsId()));
					}
					if (information == Information.IDENTITY)
					{
						createdOrUpdated.setText("Updated");
						createdLabel.setText(DATE_TIME_DISPLAY.format(identity.getUpdated()));
					}
				}))
				.subscribe();
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

	@EventListener
	public void onApplicationEvent(ContextClosedEvent ignored)
	{
		if (notificationDisposable != null && !notificationDisposable.isDisposed())
		{
			notificationDisposable.dispose();
		}
	}

	@EventListener
	public void handleOpenUriEvent(OpenUriEvent event)
	{
		if (event.uri() instanceof IdentityUri identityUri)
		{
			identityClient.findByGxsId(identityUri.id()).collectList()
					.doOnSuccess(identities -> {
						if (!identities.isEmpty())
						{
							Platform.runLater(() -> contactTableView.getItems().stream()
									.filter(contact -> contact.identityId() == identities.getFirst().getId())
									.findFirst()
									.ifPresentOrElse(contact -> {
										contactTableView.getSelectionModel().select(contact);
										contactTableView.scrollTo(contact);
									}, () -> UiUtils.alert(WARNING, "Contact not found")));
						}
					})
					.subscribe();
		}
	}
}
