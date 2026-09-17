/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.ui.controller.chess;

import atlantafx.base.controls.CustomTextField;
import io.xeres.common.dto.chess.ChessGameDTO;
import io.xeres.common.dto.chess.ChessHistorySummaryDTO;
import io.xeres.common.dto.chess.ChessLeaderboardEntryDTO;
import io.xeres.common.id.GxsId;
import io.xeres.common.rest.PathConfig;
import io.xeres.common.rest.contact.Contact;
import io.xeres.common.util.RemoteUtils;
import io.xeres.ui.client.ChessClient;
import io.xeres.ui.client.ContactClient;
import io.xeres.ui.client.GeneralClient;
import io.xeres.ui.client.IdentityClient;
import io.xeres.ui.controller.Controller;
import io.xeres.ui.custom.asyncimage.AsyncImageView;
import io.xeres.ui.custom.asyncimage.ImageCache;
import io.xeres.ui.model.identity.Identity;
import io.xeres.ui.support.chess.ChessSettings;
import io.xeres.ui.support.own.OwnCache;
import io.xeres.ui.support.sound.SoundPlayerService;
import io.xeres.ui.support.window.WindowManager;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import net.rgielen.fxweaver.core.FxmlView;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@FxmlView(value = "/view/chess/chess_page.fxml")
public class ChessPageController implements Controller, SmartLifecycle
{
	private static final Logger log = LoggerFactory.getLogger(ChessPageController.class);

	private static final DateTimeFormatter DATE_FORMATTER =
			DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

	// Top Bar
	@FXML private Button networkStatusButton;
	@FXML private Button helpButton;
	@FXML private TabPane tabPane;

	// Tabs
	@FXML private Tab chessPlayersTab;
	@FXML private Tab gameHistoryTab;
	@FXML private Tab leaderboardTab;

	// Chess Players Tab - Left Pane: Contacts
	@FXML private CustomTextField searchContactsField;
	@FXML private Button onlineFilterButton;
	@FXML private Button addContactButton;
	@FXML private TableView<ContactRow> contactsTable;
	@FXML private TableColumn<ContactRow, ContactRow> contactPlayerColumn;
	@FXML private TableColumn<ContactRow, String> contactStatusColumn;
	@FXML private TableColumn<ContactRow, String> contactLastSeenColumn;

	// Chess Players Tab - Right Pane: Available Players
	@FXML private CheckBox busyCheckBox;
	@FXML private Button chessProfileButton;
	@FXML private TableView<AvailablePlayerRow> availablePlayersTable;
	@FXML private TableColumn<AvailablePlayerRow, AvailablePlayerRow> playerColumn;
	@FXML private TableColumn<AvailablePlayerRow, String> statusColumn;
	@FXML private TableColumn<AvailablePlayerRow, AvailablePlayerRow> actionColumn;
	@FXML private TableColumn<AvailablePlayerRow, String> ratingColumn;
	@FXML private TableColumn<AvailablePlayerRow, String> rdColumn;
	@FXML private TableColumn<AvailablePlayerRow, String> lastSeenColumn;
	@FXML private TableColumn<AvailablePlayerRow, String> invitationColumn;
	@FXML private TableColumn<AvailablePlayerRow, AvailablePlayerRow> rejectColumn;

	// Chess Players Tab - Right Pane: Active Games
	@FXML private TableView<ChessGameDTO> activeGamesTable;
	@FXML private TableColumn<ChessGameDTO, String> activePlayersColumn;
	@FXML private TableColumn<ChessGameDTO, String> activeGameIdColumn;
	@FXML private TableColumn<ChessGameDTO, ChessGameDTO> activeActionColumn;

	// Game History Tab
	@FXML private TableView<ChessHistorySummaryDTO> historyTable;
	@FXML private TableColumn<ChessHistorySummaryDTO, String> historyDateColumn;
	@FXML private TableColumn<ChessHistorySummaryDTO, ChessHistorySummaryDTO> historyWhiteColumn;
	@FXML private TableColumn<ChessHistorySummaryDTO, ChessHistorySummaryDTO> historyBlackColumn;
	@FXML private TableColumn<ChessHistorySummaryDTO, String> historyResultColumn;
	@FXML private TableColumn<ChessHistorySummaryDTO, String> historyMovesColumn;
	@FXML private TableColumn<ChessHistorySummaryDTO, ChessHistorySummaryDTO> historyActionColumn;

	// Leaderboard Tab
	@FXML private TableView<ChessLeaderboardEntryDTO> leaderboardTable;
	@FXML private TableColumn<ChessLeaderboardEntryDTO, String> rankColumn;
	@FXML private TableColumn<ChessLeaderboardEntryDTO, ChessLeaderboardEntryDTO> leaderboardPlayerColumn;
	@FXML private TableColumn<ChessLeaderboardEntryDTO, String> leaderboardRatingColumn;
	@FXML private TableColumn<ChessLeaderboardEntryDTO, String> leaderboardRdColumn;
	@FXML private TableColumn<ChessLeaderboardEntryDTO, String> leaderboardGamesColumn;
	@FXML private TableColumn<ChessLeaderboardEntryDTO, String> leaderboardWinsColumn;
	@FXML private TableColumn<ChessLeaderboardEntryDTO, String> leaderboardDrawsColumn;
	@FXML private TableColumn<ChessLeaderboardEntryDTO, String> leaderboardLossesColumn;
	@FXML private TableColumn<ChessLeaderboardEntryDTO, String> leaderboardStatusColumn;
	@FXML private TableColumn<ChessLeaderboardEntryDTO, String> leaderboardLastPlayedColumn;

