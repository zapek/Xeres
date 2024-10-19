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

import io.xeres.common.dto.identity.IdentityConstants;
import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.id.Id;
import io.xeres.common.location.Availability;
import io.xeres.common.protocol.HostPort;
import io.xeres.common.rest.contact.Contact;
import io.xeres.ui.OpenUriEvent;
import io.xeres.ui.client.*;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.custom.AsyncImageView;
import io.xeres.ui.model.location.Location;
import io.xeres.ui.model.profile.Profile;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.uri.IdentityUri;
import io.xeres.ui.support.util.UiUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import net.rgielen.fxweaver.core.FxmlView;
import net.rgielen.fxweaver.spring.InjectionPointLazyFxControllerAndViewResolver;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

import static io.xeres.common.dto.profile.ProfileConstants.OWN_PROFILE_ID;
import static io.xeres.common.rest.PathConfig.IDENTITIES_PATH;
import static io.xeres.ui.support.util.DateUtils.DATE_TIME_DISPLAY;
import static io.xeres.ui.support.util.UiUtils.getWindow;
import static javafx.scene.control.Alert.AlertType.WARNING;

@Component
@FxmlView(value = "/view/contact/contactview.fxml")
public class ContactViewController implements Controller
{
	private static final Logger log = LoggerFactory.getLogger(ContactViewController.class);
	private final InjectionPointLazyFxControllerAndViewResolver injectionPointLazyFxControllerAndViewResolver;

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
	private TableColumn<Contact, Availability> contactTablePresenceColumn;

	@FXML
	private TextField searchTextField;

	@FXML
	private Button contactImageSelectButton;

	@FXML
	private FontIcon contactIcon;

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
	private Label trustLabel;

	@FXML
	private HBox detailsHeader;

	@FXML
	private VBox detailsView;

	@FXML
	private GridPane profilePane;

	@FXML
	private Label badgeOwn;

	@FXML
	private Label badgePartial;

	@FXML
	private Label badgeAccepted;

	@FXML
	private Label badgeUnvalidated;

	@FXML
	private VBox locationsView;

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
	private final ResourceBundle bundle;

	private Disposable contactNotificationDisposable;
	private Disposable connectionNotificationDisposable;

	public ContactViewController(ContactClient contactClient, GeneralClient generalClient, ProfileClient profileClient, IdentityClient identityClient, NotificationClient notificationClient, ResourceBundle bundle, InjectionPointLazyFxControllerAndViewResolver injectionPointLazyFxControllerAndViewResolver)
	{
		this.contactClient = contactClient;
		this.generalClient = generalClient;
		this.profileClient = profileClient;
		this.identityClient = identityClient;
		this.notificationClient = notificationClient;
		this.bundle = bundle;
		this.injectionPointLazyFxControllerAndViewResolver = injectionPointLazyFxControllerAndViewResolver;
	}

