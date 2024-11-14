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

import atlantafx.base.controls.CustomTextField;
import io.xeres.common.i18n.I18nUtils;
import io.xeres.common.id.Id;
import io.xeres.common.location.Availability;
import io.xeres.common.pgp.Trust;
import io.xeres.common.protocol.HostPort;
import io.xeres.common.rest.contact.Contact;
import io.xeres.common.rest.profile.ProfileKeyAttributes;
import io.xeres.ui.OpenUriEvent;
import io.xeres.ui.client.*;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.model.connection.Connection;
import io.xeres.ui.model.location.Location;
import io.xeres.ui.model.profile.Profile;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.preference.PreferenceService;
import io.xeres.ui.support.uri.IdentityUri;
import io.xeres.ui.support.util.PublicKeyUtils;
import io.xeres.ui.support.util.TextInputControlUtils;
import io.xeres.ui.support.util.TooltipUtils;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import net.harawata.appdirs.AppDirsFactory;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

import static io.xeres.common.dto.identity.IdentityConstants.NO_IDENTITY_ID;
import static io.xeres.common.dto.identity.IdentityConstants.OWN_IDENTITY_ID;
import static io.xeres.common.dto.location.LocationConstants.OWN_LOCATION_ID;
import static io.xeres.common.dto.profile.ProfileConstants.NO_PROFILE_ID;
import static io.xeres.common.dto.profile.ProfileConstants.OWN_PROFILE_ID;
import static io.xeres.ui.support.preference.PreferenceService.CONTACTS;
import static io.xeres.ui.support.util.DateUtils.DATE_TIME_DISPLAY;
import static io.xeres.ui.support.util.UiUtils.getWindow;
import static javafx.scene.control.Alert.AlertType.WARNING;

@Component
@FxmlView(value = "/view/contact/contactview.fxml")
public class ContactViewController implements Controller
{
	private static final Logger log = LoggerFactory.getLogger(ContactViewController.class);

	private static final String SHOW_ALL_CONTACTS = "ShowAllContacts";
	private final ConfigClient configClient;

	private enum Information
	{
		PROFILE,
		IDENTITY,
		MERGED
	}

	@FXML
	private TreeTableView<Contact> contactTreeTableView;

	@FXML
	private TreeTableColumn<Contact, Contact> contactTreeTableNameColumn;

	@FXML
	private TreeTableColumn<Contact, Availability> contactTreeTablePresenceColumn;

	@FXML
	private CustomTextField searchTextField;

	@FXML
	private Button contactImageSelectButton;

	@FXML
	private Button contactImageDeleteButton;

	@FXML
	private AsyncImageView contactImageView;

	@FXML
	private AsyncImageView ownContactImageView;

	@FXML
	private Circle ownContactCircle;

	@FXML
	private Circle ownContactState;

	@FXML
	private Label ownContactName;

	@FXML
	private HBox ownContactGroup;

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
	private ChoiceBox<Trust> trust;

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
	private Button chatButton;

	@FXML
	private CheckMenuItem showAllContacts;

	@FXML
	private TableView<Location> locationTableView;

	@FXML
	private TableColumn<Location, String> locationTableNameColumn;

	@FXML
	private TableColumn<Location, Availability> locationTablePresenceColumn;

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
	private final PreferenceService preferenceService;
	private final ImageCache imageCacheService;
	private final WindowManager windowManager;
	private final ResourceBundle bundle;

	private Disposable contactNotificationDisposable;
	private Disposable availabilityNotificationDisposable;

	private final ObservableList<TreeItem<Contact>> contactObservableList = FXCollections.observableArrayList(p -> new Observable[]{p.valueProperty()}); // Changing the value will mark the list as changed so that sorting works, etc...
	private final SortedList<TreeItem<Contact>> sortedList = new SortedList<>(contactObservableList);
	private final FilteredList<TreeItem<Contact>> filteredList = new FilteredList<>(sortedList);
	private final ContactFilter contactFilter = new ContactFilter(filteredList);
	private final FontIcon searchClear = new FontIcon(MaterialDesignC.CLOSE_CIRCLE);