	// Data Collections
	private final ObservableList<ContactRow> contactsList = FXCollections.observableArrayList();
	private final ObservableList<AvailablePlayerRow> availablePlayersList = FXCollections.observableArrayList();
	private final ObservableList<ChessGameDTO> activeGamesList = FXCollections.observableArrayList();
	private final ObservableList<ChessHistorySummaryDTO> historyList = FXCollections.observableArrayList();
	private final ObservableList<ChessLeaderboardEntryDTO> leaderboardList = FXCollections.observableArrayList();

	private FilteredList<ContactRow> filteredContacts;
	private boolean filterOnlineOnly;
	private boolean running;
	private javafx.animation.Timeline refreshTimeline;

	private final Map<Long, Identity> identitiesById = new ConcurrentHashMap<>();
	private final Map<String, Identity> identitiesByGxsId = new ConcurrentHashMap<>();
	private final Map<String, ChessLeaderboardEntryDTO> ratingsByPeer = new ConcurrentHashMap<>();
	private final List<ChessGameDTO> latestGames = new ArrayList<>();
	private volatile String ownGxsId;

	// Injected services
	private final ChessClient chessClient;
	private final ContactClient contactClient;
	private final IdentityClient identityClient;
	private final GeneralClient generalClient;
	private final ImageCache imageCache;
	private final WindowManager windowManager;
	private final SoundPlayerService soundPlayerService;
	private final ChessSettings chessSettings;
	private final ResourceBundle bundle;
	private final OwnCache ownCache;

	public record ContactRow(String name, String gxsId, String status, String lastSeen) {}

	public record AvailablePlayerRow(String name, String gxsId, String status, int rating, int rd,
			String lastSeen, String invitationStatus, boolean canReject, ChessGameDTO activeGame) {}

	public ChessPageController(ChessClient chessClient, ContactClient contactClient, IdentityClient identityClient,
			GeneralClient generalClient, ImageCache imageCache, WindowManager windowManager,
			SoundPlayerService soundPlayerService, ChessSettings chessSettings, ResourceBundle bundle,
			OwnCache ownCache)
	{
		this.chessClient = chessClient;
		this.contactClient = contactClient;
		this.identityClient = identityClient;
		this.generalClient = generalClient;
		this.imageCache = imageCache;
		this.windowManager = windowManager;
		this.soundPlayerService = soundPlayerService;
		this.chessSettings = chessSettings;
		this.bundle = bundle;
		this.ownCache = ownCache;
	}

	@Override
	public void initialize()
	{
		setupHeader();
		setupChessPlayersTab();
		setupHistoryTab();
		setupLeaderboardTab();

		tabPane.getSelectionModel().selectedItemProperty().addListener((_, _, newTab) -> {
			if (newTab == leaderboardTab)
			{
				refreshLeaderboard();
			}
			else if (newTab == gameHistoryTab)
			{
				refreshHistory();
			}
			else if (newTab == chessPlayersTab)
			{
				loadIdentitiesAndContacts();
				refreshActiveGames();
			}
		});

		refreshTimeline = new javafx.animation.Timeline(
				new javafx.animation.KeyFrame(javafx.util.Duration.seconds(4), _ -> {
					if (running)
					{
						refreshActiveGames();
						fetchContacts();
					}
				})
		);
		refreshTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
		refreshTimeline.play();

		loadIdentitiesAndContacts();
		refreshActiveGames();
		refreshLeaderboard();
		refreshHistory();
	}

	@Override
	public void start()
	{
		running = true;
		if (refreshTimeline != null)
		{
			refreshTimeline.play();
		}
	}

	@Override
	public void stop()
	{
		running = false;
		if (refreshTimeline != null)
		{
			refreshTimeline.stop();
		}
	}

	@Override
	public boolean isRunning()
	{
		return running;
	}

	public void refreshOnTabSelection()
	{
		loadIdentitiesAndContacts();
		refreshActiveGames();
		if (tabPane != null)
		{
			var current = tabPane.getSelectionModel().getSelectedItem();
			if (current == leaderboardTab)
			{
				refreshLeaderboard();
			}
			else if (current == gameHistoryTab)
			{
				refreshHistory();
			}
		}
	}

	private void setupHeader()
	{
		if (helpButton != null)
		{
			helpButton.setOnAction(_ -> windowManager.openHelp(true));
		}
		if (networkStatusButton != null)
		{
			networkStatusButton.setOnAction(_ -> {
				var alert = new Alert(Alert.AlertType.INFORMATION, bundle.getString("chess.page.leaderboard.info"));
				alert.setHeaderText(bundle.getString("chess.page.title"));
				alert.show();
			});
		}
	}

