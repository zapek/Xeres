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
import io.xeres.common.pgp.Trust;
import io.xeres.common.protocol.HostPort;
import io.xeres.common.rest.contact.Contact;
import io.xeres.ui.OpenUriEvent;
import io.xeres.ui.client.*;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.model.location.Location;
import io.xeres.ui.model.profile.Profile;
import io.xeres.ui.support.contextmenu.XContextMenu;
import io.xeres.ui.support.uri.IdentityUri;
import io.xeres.ui.support.util.UiUtils;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
import java.text.MessageFormat;
import java.util.*;

import static io.xeres.common.dto.profile.ProfileConstants.OWN_PROFILE_ID;
import static io.xeres.ui.support.util.DateUtils.DATE_TIME_DISPLAY;
import static io.xeres.ui.support.util.UiUtils.getWindow;
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
	private TreeTableView<Contact> contactTreeTableView;

	@FXML
	private TreeTableColumn<Contact, Contact> contactTreeTableNameColumn;

	@FXML
	private TreeTableColumn<Contact, Availability> contactTreeTablePresenceColumn;

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
	private MenuItem jumpToOwn;

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
	private final ImageCache imageCacheService;
	private final WindowManager windowManager;
	private final ResourceBundle bundle;

	private Disposable contactNotificationDisposable;
	private Disposable availabilityNotificationDisposable;

	private final ObservableList<TreeItem<Contact>> contactObservableList = FXCollections.observableArrayList(p -> new Observable[]{p.valueProperty()}); // Changing the value will mark the list as changed
	private final FilteredList<TreeItem<Contact>> filteredList = new FilteredList<>(contactObservableList);
	private final ContactFilter contactFilter = new ContactFilter(filteredList);
	private Comparator<TreeItem<Contact>> treeItemComparator;

	private TreeItem<Contact> savedSelection;
	private boolean refreshContactTableView = true; // Prevent multiple refreshes when adding/removing entries

	public ContactViewController(ContactClient contactClient, GeneralClient generalClient, ProfileClient profileClient, IdentityClient identityClient, NotificationClient notificationClient, ImageCache imageCacheService, ResourceBundle bundle, WindowManager windowManager)
	{
		this.contactClient = contactClient;
		this.generalClient = generalClient;
		this.profileClient = profileClient;
		this.identityClient = identityClient;
		this.notificationClient = notificationClient;
		this.imageCacheService = imageCacheService;
		this.bundle = bundle;
		this.windowManager = windowManager;
	}

	@Override
	public void initialize() throws IOException
	{
		contactImageView.setLoader(url -> generalClient.getImage(url).block());
		contactImageView.setOnFailure(() -> contactIcon.setVisible(true));
		contactImageView.setImageCache(imageCacheService);

		setupContactTreeTableView();
		setupLocationTableView();

		setupMenuFilters();

		// Workaround for https://github.com/mkpaz/atlantafx/issues/31
		contactIcon.iconSizeProperty()
				.addListener((observable, oldValue, newValue) -> contactIcon.setIconSize(128));

		contactImageView.setOnMouseEntered(event -> contactImageSelectButton.setOpacity(0.8));
		contactImageView.setOnMouseExited(event -> contactImageSelectButton.setOpacity(0.0));
		contactImageSelectButton.setOnMouseEntered(event -> contactImageSelectButton.setOpacity(0.8));
		contactImageSelectButton.setOnMouseExited(event -> contactImageSelectButton.setOpacity(0.0));
		contactImageSelectButton.setOnAction(this::selectOwnContactImage);

		chatButton.setOnAction(event -> startChat());

		setupContactNotifications();
		setupConnectionNotifications();

		getContacts();
	}

	private void setupContactTreeTableView()
	{
		searchTextField.textProperty().addListener((observable, oldValue, newValue) -> contactFilter.setNameFilter(newValue));

		contactTreeTableNameColumn.setCellFactory(param -> new ContactCellName(generalClient, imageCacheService));
		contactTreeTableNameColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue()));

		contactTreeTablePresenceColumn.setCellFactory(param -> new AvailabilityTreeCellStatus<>());
		contactTreeTablePresenceColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue().availability()));

		contactTreeTableView.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> displayContact(newValue != null ? newValue.getValue() : null));

		var treeRoot = new TreeItem<>(Contact.EMPTY);
		treeRoot.setExpanded(true);

		Bindings.bindContent(treeRoot.getChildren(), filteredList);

		// We need that to support sorting because a SortedList derived
		// from a FilteredList doesn't work for a TreeTableView somehow.
		contactTreeTableView.comparatorProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null)
			{
				treeItemComparator = newValue;
				sortContacts();
			}
		});

		contactTreeTableView.setRoot(treeRoot);
		contactTreeTableView.setShowRoot(false);

		createContactTableViewContextMenu();
	}

	private void sortContacts()
	{
		FXCollections.sort(contactObservableList, treeItemComparator);
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
		showAllContacts.selectedProperty().addListener((observable, oldValue, newValue) -> contactFilter.setShowAllContacts(newValue));

		jumpToOwn.setOnAction(event -> selectOwnContact());
	}

	private void selectOwnContact()
	{
		contactObservableList.stream()
				.filter(contact -> contact.getValue().profileId() == 1L)
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
					if (contact.profileId() != 0L)
					{
						if (contact.identityId() != 0L)
						{
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

					// Sort by connected first then name
					contactTreeTableView.setSortPolicy(table -> true); // This is needed otherwise the default sorting will break everything
					contactTreeTablePresenceColumn.setSortType(TreeTableColumn.SortType.ASCENDING);
					contactTreeTablePresenceColumn.setSortable(true);
					contactTreeTableNameColumn.setSortType(TreeTableColumn.SortType.ASCENDING);
					contactTreeTableNameColumn.setSortable(true);
					//noinspection unchecked
					contactTreeTableView.getSortOrder().setAll(contactTreeTablePresenceColumn, contactTreeTableNameColumn);
				}))
				.subscribe();
	}

	private void updateProfileWithIdentity(TreeItem<Contact> profile, TreeItem<Contact> identity)
	{
		if (profile.getValue().identityId() != 0L)
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
						profile.getChildren().remove(identity);
						profile.getChildren().add(identity);
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
		if (profile.getValue().name().equals(identity.getValue().name()))
		{
			profile.setValue(identity.getValue());
			return true;
		}
		return false;
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
		availabilityNotificationDisposable = notificationClient.getAvailabilityNotifications()
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
			sortContacts();
		}
	}

	private TreeItem<Contact> findProfile(long profileId)
	{
		return contactObservableList.stream()
				.filter(existingContact -> existingContact.getValue().profileId() == profileId)
				.findFirst().orElse(null);
	}

	private boolean addContact(Contact contact)
	{
		log.debug("Adding contact {}", contact);

		if (contact.profileId() != 0L && contact.identityId() != 0L)
		{
			// Full contact
			var existing = findProfile(contact.profileId());
			var item = new TreeItem<>(contact);

			if (existing != null)
			{
				imageCacheService.evictImage(ContactCellName.getIdentityImageUrl(contact));
				updateProfileWithIdentity(existing, item);
				return false;
			}
			else
			{
				contactObservableList.add(item);
				return true;
			}
		}
		else if (contact.profileId() != 0L)
		{
			// Lone profile
			var existing = findProfile(contact.profileId());
			var item = new TreeItem<>(contact);

			if (existing != null)
			{
				existing.setValue(contact);
				return false;
			}
			else
			{
				contactObservableList.add(item);
				return true;
			}
		}
		else if (contact.identityId() != 0L)
		{
			// Lone identity
			var existing = contactObservableList.stream()
					.filter(existingContact -> existingContact.getValue().identityId() == contact.identityId())
					.findFirst().orElse(null);

			if (existing != null)
			{
				imageCacheService.evictImage(ContactCellName.getIdentityImageUrl(contact));
				existing.setValue(contact);
				return false;
			}
			else
			{
				contactObservableList.add(new TreeItem<>(contact));
				return true;
			}
		}
		else
		{
			throw new IllegalStateException("Empty contact (identity == 0L and profile == 0L). Shouldn't happen.");
		}
	}

	private void saveSelection()
	{
		refreshContactTableView = false;
		savedSelection = contactTreeTableView.getSelectionModel().getSelectedItem();
	}

	private void restoreSelection(TreeItem<Contact> contact, boolean forceRefresh)
	{
		refreshContactTableView = true;
		if (isForSameContact(contact, savedSelection))
		{
			TreeItem<Contact> selectedItem = null;

			if (forceRefresh)
			{
				selectedItem = contactTreeTableView.getSelectionModel().getSelectedItem();
			}
			contactTreeTableView.getSelectionModel().select(contact);
		}
	}

	private boolean isForSameContact(TreeItem<Contact> first, TreeItem<Contact> second)
	{
		return first != null && second != null && first.getValue().identityId() == second.getValue().identityId() && first.getValue().profileId() == second.getValue().profileId();
	}

	private void removeContact(Contact contact)
	{
		log.debug("Removing contact {}", contact);
		contactObservableList.removeIf(existingContact -> existingContact.getValue().identityId() == contact.identityId());
	}

	private void updateContactConnection(long profileId, Availability availability)
	{
		log.debug("Updating contact connection {} with availability {}", profileId, availability);
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
			saveSelection();
			existing.setValue(Contact.withAvailability(existing.getValue(), availability));
			sortContacts();
			restoreSelection(existing, true);
		}
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

	private void displayContact(Contact contact)
	{
		if (!refreshContactTableView)
		{
			return;
		}

		if (contact == null)
		{
			clearSelection();
			return;
		}

		hideBadges();
		hideTableLocations();
		clearTrust();
		contactImageSelectButton.setVisible(false);
		detailsHeader.setVisible(true);
		detailsView.setVisible(true);
		nameLabel.setText(contact.name());
		chatButton.setDisable(contact.availability() == Availability.OFFLINE);
		if (contact.profileId() != 0L && contact.identityId() != 0L)
		{
			contactIcon.setVisible(false);
			contactImageView.setUrl(ContactCellName.getIdentityImageUrl(contact));
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
			contactImageView.setUrl(ContactCellName.getIdentityImageUrl(contact));
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
					showProfileInformation(profile, information);
					showBadges(profile);
					setTrust(profile);
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

	private void showProfileInformation(Profile profile, Information information)
	{
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
				UiUtils.alertConfirm(MessageFormat.format(bundle.getString("contactview.profile-delete.confirm"), contact.name()), () -> profileClient.delete(contact.profileId())
						.doOnSuccess(unused -> Platform.runLater(() -> contactObservableList.remove(new TreeItem<>(contact))))
						.subscribe());
			}
		});

		var xContextMenu = new XContextMenu<TreeItem<Contact>>(deleteItem);
		xContextMenu.setOnShowing((contextMenu, contact) -> contact != null && contact.getValue().profileId() != 0L && contact.getValue().profileId() != OWN_PROFILE_ID);
		xContextMenu.addToNode(contactTreeTableView);
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
							Platform.runLater(() -> contactObservableList.stream()
									.filter(contact -> contact.getValue().identityId() == identities.getFirst().getId())
									.findFirst()
									.ifPresentOrElse(contact -> {
										contactTreeTableView.getSelectionModel().select(contact);
										scrollToSelectedContact();
									}, () -> UiUtils.alert(WARNING, "Contact not found")));
						}
					})
					.subscribe();
		}
	}
}