	private final TreeItem<Contact> treeRoot = new TreeItem<>(Contact.EMPTY);

	private TreeItem<Contact> ownContact;

	// Workaround for https://bugs.openjdk.org/browse/JDK-8090563
	private TreeItem<Contact> selectedItem;
	private TreeItem<Contact> displayedContact;
	private Contact addedContact;

	public ContactViewController(ContactClient contactClient, GeneralClient generalClient, ProfileClient profileClient, IdentityClient identityClient, NotificationClient notificationClient, PreferenceService preferenceService, ImageCache imageCacheService, ResourceBundle bundle, WindowManager windowManager, ConfigClient configClient)
	{
		this.contactClient = contactClient;
		this.generalClient = generalClient;
		this.profileClient = profileClient;
		this.identityClient = identityClient;
		this.notificationClient = notificationClient;
		this.preferenceService = preferenceService;
		this.imageCacheService = imageCacheService;
		this.bundle = bundle;
		this.windowManager = windowManager;
		this.configClient = configClient;
	}

	@Override
	public void initialize() throws IOException
	{
		contactImageView.setLoader(url -> generalClient.getImage(url).block());
		contactImageView.setImageCache(imageCacheService);

		setupContactSearch();
		setupContactTreeTableView();
		setupLocationTableView();

		setupMenuFilters();

		contactImageView.setOnMouseEntered(event -> setContactActionImagesOpacity(0.8));
		contactImageView.setOnMouseExited(event -> setContactActionImagesOpacity(0.0));
		contactImageSelectButton.setOnMouseEntered(event -> setContactActionImagesOpacity(0.8));
		contactImageSelectButton.setOnMouseExited(event -> setContactActionImagesOpacity(0.0));
		contactImageDeleteButton.setOnMouseEntered(event -> setContactActionImagesOpacity(0.8));
		contactImageDeleteButton.setOnMouseExited(event -> setContactActionImagesOpacity(0.0));
		contactImageSelectButton.setOnAction(this::selectOwnContactImage);
		contactImageDeleteButton.setOnAction(event -> UiUtils.alertConfirm(bundle.getString("contact-view.avatar-delete.confirm"), () -> identityClient.deleteIdentityImage(OWN_IDENTITY_ID).subscribe()));

		chatButton.setOnAction(event -> startChat());

		setupOwnContact();

		setupContactNotifications();
		setupConnectionNotifications();

		getContacts();
	}

	private void setContactActionImagesOpacity(double opacity)
	{
		contactImageSelectButton.setOpacity(opacity);
		if (contactImageView.getImage() != null)
		{
			contactImageDeleteButton.setOpacity(opacity);
		}
	}