	private void setupChessPlayersTab()
	{
		// Filtered contacts
		filteredContacts = new FilteredList<>(contactsList, _ -> true);
		contactsTable.setItems(filteredContacts);

		searchContactsField.textProperty().addListener((_, _, text) -> applyContactsFilter(text));
		onlineFilterButton.setOnAction(_ -> {
			filterOnlineOnly = !filterOnlineOnly;
			if (filterOnlineOnly)
			{
				if (!onlineFilterButton.getStyleClass().contains("accent"))
				{
					onlineFilterButton.getStyleClass().add("accent");
				}
			}
			else
			{
				onlineFilterButton.getStyleClass().remove("accent");
			}
			applyContactsFilter(searchContactsField.getText());
		});
		addContactButton.setOnAction(_ -> showAddChessContactDialog());
		contactsTable.setPlaceholder(new Label(bundle.getString("chess.page.contacts.empty")));

		contactsTable.setRowFactory(_ -> {
			var row = new TableRow<ContactRow>();
			var contextMenu = new ContextMenu();
			var inviteItem = new MenuItem(bundle.getString("chess.invite"));
			inviteItem.setOnAction(_ -> {
				var item = row.getItem();
				if (item != null && item.gxsId() != null)
				{
					chessClient.invite(item.gxsId()).subscribe(
							game -> Platform.runLater(() -> {
								openGame(game);
								refreshActiveGames();
							}),
							failure -> Platform.runLater(() -> showError(bundle.getString("chess.page.invite.error") + ": " + failure.getMessage()))
					);
				}
			});
			var removeItem = new MenuItem(bundle.getString("chess.page.contacts.remove"));
			removeItem.setOnAction(_ -> {
				var item = row.getItem();
				if (item != null && item.gxsId() != null)
				{
					chessClient.removeContact(item.gxsId()).subscribe(
							_ -> Platform.runLater(this::loadIdentitiesAndContacts),
							failure -> Platform.runLater(() -> showError(failure.getMessage()))
					);
				}
			});
			contextMenu.getItems().addAll(inviteItem, removeItem);

			row.contextMenuProperty().bind(
					javafx.beans.binding.Bindings.when(row.emptyProperty())
							.then((ContextMenu) null)
							.otherwise(contextMenu)
			);

			row.setOnMouseClicked(e -> {
				if (e.getClickCount() == 2 && !row.isEmpty())
				{
					var item = row.getItem();
					if (item != null && item.gxsId() != null)
					{
						chessClient.invite(item.gxsId()).subscribe(
								game -> Platform.runLater(() -> {
									openGame(game);
									refreshActiveGames();
								}),
								failure -> Platform.runLater(() -> showError(bundle.getString("chess.page.invite.error") + ": " + failure.getMessage()))
						);
					}
				}
			});
			return row;
		});

		contactPlayerColumn.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue()));
		contactPlayerColumn.setCellFactory(_ -> new TableCell<>() {
			private final AsyncImageView avatar = createAvatarView();
			private final Label nameLabel = new Label();
			private final HBox container = new HBox(8, avatar, nameLabel);
			{
				container.setAlignment(Pos.CENTER_LEFT);
			}
			@Override
			protected void updateItem(ContactRow item, boolean empty)
			{
				super.updateItem(item, empty);
				if (empty || item == null)
				{
					setGraphic(null);
				}
				else
				{
					nameLabel.setText(item.name());
					setAvatar(avatar, item.gxsId());
					setGraphic(container);
				}
			}
		});

		contactStatusColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().status()));
		contactStatusColumn.setCellFactory(_ -> createChessStatusCell());

		contactLastSeenColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().lastSeen()));
		contactLastSeenColumn.setCellFactory(_ -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty)
			{
				super.updateItem(item, empty);
				if (empty || item == null || item.isBlank())
				{
					setText(empty ? null : bundle.getString("chess.page.status.never"));
				}
				else
				{
					setText(formatDate(item));
				}
			}
		});

		// Available Players table
		availablePlayersTable.setItems(availablePlayersList);

		playerColumn.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue()));
		playerColumn.setCellFactory(_ -> new TableCell<>() {
			private final AsyncImageView avatar = createAvatarView();
			private final Label nameLabel = new Label();
			private final HBox container = new HBox(8, avatar, nameLabel);
			{
				container.setAlignment(Pos.CENTER_LEFT);
			}
			@Override
			protected void updateItem(AvailablePlayerRow item, boolean empty)
			{
				super.updateItem(item, empty);
				if (empty || item == null)
				{
					setGraphic(null);
				}
				else
				{
					nameLabel.setText(item.name());
					setAvatar(avatar, item.gxsId());
					setGraphic(container);
				}
			}
		});

		statusColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().status()));
		statusColumn.setCellFactory(_ -> createChessStatusCell());

		actionColumn.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue()));
		actionColumn.setCellFactory(_ -> new TableCell<>() {
			private final Button actionButton = new Button();
			{
				actionButton.getStyleClass().add("small");
			}

			@Override
			protected void updateItem(AvailablePlayerRow item, boolean empty)
			{
				super.updateItem(item, empty);
				if (empty || item == null)
				{
					setGraphic(null);
					return;
				}

				actionButton.getStyleClass().removeAll("accent", "danger", "success");
				actionButton.setOnAction(null);
				actionButton.setDisable(false);

				if ("INCOMING".equals(item.activeGame() != null ? item.activeGame().status() : ""))
				{
					// Incoming invite → show "Accept"
					actionButton.setText(bundle.getString("chess.page.accept"));
					actionButton.getStyleClass().add("success");
					actionButton.setOnAction(_ -> {
						if (item.gxsId() != null)
						{
							actionButton.setDisable(true);
							chessClient.action(item.gxsId(), "accept").subscribe(
									game -> Platform.runLater(() -> {
										openGame(game);
										refreshActiveGames();
									}),
									failure -> Platform.runLater(() -> {
										actionButton.setDisable(false);
										showError(failure.getMessage());
									})
							);
						}
					});
				}
				else if ("OUTGOING".equals(item.activeGame() != null ? item.activeGame().status() : ""))
				{
					// Outgoing pending invite → show "Pending…" / Cancel
					actionButton.setText(bundle.getString("chess.page.invite.pending"));
					actionButton.getStyleClass().add("danger");
					actionButton.setOnAction(_ -> {
						if (item.gxsId() != null)
						{
							actionButton.setDisable(true);
							chessClient.action(item.gxsId(), "cancel").subscribe(
									_ -> Platform.runLater(ChessPageController.this::refreshActiveGames),
									failure -> Platform.runLater(() -> {
										actionButton.setDisable(false);
										showError(failure.getMessage());
									})
							);
						}
					});
				}
				else if (item.activeGame() == null || !"ACTIVE".equals(item.activeGame().status()))
				{
					// Available and no ongoing game → show "Invite"
					actionButton.setText(bundle.getString("chess.page.invite"));
					actionButton.getStyleClass().add("accent");
					actionButton.setOnAction(_ -> {
						if (item.gxsId() != null)
						{
							actionButton.setDisable(true);
							chessClient.invite(item.gxsId()).subscribe(
									game -> Platform.runLater(() -> {
										actionButton.setDisable(false);
										openGame(game);
										ChessPageController.this.refreshActiveGames();
									}),
									failure -> Platform.runLater(() -> {
										actionButton.setDisable(false);
										showError(bundle.getString("chess.page.invite.error") + ": " + failure.getMessage());
									})
							);
						}
					});
				}
				else
				{
					// ACTIVE game in progress — no action button
					setGraphic(null);
					return;
				}
				setGraphic(actionButton);
			}
		});


		ratingColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(String.valueOf(v.getValue().rating())));
		rdColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(String.valueOf(v.getValue().rd())));
		lastSeenColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().lastSeen()));
		lastSeenColumn.setCellFactory(_ -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty)
			{
				super.updateItem(item, empty);
				if (empty || item == null || item.isBlank())
				{
					setText(empty ? null : bundle.getString("chess.page.status.never"));
				}
				else
				{
					setText(formatDate(item));
				}
			}
		});
		invitationColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().invitationStatus()));

		rejectColumn.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue()));
		rejectColumn.setCellFactory(_ -> new TableCell<>() {
			private final Button rejectButton = new Button(bundle.getString("chess.page.reject"));
			{
				rejectButton.getStyleClass().addAll("danger", "small");
				rejectButton.setOnAction(_ -> {
					var item = getItem();
					if (item != null && item.activeGame() != null)
					{
						var action = "INCOMING".equals(item.activeGame().status()) ? "decline" : "abort";
						chessClient.action(item.gxsId(), action).subscribe(
								_ -> Platform.runLater(() -> refreshActiveGames()),
								failure -> Platform.runLater(() -> showError(failure.getMessage()))
						);
					}
				});
			}
			@Override
			protected void updateItem(AvailablePlayerRow item, boolean empty)
			{
				super.updateItem(item, empty);
				if (empty || item == null || !item.canReject())
				{
					setGraphic(null);
				}
				else
				{
					setGraphic(rejectButton);
				}
			}
		});

		availablePlayersTable.setPlaceholder(new Label(bundle.getString("chess.page.no-active-games")));
		availablePlayersTable.setRowFactory(_ -> {
			var row = new TableRow<AvailablePlayerRow>();
			var contextMenu = new ContextMenu();
			var inviteItem = new MenuItem(bundle.getString("chess.invite"));
			inviteItem.setOnAction(_ -> {
				var item = row.getItem();
				if (item != null && item.gxsId() != null)
				{
					chessClient.invite(item.gxsId()).subscribe(
							game -> Platform.runLater(() -> {
								openGame(game);
								refreshActiveGames();
							}),
							failure -> Platform.runLater(() -> showError(bundle.getString("chess.page.invite.error") + ": " + failure.getMessage()))
					);
				}
			});
			var saveContactItem = new MenuItem(bundle.getString("chess.page.contacts.add"));
			saveContactItem.setOnAction(_ -> {
				var item = row.getItem();
				if (item != null && item.gxsId() != null)
				{
					chessClient.addContact(item.gxsId()).subscribe(
							_ -> Platform.runLater(this::loadIdentitiesAndContacts),
							failure -> Platform.runLater(() -> showError(failure.getMessage()))
					);
				}
			});
			var removeContactItem = new MenuItem(bundle.getString("chess.page.contacts.remove"));
			removeContactItem.setOnAction(_ -> {
				var item = row.getItem();
				if (item != null && item.gxsId() != null)
				{
					chessClient.removeContact(item.gxsId()).subscribe(
							_ -> Platform.runLater(this::loadIdentitiesAndContacts),
							failure -> Platform.runLater(() -> showError(failure.getMessage()))
					);
				}
			});
			var acceptItem = new MenuItem(bundle.getString("chess.accept"));
			acceptItem.setOnAction(_ -> {
				var item = row.getItem();
				if (item != null && item.gxsId() != null)
				{
					chessClient.action(item.gxsId(), "accept").subscribe(
							game -> Platform.runLater(() -> {
								openGame(game);
								refreshActiveGames();
							}),
							failure -> Platform.runLater(() -> showError(failure.getMessage()))
					);
				}
			});
			var declineItem = new MenuItem(bundle.getString("chess.decline"));
			declineItem.setOnAction(_ -> {
				var item = row.getItem();
				if (item != null && item.gxsId() != null)
				{
					chessClient.action(item.gxsId(), "decline").subscribe(
							_ -> Platform.runLater(this::refreshActiveGames),
							failure -> Platform.runLater(() -> showError(failure.getMessage()))
					);
				}
			});

			row.contextMenuProperty().bind(
					javafx.beans.binding.Bindings.createObjectBinding(() -> {
						if (row.isEmpty() || row.getItem() == null)
						{
							return null;
						}
						var item = row.getItem();
						contextMenu.getItems().clear();
						boolean isIncoming = item.activeGame() != null && "INCOMING".equals(item.activeGame().status());
						if (isIncoming)
						{
							contextMenu.getItems().addAll(acceptItem, declineItem, new SeparatorMenuItem());
						}
						else
						{
							contextMenu.getItems().add(inviteItem);
						}
						boolean isSaved = contactsList.stream().anyMatch(c -> c.gxsId().equalsIgnoreCase(item.gxsId()));
						if (isSaved)
						{
							contextMenu.getItems().add(removeContactItem);
						}
						else
						{
							contextMenu.getItems().add(saveContactItem);
						}
						return contextMenu;
					}, row.itemProperty(), contactsList)
			);

			row.setOnMouseClicked(e -> {
				if (e.getClickCount() == 2 && !row.isEmpty())
				{
					var item = row.getItem();
					if (item != null && item.gxsId() != null)
					{
						if (item.activeGame() != null && "INCOMING".equals(item.activeGame().status()))
						{
							chessClient.action(item.gxsId(), "accept").subscribe(
									game -> Platform.runLater(() -> {
										openGame(game);
										refreshActiveGames();
									}),
									failure -> Platform.runLater(() -> showError(failure.getMessage()))
							);
						}
						else
						{
							chessClient.invite(item.gxsId()).subscribe(
									game -> Platform.runLater(() -> {
										openGame(game);
										refreshActiveGames();
									}),
									failure -> Platform.runLater(() -> showError(bundle.getString("chess.page.invite.error") + ": " + failure.getMessage()))
							);
						}
					}
				}
			});
			return row;
		});

		// Active Games table
		activeGamesTable.setItems(activeGamesList);
		activePlayersColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(formatPlayers(v.getValue())));
		activeGameIdColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().peer()));
		activeActionColumn.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue()));
		activeActionColumn.setCellFactory(_ -> new TableCell<>() {
			private final Button openButton = new Button(bundle.getString("chess.history-open"));
			{
				openButton.getStyleClass().addAll("accent", "small");
				openButton.setOnAction(_ -> {
					var item = getItem();
					if (item != null)
					{
						openGame(item);
					}
				});
			}
			@Override
			protected void updateItem(ChessGameDTO item, boolean empty)
			{
				super.updateItem(item, empty);
				setGraphic(empty || item == null ? null : openButton);
			}
		});

		activeGamesTable.setRowFactory(_ -> {
			var row = new TableRow<ChessGameDTO>();
			row.setOnMouseClicked(e -> {
				if (e.getClickCount() == 2 && !row.isEmpty())
				{
					openGame(row.getItem());
				}
			});
			return row;
		});

		if (busyCheckBox != null)
		{
			chessClient.isBusy().subscribe(busy -> Platform.runLater(() -> busyCheckBox.setSelected(busy)));
			busyCheckBox.setOnAction(_ -> {
				boolean busy = busyCheckBox.isSelected();
				chessClient.setBusy(busy).subscribe(
						null,
						failure -> Platform.runLater(() -> {
							busyCheckBox.setSelected(!busy);
							showError(failure.getMessage());
						})
				);
			});
		}

		chessProfileButton.setOnAction(_ -> showChessProfileDialog());
	}

	private void setupHistoryTab()
	{
		historyTable.setItems(historyList);
		historyDateColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(formatDate(v.getValue().startedAt())));

		historyWhiteColumn.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue()));
		historyWhiteColumn.setCellFactory(_ -> new ChessHistoryIdentityCell(true, generalClient, imageCache));

		historyBlackColumn.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue()));
		historyBlackColumn.setCellFactory(_ -> new ChessHistoryIdentityCell(false, generalClient, imageCache));

		historyResultColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(formatResult(v.getValue().status())));
		historyMovesColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(String.valueOf((v.getValue().moves() + 1) / 2)));

		historyActionColumn.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue()));
		historyActionColumn.setCellFactory(_ -> new TableCell<>() {
			private final Button reviewButton = new Button(bundle.getString("chess.review"));
			{
				reviewButton.getStyleClass().addAll("small");
				reviewButton.setOnAction(_ -> {
					var item = getItem();
					if (item != null)
					{
						chessClient.history(item.id()).subscribe(
								game -> Platform.runLater(() -> ChessGameReviewWindow.open(game, item, bundle, chessSettings)),
								failure -> Platform.runLater(() -> showError(failure.getMessage()))
						);
					}
				});
			}
			@Override
			protected void updateItem(ChessHistorySummaryDTO item, boolean empty)
			{
				super.updateItem(item, empty);
				setGraphic(empty || item == null ? null : reviewButton);
			}
		});

		historyTable.setRowFactory(_ -> {
			var row = new TableRow<ChessHistorySummaryDTO>();
			row.setOnMouseClicked(e -> {
				if (e.getClickCount() == 2 && !row.isEmpty())
				{
					var item = row.getItem();
					chessClient.history(item.id()).subscribe(
							game -> Platform.runLater(() -> ChessGameReviewWindow.open(game, item, bundle, chessSettings)),
							failure -> Platform.runLater(() -> showError(failure.getMessage()))
					);
				}
			});
			return row;
		});
	}

	private void setupLeaderboardTab()
	{
		leaderboardTable.setItems(leaderboardList);
		rankColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(String.valueOf(v.getValue().rank())));

		leaderboardPlayerColumn.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue()));
		leaderboardPlayerColumn.setCellFactory(_ -> new TableCell<>() {
			private final AsyncImageView avatar = createAvatarView();
			private final Label nameLabel = new Label();
			private final HBox container = new HBox(8, avatar, nameLabel);
			{
				container.setAlignment(Pos.CENTER_LEFT);
			}
			@Override
			protected void updateItem(ChessLeaderboardEntryDTO item, boolean empty)
			{
				super.updateItem(item, empty);
				if (empty || item == null)
				{
					setGraphic(null);
				}
				else
				{
					nameLabel.setText(item.name());
					setAvatar(avatar, item.peer());
					setGraphic(container);
				}
			}
		});

		leaderboardRatingColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(String.valueOf(v.getValue().rating())));
		leaderboardRdColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(String.valueOf(v.getValue().rd())));
		leaderboardGamesColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(String.valueOf(v.getValue().games())));
		leaderboardWinsColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(String.valueOf(v.getValue().wins())));
		leaderboardDrawsColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(String.valueOf(v.getValue().draws())));
		leaderboardLossesColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(String.valueOf(v.getValue().losses())));
		leaderboardStatusColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().status()));
		leaderboardLastPlayedColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(formatDate(v.getValue().lastPlayed())));
	}

	private void applyContactsFilter(String query)
	{
		filteredContacts.setPredicate(row -> {
			if (filterOnlineOnly && ("offline".equalsIgnoreCase(row.status()) || "unknown".equalsIgnoreCase(row.status())))
			{
				return false;
			}
			if (query == null || query.isBlank())
			{
				return true;
			}
			return row.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
		});
	}

	private void loadIdentitiesAndContacts()
	{
		identityClient.getIdentities().collectList().subscribe(identities -> {
			for (var id : identities)
			{
				identitiesById.put(id.getId(), id);
				if (id.getGxsId() != null)
				{
					identitiesByGxsId.put(id.getGxsId().asString(), id);
					if (id.getType() == io.xeres.common.identity.Type.OWN)
					{
						ownGxsId = id.getGxsId().asString();
					}
				}
			}
			fetchContacts();
		}, failure -> log.warn("Unable to load identities for chess", failure));
	}

	private void fetchContacts()
	{
		chessClient.contacts().subscribe(contacts -> Platform.runLater(() -> {
			var contactRows = new ArrayList<ContactRow>();
			for (var contact : contacts)
			{
				var gxsId = contact.gxsId();
				if (gxsId == null || gxsId.isBlank()) continue;
				if (ownGxsId != null && ownGxsId.equalsIgnoreCase(gxsId))
				{
					continue;
				}
				var identity = identitiesByGxsId.get(gxsId);
				var name = (identity != null && identity.getName() != null && !identity.getName().isBlank())
						? identity.getName()
						: (contact.name() != null && !contact.name().isBlank() ? contact.name() : gxsId);
				var status = contact.status() != null ? contact.status() : "unknown";
				contactRows.add(new ContactRow(name, gxsId, status, contact.lastSeen()));
			}
			contactsList.setAll(contactRows);
			updateAvailablePlayers();
		}), failure -> log.warn("Unable to load chess contacts", failure));
	}

	private void refreshActiveGames()
	{
		chessClient.games().subscribe(games -> Platform.runLater(() -> {
			latestGames.clear();
			latestGames.addAll(games);
			var active = games.stream()
					.filter(g -> "ACTIVE".equals(g.status()))
					.toList();
			activeGamesList.setAll(active);
			updateAvailablePlayers();
		}), failure -> log.debug("Active chess games refresh error", failure));
	}

	private void updateAvailablePlayers()
	{
		var mapByPeer = new HashMap<String, ChessGameDTO>();
		for (var game : latestGames)
		{
			mapByPeer.put(game.peer(), game);
		}

		var list = new ArrayList<AvailablePlayerRow>();
		for (var contact : contactsList)
		{
			if (contact.gxsId().isBlank()) continue;
			if (ownGxsId != null && ownGxsId.equalsIgnoreCase(contact.gxsId())) continue;

			var activeGame = mapByPeer.get(contact.gxsId());
			boolean hasGame = activeGame != null && ("ACTIVE".equals(activeGame.status())
					|| "INCOMING".equals(activeGame.status()) || "OUTGOING".equals(activeGame.status()));
			boolean isAvailable = "available".equalsIgnoreCase(contact.status());

			// Mirror RetroChess logic: show in available players only if:
			// - has a pending/active chess game (INCOMING, OUTGOING, ACTIVE), OR
			// - is currently available (confirmed chess presence)
			if (!isAvailable && !hasGame) continue;

			String invStatus = "";
			boolean canReject = false;
			if (hasGame)
			{
				if ("INCOMING".equals(activeGame.status()))
				{
					invStatus = bundle.getString("chess.page.invite.received");
					canReject = true;
				}
				else if ("OUTGOING".equals(activeGame.status()))
				{
					invStatus = bundle.getString("chess.page.invite.sent");
					canReject = false;
				}
				else if ("ACTIVE".equals(activeGame.status()))
				{
					invStatus = bundle.getString("chess.page.playing");
					canReject = false;
				}
			}

			var ratingEntry = ratingsByPeer.get(contact.gxsId());
			int rating = ratingEntry != null ? ratingEntry.rating() : 1500;
			int rd = ratingEntry != null ? ratingEntry.rd() : 350;

			list.add(new AvailablePlayerRow(
					contact.name(),
					contact.gxsId(),
					contact.status(),
					rating,
					rd,
					contact.lastSeen(),
					invStatus,
					canReject,
					activeGame
			));
		}

		// Also include any active or invited games whose peer is not in contactsList (e.g. unsolicited incoming invite)
		var savedGxsIds = contactsList.stream().map(ContactRow::gxsId).collect(java.util.stream.Collectors.toSet());
		for (var game : latestGames)
		{
			if (ownGxsId != null && ownGxsId.equalsIgnoreCase(game.peer()))
			{
				continue;
			}
			if (!savedGxsIds.contains(game.peer()) && ("ACTIVE".equals(game.status())
					|| "INCOMING".equals(game.status()) || "OUTGOING".equals(game.status())))
			{
				var identity = identitiesByGxsId.get(game.peer());
				var name = identity != null ? identity.getName() : game.name();
				var ratingEntry = ratingsByPeer.get(game.peer());
				int rating = ratingEntry != null ? ratingEntry.rating() : 1500;
				int rd = ratingEntry != null ? ratingEntry.rd() : 350;
				String invStatus = "INCOMING".equals(game.status())
						? bundle.getString("chess.page.invite.received")
						: "OUTGOING".equals(game.status())
								? bundle.getString("chess.page.invite.sent")
								: bundle.getString("chess.page.playing");
				boolean canReject = "INCOMING".equals(game.status());
				list.add(new AvailablePlayerRow(
						name,
						game.peer(),
						"available",
						rating,
						rd,
						"",
						invStatus,
						canReject,
						game
				));
			}
		}

		availablePlayersList.setAll(list);
	}


	private void refreshLeaderboard()
	{
		chessClient.leaderboard().subscribe(entries -> Platform.runLater(() -> {
			leaderboardList.setAll(entries);
			for (var entry : entries)
			{
				ratingsByPeer.put(entry.peer(), entry);
			}
			updateAvailablePlayers();
		}), failure -> log.warn("Failed to load chess leaderboard", failure));
	}

	private void refreshHistory()
	{
		chessClient.history().subscribe(
				entries -> Platform.runLater(() -> historyList.setAll(entries)),
				failure -> log.warn("Failed to load chess history", failure)
		);
	}

	private void openGame(ChessGameDTO game)
	{
		windowManager.openChess(game);
	}

	private void showChessProfileDialog()
	{
		var ownPeer = "";
		var ownName = ownCache.getProfileName() != null ? ownCache.getProfileName() : "Player";
		for (var id : identitiesByGxsId.values())
		{
			if (id.getName().equalsIgnoreCase(ownName) && id.getGxsId() != null)
			{
				ownPeer = id.getGxsId().asString();
				break;
			}
		}

		var stats = ratingsByPeer.get(ownPeer);
		int rating = stats != null ? stats.rating() : 1500;
		int rd = stats != null ? stats.rd() : 350;
		int games = stats != null ? stats.games() : 0;
		int wins = stats != null ? stats.wins() : 0;
		int draws = stats != null ? stats.draws() : 0;
		int losses = stats != null ? stats.losses() : 0;
		String status = stats != null ? stats.status() : "Provisional";
		double winRate = games > 0 ? (wins * 100.0) / games : 0.0;

		var dialog = new Dialog<Void>();
		dialog.setTitle(bundle.getString("chess.page.profile.title"));
		dialog.setHeaderText(bundle.getString("chess.page.title") + " - " + ownName);
		dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

		var grid = new GridPane();
		grid.setHgap(16);
		grid.setVgap(10);
		grid.setPadding(new Insets(16));

		var avatar = createAvatarView();
		avatar.setFitWidth(56);
		avatar.setFitHeight(56);
		setAvatar(avatar, ownPeer);

		var infoBox = new VBox(4,
				new Label(ownName, new FontIcon("mdi2a-account")),
				new Label(ownPeer.isEmpty() ? "" : ownPeer)
		);

		grid.add(avatar, 0, 0, 1, 2);
		grid.add(infoBox, 1, 0, 2, 1);

		grid.add(new Label(bundle.getString("chess.page.rating") + ":"), 0, 2);
		grid.add(new Label(String.valueOf(rating)), 1, 2);

		grid.add(new Label(bundle.getString("chess.page.rd") + ":"), 0, 3);
		grid.add(new Label(String.valueOf(rd)), 1, 3);

		grid.add(new Label(bundle.getString("chess.page.status") + ":"), 0, 4);
		grid.add(new Label(status), 1, 4);

		grid.add(new Label(bundle.getString("chess.page.games") + ":"), 2, 2);
		grid.add(new Label(String.valueOf(games)), 3, 2);

		grid.add(new Label(bundle.getString("chess.page.wins") + " / " + bundle.getString("chess.page.draws") + " / " + bundle.getString("chess.page.losses") + ":"), 2, 3);
		grid.add(new Label(wins + " / " + draws + " / " + losses), 3, 3);

		grid.add(new Label(bundle.getString("chess.page.profile.win-rate") + ":"), 2, 4);
		grid.add(new Label(String.format(Locale.ROOT, "%.1f%%", winRate)), 3, 4);

		dialog.getDialogPane().setContent(grid);
		dialog.showAndWait();
	}

	private void showAddChessContactDialog()
	{
		var dialog = new Dialog<String>();
		dialog.setTitle(bundle.getString("chess.page.contacts.add-title"));
		dialog.setHeaderText(bundle.getString("chess.page.contacts.add-hint"));

		var addBtnType = new ButtonType(bundle.getString("add"), ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(addBtnType, ButtonType.CANCEL);

		var searchField = new CustomTextField();
		searchField.setPromptText(bundle.getString("chess.page.contacts.search"));
		searchField.setLeft(new FontIcon("mdi2m-magnify"));

		var existingGxsIds = contactsList.stream().map(ContactRow::gxsId).map(String::toLowerCase).collect(java.util.stream.Collectors.toSet());
		var ownName = ownCache.getProfileName() != null ? ownCache.getProfileName() : "";

		var observableIdentities = FXCollections.observableArrayList(
				identitiesByGxsId.values().stream()
						.filter(id -> id.getGxsId() != null)
						.filter(id -> !existingGxsIds.contains(id.getGxsId().asString().toLowerCase()))
						.filter(id -> ownName.isBlank() || !id.getName().equalsIgnoreCase(ownName))
						.toList()
		);

		var filteredIdentities = new FilteredList<>(observableIdentities, _ -> true);
		searchField.textProperty().addListener((_, _, text) -> {
			var query = text != null ? text.trim().toLowerCase(Locale.ROOT) : "";
			filteredIdentities.setPredicate(id -> query.isBlank()
					|| id.getName().toLowerCase(Locale.ROOT).contains(query)
					|| id.getGxsId().asString().toLowerCase(Locale.ROOT).contains(query));
		});

		var identityListView = new ListView<>(filteredIdentities);
		identityListView.setPrefHeight(220);
		identityListView.setCellFactory(_ -> new ListCell<>() {
			private final AsyncImageView avatar = createAvatarView();
			private final Label nameLabel = new Label();
			private final Label idLabel = new Label();
			private final VBox textContainer = new VBox(2, nameLabel, idLabel);
			private final HBox rowContainer = new HBox(8, avatar, textContainer);
			{
				rowContainer.setAlignment(Pos.CENTER_LEFT);
				idLabel.getStyleClass().add("text-muted");
				idLabel.setStyle("-fx-font-size: 10px;");
			}

			@Override
			protected void updateItem(Identity item, boolean empty)
			{
				super.updateItem(item, empty);
				if (empty || item == null || item.getGxsId() == null)
				{
					setGraphic(null);
				}
				else
				{
					nameLabel.setText(item.getName());
					var gxsStr = item.getGxsId().asString();
					idLabel.setText(gxsStr.substring(0, Math.min(16, gxsStr.length())) + "...");
					setAvatar(avatar, gxsStr);
					setGraphic(rowContainer);
				}
			}
		});

		var customIdField = new CustomTextField();
		customIdField.setPromptText("GXS ID (32 hex characters)");
		var customBox = new VBox(4, new Label(bundle.getString("chess.page.contacts.custom-id")), customIdField);

		var content = new VBox(10, searchField, identityListView, customBox);
		content.setPadding(new Insets(10));
		dialog.getDialogPane().setContent(content);

		var addBtn = dialog.getDialogPane().lookupButton(addBtnType);
		addBtn.setDisable(true);

		Runnable updateAddBtn = () -> {
			var customText = customIdField.getText() != null ? customIdField.getText().trim() : "";
			boolean validCustom = customText.matches("^[0-9a-fA-F]{32}$");
			boolean selectedIdentity = identityListView.getSelectionModel().getSelectedItem() != null;
			addBtn.setDisable(!validCustom && !selectedIdentity);
		};

		identityListView.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> updateAddBtn.run());
		customIdField.textProperty().addListener((_, _, _) -> updateAddBtn.run());

		identityListView.setOnMouseClicked(e -> {
			if (e.getClickCount() == 2 && identityListView.getSelectionModel().getSelectedItem() != null)
			{
				var selected = identityListView.getSelectionModel().getSelectedItem();
				if (selected.getGxsId() != null)
				{
					dialog.setResult(selected.getGxsId().asString());
					dialog.close();
				}
			}
		});

		dialog.setResultConverter(btn -> {
			if (btn == addBtnType)
			{
				var customText = customIdField.getText() != null ? customIdField.getText().trim() : "";
				if (customText.matches("^[0-9a-fA-F]{32}$"))
				{
					return customText;
				}
				var selected = identityListView.getSelectionModel().getSelectedItem();
				if (selected != null && selected.getGxsId() != null)
				{
					return selected.getGxsId().asString();
				}
			}
			return null;
		});

		var result = dialog.showAndWait();
		result.ifPresent(gxsId -> {
			chessClient.addContact(gxsId).subscribe(
					_ -> Platform.runLater(this::loadIdentitiesAndContacts),
					failure -> Platform.runLater(() -> showError("Failed to add chess contact: " + failure.getMessage()))
			);
		});
	}

	private AsyncImageView createAvatarView()
	{
		var view = new AsyncImageView(url -> generalClient.getImage(url).block(), imageCache);
		view.setFitWidth(28);
		view.setFitHeight(28);
		view.setPreserveRatio(true);
		return view;
	}

	private void setAvatar(AsyncImageView view, String peer)
	{
		if (peer != null && peer.matches("[0-9a-fA-F]{32}"))
		{
			view.setUrl(RemoteUtils.getControlUrl() + PathConfig.IDENTITIES_PATH + "/image?find=true&gxsId=" + peer);
		}
		else
		{
			view.setUrl(null);
		}
	}

	private <T> TableCell<T, String> createChessStatusCell()
	{
		return new TableCell<>()
		{
			private final Circle dot = new Circle(4);
			private final Label label = new Label();
			private final HBox container = new HBox(6, dot, label);
			{
				container.setAlignment(Pos.CENTER_LEFT);
			}

			@Override
			protected void updateItem(String item, boolean empty)
			{
				super.updateItem(item, empty);
				if (empty || item == null)
				{
					setGraphic(null);
				}
				else
				{
					var lower = item.toLowerCase(Locale.ROOT);
					switch (lower)
					{
						case "available" -> {
							dot.setFill(Color.web("#22c55e"));
							label.setText(bundle.getString("chess.page.status.available"));
						}
						case "busy" -> {
							dot.setFill(Color.web("#f59e0b"));
							label.setText(bundle.getString("chess.page.busy"));
						}
						case "playing" -> {
							dot.setFill(Color.web("#3b82f6"));
							label.setText(bundle.getString("chess.page.playing"));
						}
						case "checking" -> {
							dot.setFill(Color.web("#eab308"));
							label.setText(bundle.getString("chess.page.status.checking"));
						}
						case "offline" -> {
							dot.setFill(Color.web("#94a3b8"));
							label.setText(bundle.getString("chess.page.status.offline"));
						}
						default -> {
							dot.setFill(Color.web("#94a3b8"));
							label.setText(bundle.getString("chess.page.status.unknown"));
						}
					}
					setGraphic(container);
				}
			}
		};
	}

	private String formatPlayers(ChessGameDTO game)
	{
		if (game == null) return "";
		var own = ownCache.getProfileName() != null ? ownCache.getProfileName() : "Player";
		return game.white() ? (own + " vs " + game.name()) : (game.name() + " vs " + own);
	}

	private String formatDate(String iso)
	{
		if (iso == null || iso.isBlank()) return "";
		try
		{
			return DATE_FORMATTER.format(Instant.parse(iso));
		}
		catch (Exception ignored)
		{
			return iso;
		}
	}

	private String formatResult(String status)
	{
		if (status == null) return "";
		try
		{
			return bundle.getString("chess.status." + status);
		}
		catch (Exception ignored)
		{
			return status;
		}
	}

	private void showError(String message)
	{
		new Alert(Alert.AlertType.ERROR, message).show();
	}

	Button getOnlineFilterButton()
	{
		return onlineFilterButton;
	}

	Button getAddContactButton()
	{
		return addContactButton;
	}
}