	@Override
	public void initialize() throws IOException
	{
		contactImageView.setLoader(url -> generalClient.getImage(url).block());
		contactImageView.setOnFailure(() -> contactIcon.setVisible(true));

		contactTableNameColumn.setCellFactory(param -> new ContactCellName(generalClient));
		contactTableNameColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));

		contactTablePresenceColumn.setCellFactory(param -> new ContactCellStatus());
		contactTablePresenceColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().availability()));

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

		createContactTableViewContextMenu();

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

		contactImageView.setOnMouseEntered(event -> contactImageSelectButton.setOpacity(0.8));
		contactImageView.setOnMouseExited(event -> contactImageSelectButton.setOpacity(0.0));
		contactImageSelectButton.setOnMouseEntered(event -> contactImageSelectButton.setOpacity(0.8));
		contactImageSelectButton.setOnMouseExited(event -> contactImageSelectButton.setOpacity(0.0));
		contactImageSelectButton.setOnAction(this::selectOwnContactImage);

		setupContactNotifications();
		setupConnectionNotifications();

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
		contactNotificationDisposable = notificationClient.getContactNotifications()
				.doOnError(UiUtils::showAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					Objects.requireNonNull(sse.data());

					switch (sse.data().operation())
					{
						case ADD_OR_UPDATE -> addContacts(sse.data().contacts());
						case REMOVE -> sse.data().contacts().forEach(this::removeContact);
					}
				}))
				.subscribe();
	}

	private void setupConnectionNotifications()
	{
		connectionNotificationDisposable = notificationClient.getConnectionNotifications()
				.doOnError(UiUtils::showAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					Objects.requireNonNull(sse.data());
					updateContactConnection(sse.data().profileId(), sse.data().availability());
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
			var existing = contactTableView.getItems().stream()
					.filter(existingContact -> existingContact.identityId() == contact.identityId())
					.findFirst().orElse(null);
			removeIfFound(existing);

			contactTableView.getItems().add(contact);
			selectAgainIfPreviouslySelected(existing, contact);

			return contactChanged(existing, contact);
		}
		else if (contact.profileId() != 0L)
		{
			var existing = contactTableView.getItems().stream()
					.filter(existingContact -> existingContact.profileId() == contact.profileId() && existingContact.name().equals(contact.name()))
					.findFirst().orElse(null);
			removeIfFound(existing);

			contactTableView.getItems().add(contact);
			selectAgainIfPreviouslySelected(existing, contact);

			return contactChanged(existing, contact);
		}
		else
		{
			throw new IllegalStateException("Empty contact (identity == 0L and profile == 0L). Shouldn't happen.");
		}
	}

	private void removeIfFound(Contact contact)
	{
		if (contact != null)
		{
			contactTableView.getItems().remove(contact);
		}
	}

	private void selectAgainIfPreviouslySelected(Contact previous, Contact current)
	{
		if (previous != null)
		{
			contactTableView.getSelectionModel().select(current);
		}
	}

	private boolean contactChanged(Contact existing, Contact incoming)
	{
		return existing == null || !existing.name().equals(incoming.name());
	}

	private void removeContact(Contact contact)
	{
		log.debug("Removing contact {}", contact);
		contactTableView.getItems().removeIf(existingContact -> existingContact.identityId() == contact.identityId());
	}

	private void updateContactConnection(long profileId, Availability availability)
	{
		var existing = contactTableView.getItems().stream()
				.filter(existingContact -> existingContact.profileId() == profileId)
				.findFirst().orElse(null);

		if (existing == null)
		{
			log.debug("Contact for profile {} not found. Removed then disconnected?", profileId);
			return;
		}

		var updated = new Contact(existing.name(), existing.profileId(), existing.identityId(), availability);
		contactTableView.getItems().remove(existing);
		contactTableView.getItems().add(updated);
		selectAgainIfPreviouslySelected(existing, updated);
		contactTableView.sort();
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
			clearSelection();
			return;
		}

		hideBadges();
		contactImageSelectButton.setVisible(false);
		detailsHeader.setVisible(true);
		detailsView.setVisible(true);
		nameLabel.setText(contact.name());
		if (contact.profileId() != 0L && contact.identityId() != 0L)
		{
			contactIcon.setVisible(false);
			contactImageView.setUrl(IDENTITIES_PATH + "/" + contact.identityId() + "/image");
			typeLabel.setText("Contact linked to profile");

			fetchProfile(contact.profileId(), Information.MERGED);
			fetchContact(contact.identityId(), Information.MERGED);
		}
		else if (contact.profileId() != 0L)
		{
			contactIcon.setVisible(true);
			contactImageView.setUrl(null);
			typeLabel.setText("Profile");

			fetchProfile(contact.profileId(), Information.PROFILE);
		}
		else if (contact.identityId() != 0L)
		{
			profilePane.setVisible(false);

			contactIcon.setVisible(false);
			contactImageView.setUrl(IDENTITIES_PATH + "/" + contact.identityId() + "/image");
			typeLabel.setText("Contact");
			hideTableLocations();

			fetchContact(contact.identityId(), Information.IDENTITY);
		}
	}

	private void clearSelection()
	{
		contactImageSelectButton.setVisible(false);
		detailsHeader.setVisible(false);
		detailsView.setVisible(false);
		nameLabel.setText(null);
		idLabel.setText(null);
		typeLabel.setText(null);
		createdLabel.setText(null);
		contactImageView.setImage(null);
		contactIcon.setVisible(true);
		profilePane.setVisible(false);
		hideTableLocations();
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
						createdLabel.setText(profile.getCreated() != null ? DATE_TIME_DISPLAY.format(profile.getCreated()) : "unknown");
						if (information == Information.MERGED)
						{
							typeLabel.setText("Contact linked to profile " + Id.toString(profile.getPgpIdentifier()));
						}
					}
					showBadges(profile);
					trustLabel.setText(profile.getTrust().toString());
					profilePane.setVisible(true);
					showTableLocations(profile.getLocations());
					if (profile.isOwn())
					{
						contactImageSelectButton.setVisible(true);
					}
				}))
				.doOnError(throwable -> {
					if (throwable instanceof WebClientResponseException wEx && wEx.getStatusCode() == HttpStatus.NOT_FOUND)
					{
						Platform.runLater(() -> UiUtils.setPresent(badgeUnvalidated, true));
					}
				})
				.subscribe();
	}

	private void showBadges(Profile profile)
	{
		UiUtils.setPresent(badgeOwn, profile.isOwn());
		UiUtils.setPresent(badgePartial, profile.isPartial());
		UiUtils.setPresent(badgeAccepted, profile.isAccepted());
		UiUtils.setAbsent(badgeUnvalidated);
	}

	private void hideBadges()
	{
		UiUtils.setAbsent(badgeOwn);
		UiUtils.setAbsent(badgePartial);
		UiUtils.setAbsent(badgeAccepted);
		UiUtils.setAbsent(badgeUnvalidated);
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
				.doOnError(throwable -> Platform.runLater(this::clearSelection))
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
			locationsView.setVisible(true);
		}
	}

	private void hideTableLocations()
	{
		locationTableView.getItems().clear();
		locationsView.setVisible(false);
	}

	private void createContactTableViewContextMenu()
	{
		var deleteItem = new MenuItem(I18nUtils.getString("profiles.delete"));
		deleteItem.setGraphic(new FontIcon(FontAwesomeSolid.TIMES));
		deleteItem.setOnAction(event -> {
			var contact = (Contact) event.getSource();
			if (contact.profileId() != 0L && contact.profileId() != OWN_PROFILE_ID)
			{
				profileClient.delete(contact.profileId())
						.doOnSuccess(unused -> Platform.runLater(() -> contactTableView.getItems().remove(contact)))
						.subscribe();
			}
		});

		var xContextMenu = new XContextMenu<Contact>(deleteItem);
		xContextMenu.setOnShowing((contextMenu, contact) -> contact != null && contact.profileId() != 0L && contact.profileId() != OWN_PROFILE_ID);
		xContextMenu.addToNode(contactTableView);
	}

	private void selectOwnContactImage(ActionEvent event)
	{
		var fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("main.select-avatar"));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(bundle.getString("file-requester.images"), "*.png", "*.jpg", "*.jpeg", "*.jfif"));
		var selectedFile = fileChooser.showOpenDialog(getWindow(event));
		if (selectedFile != null && selectedFile.canRead())
		{
			identityClient.uploadIdentityImage(IdentityConstants.OWN_IDENTITY_ID, selectedFile)
					.subscribe();
		}
	}

	@EventListener
	public void onApplicationEvent(ContextClosedEvent ignored)
	{
		if (contactNotificationDisposable != null && !contactNotificationDisposable.isDisposed())
		{
			contactNotificationDisposable.dispose();
		}
		if (connectionNotificationDisposable != null && !connectionNotificationDisposable.isDisposed())
		{
			contactNotificationDisposable.dispose();
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