	private void setupOwnContact()
	{
		ownContactImageView.setLoader(url -> generalClient.getImage(url).block());
		ownContactImageView.setOnSuccess(() -> {
			ownContactCircle.setVisible(true);
			ownContactCircle.setFill(new ImagePattern(ownContactImageView.getImage()));
		});
		if (Platform.isSupported(ConditionalFeature.EFFECT))
		{
			ownContactCircle.setEffect(new DropShadow(6, Color.rgb(0, 0, 0, 0.7)));
			ownContactState.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.9)));
		}
		ownContactImageView.setImageCache(imageCacheService);

		profileClient.getOwn()
				.doOnSuccess(profile -> {
					ownContactName.setText(profile.getName());
					ownContact = new TreeItem<>(Contact.withName(Contact.OWN, profile.getName()));
				})
				.subscribe();
		displayOwnContactImage();

		ownContactGroup.setOnMouseClicked(event -> {
			if (event.getButton() == MouseButton.PRIMARY)
			{
				displayOwnContact();
			}
		});

		createStateContextMenu();
	}

	private void displayOwnContact()
	{
		if (ownContact == null)
		{
			log.error("Failure to load own contact, can't display");
			return;
		}
		contactTreeTableView.getSelectionModel().clearSelection();
		displayContact(ownContact);
	}

	private void displayOwnContactImage()
	{
		ownContactImageView.setUrl(ContactCellName.getIdentityImageUrl(Contact.OWN));
	}

	private void setupContactSearch()
	{
		searchClear.setCursor(Cursor.HAND);
		searchClear.setOnMouseClicked(event -> searchTextField.clear());

		TextInputControlUtils.addEnhancedInputContextMenu(searchTextField, null);
		searchTextField.textProperty().addListener((observable, oldValue, newValue) -> contactFilter.setNameFilter(newValue));
		searchTextField.lengthProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.intValue() > 0)
			{
				searchTextField.setRight(searchClear);
			}
			else
			{
				searchTextField.setRight(null);
			}
		});
	}

	private boolean initiatedByListChange;

	private void setupContactTreeTableView()
	{
		contactTreeTableNameColumn.setCellFactory(param -> new ContactCellName(generalClient, imageCacheService));
		contactTreeTableNameColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue()));

		contactTreeTablePresenceColumn.setCellFactory(param -> new AvailabilityTreeCellStatus<>());
		contactTreeTablePresenceColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue().availability()));

		// Sort by connected first then name
		contactTreeTableView.setSortPolicy(table -> true); // This is needed otherwise the default sorting will break everything
		contactTreeTablePresenceColumn.setSortType(TreeTableColumn.SortType.ASCENDING);
		contactTreeTablePresenceColumn.setSortable(true);
		contactTreeTableNameColumn.setSortType(TreeTableColumn.SortType.ASCENDING);
		contactTreeTableNameColumn.setSortable(true);

		// Do not allow selection of multiple entries
		contactTreeTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

		contactTreeTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (!initiatedByListChange) // We ignore events initiated by sortedList
			{
				selectedItem = newValue;
				displayContact(newValue);
			}
		});

		treeRoot.setExpanded(true);

		Bindings.bindContent(treeRoot.getChildren(), filteredList);

		sortedList.comparatorProperty().bind(contactTreeTableView.comparatorProperty());

		// Because of JDK-8248217 (and others), we have
		// to save the selection before the sortedList (and then filteredList) are
		// updated.
		sortedList.addListener((InvalidationListener) c -> {
			initiatedByListChange = true;
			selectedItem = contactTreeTableView.getSelectionModel().getSelectedItem();
		});

		// Then we restore the selection after filteredList has been
		// updated.
		filteredList.addListener((ListChangeListener<? super TreeItem<Contact>>) c -> {
			initiatedByListChange = false; // Events initiated by sortedList ends up here
			if (contactTreeTableView.getSelectionModel().getSelectedItem() == null && selectedItem != null)
			{
				contactTreeTableView.getSelectionModel().select(selectedItem);
			}
		});

		contactTreeTableView.setRoot(treeRoot);
		contactTreeTableView.setShowRoot(false);

		createContactTableViewContextMenu();
	}

	private void scrollToSelectedContact()
	{
		var index = contactTreeTableView.getSelectionModel().getSelectedIndex();
		if (index != -1)
		{
			contactTreeTableView.scrollTo(index);
		}
	}

	private void setupLocationTableView()
	{
		locationTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

		locationTableView.setRowFactory(param -> new LocationRow());

		locationTableNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
		locationTablePresenceColumn.setCellFactory(param -> new AvailabilityCellStatus<>());
		// The following is needed because location has no concept of offline presence
		locationTablePresenceColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().isConnected() ? param.getValue().getAvailability() : Availability.OFFLINE));
		locationTableIPColumn.setCellValueFactory(param -> {
			var hostPort = getConnectedAddress(param.getValue());
			return new SimpleStringProperty(hostPort != null ? hostPort.host() : "-");
		});
		locationTablePortColumn.setCellValueFactory(param -> {
			var hostPort = getConnectedAddress(param.getValue());
			return new SimpleStringProperty(hostPort != null ? String.valueOf(hostPort.port()) : "-");
		});
		locationTableLastConnectedColumn.setCellValueFactory(param -> new SimpleStringProperty(getLastConnection(param.getValue())));
	}

	private void setupMenuFilters()
	{
		showAllContacts.selectedProperty().addListener((observable, oldValue, newValue) -> {
			contactFilter.setShowAllContacts(newValue);
			preferenceService.getPreferences().node(CONTACTS).putBoolean(SHOW_ALL_CONTACTS, newValue);
		});
		showAllContacts.selectedProperty().set(preferenceService.getPreferences().node(CONTACTS).getBoolean(SHOW_ALL_CONTACTS, false));
	}

	private void selectOwnContact()
	{
		contactObservableList.stream()
				.filter(contact -> contact.getValue().profileId() == OWN_PROFILE_ID)
				.findFirst()
				.ifPresent(contact -> {
					contactTreeTableView.getSelectionModel().select(contact);
					scrollToSelectedContact();
				});
	}

	private void getContacts()
	{
		Map<Long, TreeItem<Contact>> contacts = new HashMap<>();
		List<TreeItem<Contact>> identities = new ArrayList<>();

		contactClient.getContacts()
				.doOnNext(contact -> {
					if (contact.profileId() != NO_PROFILE_ID)
					{
						if (contact.identityId() != NO_IDENTITY_ID)
						{
							if (contact.identityId() == OWN_IDENTITY_ID || contact.profileId() == OWN_PROFILE_ID)
							{
								// Own profile, we don't add it to the list
								// because it has its own section above.
								return;
							}
							if (contacts.containsKey(contact.profileId()))
							{
								var profile = contacts.get(contact.profileId());
								updateProfileWithIdentity(profile, new TreeItem<>(contact));
							}
							else
							{
								contacts.put(contact.profileId(), new TreeItem<>(contact));
							}
						}
						else
						{
							if (contact.profileId() == OWN_IDENTITY_ID)
							{
								// Own profile, we don't add it to the list
								// because it has its own section above.
								return;
							}
							if (contacts.put(contact.profileId(), new TreeItem<>(contact)) != null)
							{
								throw new IllegalStateException("Profile overwritten");
							}
						}
					}
					else
					{
						identities.add(new TreeItem<>(contact));
					}
				})
				.doOnComplete(() -> Platform.runLater(() -> {
					// Add all contacts
					contactObservableList.addAll(contacts.values());
					contactObservableList.addAll(identities);

					//noinspection unchecked
					contactTreeTableView.getSortOrder().setAll(contactTreeTablePresenceColumn, contactTreeTableNameColumn);
				}))
				.subscribe();
	}

	private void updateProfileWithIdentity(TreeItem<Contact> profile, TreeItem<Contact> identity)
	{
		if (profile.getValue().identityId() != NO_IDENTITY_ID)
		{
			// Profile with an identity already
			if (profile.getValue().identityId() == identity.getValue().identityId())
			{
				// Same identity, we replace it
				profile.setValue(identity.getValue());
			}
			else
			{
				if (profile.getChildren().isEmpty())
				{
					// Not the same, we replace if we have a matching name
					if (!replaceIfSameName(profile, identity))
					{
						profile.getChildren().add(identity);
					}
				}
				else
				{
					if (!replaceIfSameName(profile, identity))
					{
						replaceOrAddChildren(profile, identity);
					}
				}
			}
		}
		else
		{
			// Lone profile that gets an identity added
			if (!replaceIfSameName(profile, identity))
			{
				profile.getChildren().add(identity);
			}
		}
	}

	private boolean replaceIfSameName(TreeItem<Contact> profile, TreeItem<Contact> identity)
	{
		if (profile.getValue().name().equalsIgnoreCase(identity.getValue().name()))
		{
			profile.setValue(identity.getValue());
			return true;
		}
		return false;
	}

	private void replaceOrAddChildren(TreeItem<Contact> parent, TreeItem<Contact> identity)
	{
		for (TreeItem<Contact> child : parent.getChildren())
		{
			if (child.getValue().identityId() == identity.getValue().identityId())
			{
				child.setValue(identity.getValue());
				return;
			}
		}
		parent.getChildren().add(identity);
	}

	private void setupContactNotifications()
	{
		contactNotificationDisposable = notificationClient.getContactNotifications()
				.doOnError(UiUtils::showAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					Objects.requireNonNull(sse.data());

					switch (sse.data().operation())
					{
						case ADD_OR_UPDATE -> sse.data().contacts().forEach(this::addContact);
						case REMOVE -> sse.data().contacts().forEach(this::removeContact);
					}
				}))
				.subscribe();
	}

	private void setupConnectionNotifications()
	{
		availabilityNotificationDisposable = notificationClient.getAvailabilityNotifications()
				.doOnError(UiUtils::showAlertError)
				.doOnNext(sse -> Platform.runLater(() -> {
					Objects.requireNonNull(sse.data());
					updateContactConnection(sse.data().profileId(), sse.data().locationId(), sse.data().availability());
				}))
				.subscribe();
	}

	private TreeItem<Contact> findProfile(long profileId)
	{
		return contactObservableList.stream()
				.filter(existingContact -> existingContact.getValue().profileId() == profileId)
				.findFirst().orElse(null);
	}

	private void clearCachedImages(TreeItem<Contact> contact)
	{
		imageCacheService.evictImage(ContactCellName.getIdentityImageUrl(contact.getValue()));

		// Make sure AsyncImageView doesn't refuse to load the
		// url because it thinks it's already loaded.
		if (displayedContact != null && displayedContact.getValue().equals(contact.getValue()))
		{
			contactImageView.setImageProper(null);
		}
	}

	private void addContact(Contact contact)
	{
		log.debug("Adding contact {}", contact);
		addedContact = contact;

		if (contact.profileId() != NO_PROFILE_ID && contact.identityId() != NO_IDENTITY_ID)
		{
			if (contact.identityId() == OWN_IDENTITY_ID)
			{
				// Own identity, special handling
				Objects.requireNonNull(ownContact);
				clearCachedImages(ownContact);
				ownContactImageView.setUrl(null);
				ownContactCircle.setVisible(false);
				displayOwnContact();
				displayOwnContactImage();
				return;
			}

			// Full contact
			var existing = findProfile(contact.profileId());
			var item = new TreeItem<>(contact);

			if (existing != null)
			{
				clearCachedImages(existing);
				updateProfileWithIdentity(existing, item);
			}
			else
			{
				contactObservableList.add(item);
			}
		}
		else if (contact.profileId() != NO_PROFILE_ID)
		{
			if (contact.profileId() == OWN_PROFILE_ID)
			{
				// Own profile, special handling
				return;
			}

			// Lone profile
			var existing = findProfile(contact.profileId());
			var item = new TreeItem<>(contact);

			if (existing != null)
			{
				// This is a profile update (eg. different trust). We need to restore
				// the identity otherwise it won't display its image
				if (existing.getValue().identityId() != NO_IDENTITY_ID)
				{
					existing.setValue(Contact.withIdentityId(contact, existing.getValue().identityId()));
				}
				else
				{
					existing.setValue(contact);
				}
			}
			else
			{
				contactObservableList.add(item);
			}
		}
		else if (contact.identityId() != NO_IDENTITY_ID)
		{
			// Lone identity
			var existing = contactObservableList.stream()
					.filter(existingContact -> existingContact.getValue().identityId() == contact.identityId())
					.findFirst().orElse(null);

			if (existing != null)
			{
				clearCachedImages(existing);
				existing.setValue(contact);
			}
			else
			{
				contactObservableList.add(new TreeItem<>(contact));
			}
		}
		else
		{
			throw new IllegalStateException("Empty contact (identity == 0L and profile == 0L). Shouldn't happen.");
		}
	}

	private void removeContact(Contact contact)
	{
		log.debug("Removing contact {}", contact);
		contactObservableList.removeIf(existingContact -> existingContact.getValue().identityId() == contact.identityId());
	}

	private void updateContactConnection(long profileId, long locationId, Availability availability)
	{
		log.debug("Updating contact connection {} with availability {}", profileId, availability);

		if (locationId == OWN_LOCATION_ID)
		{
			setOwnContactState(availability);
			return;
		}

		var existing = contactObservableList.stream()
				.filter(existingContact -> existingContact.getValue().profileId() == profileId)
				.findFirst().orElse(null);

		if (existing == null)
		{
			log.debug("Contact for profile {} not found. Removed then disconnected?", profileId);
			return;
		}

		if (existing.getValue().availability() != availability) // Avoid useless refreshes
		{
			existing.setValue(Contact.withAvailability(existing.getValue(), availability));
		}
	}

	private void setOwnContactState(Availability availability)
	{
		switch (availability)
		{
			case AVAILABLE -> ownContactState.setFill(Color.LIMEGREEN);
			case AWAY -> ownContactState.setFill(Color.ORANGE);
			case BUSY -> ownContactState.setFill(Color.RED);
			case OFFLINE -> ownContactState.setFill(Color.GRAY);
		}
	}

	private HostPort getConnectedAddress(Location location)
	{
		if (location.isConnected())
		{
			var connection = location.getConnections().stream().max(Comparator.comparing(Connection::getLastConnected, Comparator.nullsFirst(Comparator.naturalOrder()))).orElse(null);
			if (connection != null)
			{
				return HostPort.parse(connection.getAddress());
			}
		}
		return null;
	}

	private String getLastConnection(Location location)
	{
		if (location.isConnected())
		{
			return bundle.getString("contact-view.location.last-connected.now");
		}
		else
		{
			var lastConnected = location.getLastConnected();
			if (lastConnected == null)
			{
				return bundle.getString("contact-view.location.last-connected.never");
			}
			else
			{
				return DATE_TIME_DISPLAY.format(lastConnected);
			}
		}
	}

	private void setTrust(Profile profile)
	{
		clearTrust();

		trust.getItems().addAll(Arrays.stream(Trust.values()).filter(t -> profile.isOwn() == (t == Trust.ULTIMATE)).toList());
		trust.getSelectionModel().select(profile.getTrust());
		trust.setDisable(profile.isOwn());
		trust.setOnAction(event -> profileClient.setTrust(profile.getId(), trust.getSelectionModel().getSelectedItem()).subscribe());
	}

	private void clearTrust()
	{
		trust.setOnAction(null);
		trust.getItems().clear();
	}

	private void displayContact(TreeItem<Contact> contact)
	{
		if (contact == null)
		{
			displayedContact = null;
			clearSelection();
			return;
		}

		// Prevent re-displaying the same contact when a different contact was added
		if (displayedContact != null && displayedContact == contact && addedContact != null && !addedContact.equals(contact.getValue()))
		{
			addedContact = null; // Only once
			return;
		}
		addedContact = null;
		displayedContact = contact;

		hideBadges();
		hideTableLocations();
		clearTrust();
		TooltipUtils.uninstall(idLabel);
		TooltipUtils.uninstall(typeLabel);
		setImageEditButtonsVisibility(false);
		detailsHeader.setVisible(true);
		detailsView.setVisible(true);
		nameLabel.setText(contact.getValue().name());
		chatButton.setDisable(contact.getValue().availability() == Availability.OFFLINE);
		contactImageView.setUrl(ContactCellName.getIdentityImageUrl(contact.getValue()));
		if (contact.getValue().profileId() != NO_PROFILE_ID && contact.getValue().identityId() != NO_IDENTITY_ID)
		{
			typeLabel.setText(bundle.getString("contact-view.information.linked-to-profile"));

			fetchProfile(contact.getValue().profileId(), Information.MERGED, contact.isLeaf());
			fetchIdentity(contact.getValue().identityId(), Information.MERGED);
		}
		else if (contact.getValue().profileId() != NO_PROFILE_ID)
		{
			typeLabel.setText(bundle.getString("contact-view.information.profile"));

			fetchProfile(contact.getValue().profileId(), Information.PROFILE, false);
		}
		else if (contact.getValue().identityId() != NO_IDENTITY_ID)
		{
			profilePane.setVisible(false);

			typeLabel.setText(bundle.getString("contact-view.information.identity"));
			hideTableLocations();

			fetchIdentity(contact.getValue().identityId(), Information.IDENTITY);
		}
	}

	private void clearSelection()
	{
		setImageEditButtonsVisibility(false);
		detailsHeader.setVisible(false);
		detailsView.setVisible(false);
		nameLabel.setText(null);
		idLabel.setText(null);
		TooltipUtils.uninstall(idLabel);
		typeLabel.setText(null);
		TooltipUtils.uninstall(typeLabel);
		createdLabel.setText(null);
		contactImageView.setImageProper(null);
		profilePane.setVisible(false);
		hideTableLocations();
	}

	private void setImageEditButtonsVisibility(boolean visible)
	{
		contactImageSelectButton.setVisible(visible);
		contactImageDeleteButton.setVisible(visible);
	}

	private void fetchProfile(long profileId, Information information, boolean isLeaf)
	{
		profileClient.findById(profileId)
				.doOnSuccess(profile -> Platform.runLater(() -> {
					showProfileInformation(profile, information);
					showBadges(profile);
					setTrust(profile);
					trust.setDisable(isLeaf);
					profilePane.setVisible(true);
					showTableLocations(profile.getLocations());
				}))
				.doOnError(throwable -> {
					if (throwable instanceof WebClientResponseException wEx && wEx.getStatusCode() == HttpStatus.NOT_FOUND)
					{
						Platform.runLater(() -> UiUtils.setPresent(badgeUnvalidated, true));
					}
				})
				.subscribe();
	}

	private void showProfileInformation(Profile profile, Information information)
	{
		if (information == Information.PROFILE)
		{
			idLabel.setText(Id.toString(profile.getPgpIdentifier()));
			showProfileKeyInformation(profile, idLabel);
		}
		if (information == Information.PROFILE || information == Information.MERGED)
		{
			createdOrUpdated.setText(bundle.getString("contact-view.information.created"));
			createdLabel.setText(profile.getCreated() != null ? DATE_TIME_DISPLAY.format(profile.getCreated()) : bundle.getString("contact-view.information.created-unknown"));
			if (information == Information.MERGED)
			{
				typeLabel.setText(bundle.getString("contact-view.information.linked-to-profile") + " " + Id.toString(profile.getPgpIdentifier()));
				showProfileKeyInformation(profile, typeLabel);
			}
		}
	}

	private void showProfileKeyInformation(Profile profile, Label node)
	{
		if (!profile.isPartial())
		{
			TooltipUtils.install(node, delayedTooltip -> profileClient.findProfileKeyAttributes(profile.getId())
					.doOnSuccess(profileKeyAttributes -> Platform.runLater(() -> delayedTooltip.show(getKeyInformation(profileKeyAttributes))))
					.subscribe());
		}
	}

	private String getKeyInformation(ProfileKeyAttributes profileKeyAttributes)
	{
		return MessageFormat.format(bundle.getString("contact-view.information.key-information"),
				profileKeyAttributes.version(),
				PublicKeyUtils.getKeyAlgorithmName(profileKeyAttributes.keyAlgorithm()),
				profileKeyAttributes.keyBits(),
				PublicKeyUtils.getSignatureHash(profileKeyAttributes.signatureHash()));
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

	private void fetchIdentity(long identityId, Information information)
	{
		identityClient.findById(identityId)
				.doOnSuccess(identity -> Platform.runLater(() -> {
					if (information == Information.IDENTITY || information == Information.MERGED)
					{
						idLabel.setText(Id.toString(identity.getGxsId()));
					}
					if (information == Information.IDENTITY)
					{
						createdOrUpdated.setText(bundle.getString("contact-view.information.updated"));
						createdLabel.setText(DATE_TIME_DISPLAY.format(identity.getUpdated()));
					}
					if (identityId == OWN_IDENTITY_ID)
					{
						if (identity.hasImage())
						{
							contactImageDeleteButton.setVisible(true);
						}
						contactImageSelectButton.setVisible(true);
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
		deleteItem.setGraphic(new FontIcon(MaterialDesignA.ACCOUNT_REMOVE));
		deleteItem.setOnAction(event -> {
			@SuppressWarnings("unchecked") var contact = (TreeItem<Contact>) event.getSource();
			if (contact.getValue().profileId() != NO_PROFILE_ID && contact.getValue().profileId() != OWN_PROFILE_ID)
			{
				UiUtils.alertConfirm(MessageFormat.format(bundle.getString("contact-view.profile-delete.confirm"), contact.getValue().name()), () -> profileClient.delete(contact.getValue().profileId())
						.subscribe());
			}
		});

		var xContextMenu = new XContextMenu<TreeItem<Contact>>(deleteItem);
		xContextMenu.setOnShowing((contextMenu, contact) -> contact != null && !isSubContact(contact) && contact.getValue().profileId() != NO_PROFILE_ID && contact.getValue().profileId() != OWN_PROFILE_ID);
		xContextMenu.addToNode(contactTreeTableView);
	}

	private void createStateContextMenu()
	{
		var availableItem = createStateMenuItem(Availability.AVAILABLE);
		var awayItem = createStateMenuItem(Availability.AWAY);
		var busyItem = createStateMenuItem(Availability.BUSY);

		var contextMenu = new ContextMenu(availableItem, awayItem, busyItem);
		ownContactState.setOnContextMenuRequested(event -> contextMenu.show(ownContactState, event.getScreenX(), event.getScreenY()));
		ownContactState.setOnMouseClicked(event -> {
			if (event.getButton() == MouseButton.PRIMARY)
			{
				contextMenu.show(ownContactState, event.getScreenX(), event.getScreenY());
				event.consume();
			}
		});
	}

	private MenuItem createStateMenuItem(Availability availability)
	{
		var menuItem = new MenuItem(availability.toString());
		menuItem.setGraphic(AvailabilityCellUtil.updateAvailability(null, availability));
		menuItem.setOnAction(event -> configClient.changeAvailability(availability).subscribe());
		return menuItem;
	}

	private boolean isSubContact(TreeItem<Contact> contact)
	{
		return contact.getParent() != treeRoot && contact.isLeaf();
	}

	private void selectOwnContactImage(ActionEvent event)
	{
		var fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("main.select-avatar"));
		fileChooser.setInitialDirectory(new File(AppDirsFactory.getInstance().getUserDownloadsDir(null, null, null)));
		fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(bundle.getString("file-requester.images"), "*.png", "*.jpg", "*.jpeg", "*.jfif"));
		var selectedFile = fileChooser.showOpenDialog(getWindow(event));
		if (selectedFile != null && selectedFile.canRead())
		{
			identityClient.uploadIdentityImage(OWN_IDENTITY_ID, selectedFile)
					.subscribe();
		}
	}

	private void startChat()
	{
		locationTableView.getItems().stream()
				.filter(Location::isConnected)
				.findFirst()
				.ifPresent(location -> windowManager.openMessaging(location.getLocationId().toString()));
	}

	@EventListener
	public void onApplicationEvent(ContextClosedEvent ignored)
	{
		if (contactNotificationDisposable != null && !contactNotificationDisposable.isDisposed())
		{
			contactNotificationDisposable.dispose();
		}
		if (availabilityNotificationDisposable != null && !availabilityNotificationDisposable.isDisposed())
		{
			availabilityNotificationDisposable.dispose();
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
							Platform.runLater(() -> {
								var identity = identities.getFirst();

								if (identity.getId() == OWN_IDENTITY_ID)
								{
									// This is our own identity.
									displayOwnContact();
									return;
								}

								contactObservableList.stream()
										.filter(contact -> contact.getValue().identityId() == identity.getId())
										.findFirst()
										.ifPresentOrElse(contact -> {
											contactTreeTableView.getSelectionModel().select(contact);
											scrollToSelectedContact();
										}, () -> UiUtils.alert(WARNING, bundle.getString("contact-view.open.identity-not-found")));
							});
						}
					})
					.subscribe();
		}
	}
}
