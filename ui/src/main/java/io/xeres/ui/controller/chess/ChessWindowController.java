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

import io.xeres.common.dto.chess.ChessGameDTO;
import io.xeres.ui.client.ChessClient;
import io.xeres.ui.controller.WindowController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.RowConstraints;
import javafx.beans.binding.Bindings;
import net.rgielen.fxweaver.core.FxmlView;

import java.util.ResourceBundle;

@FxmlView("/view/chess/chess_window.fxml")
public class ChessWindowController implements WindowController
{
	@FXML private GridPane boardContainer;
	@FXML private GridPane rankLabels;
	@FXML private GridPane fileLabels;
	@FXML private GridPane board;
	@FXML private javafx.scene.layout.VBox opponentCard;
	@FXML private javafx.scene.layout.VBox ownCard;
	@FXML private Label opponentColor;
	@FXML private Label ownColor;
	@FXML private Label opponentName;
	@FXML private Label ownName;
	@FXML private StackPane boardArea;
	@FXML private javafx.scene.layout.HBox gameLayout;
	@FXML private Label status;
	@FXML private Label detail;
	@FXML private TableView<MoveRow> moves;
	@FXML private Button firstMove;
	@FXML private Button previousMove;
	@FXML private Button nextMove;
	@FXML private Button latestMove;
	@FXML private MenuItem loadHistory;
	private int reviewPly = -1;
	private boolean updatingMoves;
	private TableColumn<MoveRow, String> whiteMoveColumn;
	private TableColumn<MoveRow, String> blackMoveColumn;
	private record MoveRow(int number, String white, String black) { }
	private boolean archive;
	@FXML private Button abort;
	@FXML private Button resign;
	@FXML private Button draw;
	@FXML private MenuItem newGame;
	@FXML private MenuItem positionDetails;
	@FXML private MenuItem settingsMenu;
	private final ChessClient client;
	private final io.xeres.ui.support.chess.ChessSettings chessSettings;
	private final javafx.beans.value.ChangeListener<io.xeres.ui.support.chess.ChessBoardTheme> themeListener = (_, _, _) -> paint();
	private final io.xeres.ui.support.sound.SoundPlayerService soundPlayer;
	private final ResourceBundle bundle;
	private io.xeres.ui.client.GeneralClient avatarClient;
	private io.xeres.ui.custom.asyncimage.ImageCache avatarCache;
	private final Button[] squares = new Button[64];
	private final Label[] rankCoordinateLabels = new Label[8];
	private final Label[] fileCoordinateLabels = new Label[8];
	private ChessGameDTO game;
	private String selected;
	private boolean pending;
	private Alert prompt;
	private Alert debugDialog;
	private TextArea debugText;
	private String promptKind;
	private String lastDetail = "";
	private boolean noticeExpired;
	private boolean resultDismissed;
	private final javafx.animation.PauseTransition noticeTimer = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(6));

	public ChessWindowController(ChessClient client, ResourceBundle bundle, ChessGameDTO game, io.xeres.ui.support.sound.SoundPlayerService soundPlayer, io.xeres.ui.support.chess.ChessSettings chessSettings)
	{
		this.client = client;
		this.soundPlayer = soundPlayer;
		this.chessSettings = chessSettings;
		this.bundle = bundle;
		this.game = game;
	}

	public void setOpenSettingsAction(Runnable action)
	{
		settingsMenu.setOnAction(_ -> action.run());
	}

	@Override
	public void initialize()
	{
		noticeTimer.setOnFinished(_ -> {
			noticeExpired = true;
			detail.setText("");
			detail.setVisible(false);
		});
		var boardSize = Bindings.max(0, Bindings.min(boardArea.widthProperty().subtract(406), boardArea.heightProperty()));
		gameLayout.prefWidthProperty().bind(boardSize.add(406));
		gameLayout.maxWidthProperty().bind(boardSize.add(406));
		gameLayout.prefHeightProperty().bind(boardSize);
		gameLayout.maxHeightProperty().bind(boardSize);
		boardContainer.setMinSize(0, 0);
		boardContainer.prefWidthProperty().bind(boardSize);
		boardContainer.prefHeightProperty().bind(boardSize);
		boardContainer.maxWidthProperty().bind(boardSize);
		boardContainer.maxHeightProperty().bind(boardSize);
		boardContainer.styleProperty().bind(Bindings.createStringBinding(() -> "-fx-background-color: " + chessSettings.getTheme().border() + "; -fx-font-size: " + Math.max(9, boardSize.doubleValue() * 0.024) + "px;", boardSize, chessSettings.themeProperty()));
		board.setMinSize(0, 0);
		board.setPrefSize(0, 0);
		board.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		var sideCol = new ColumnConstraints();
		sideCol.setPercentWidth(20.0 / 552.0 * 100.0);
		sideCol.setHalignment(javafx.geometry.HPos.CENTER);
		var centerCol = new ColumnConstraints();
		centerCol.setPercentWidth(512.0 / 552.0 * 100.0);
		var rightCol = new ColumnConstraints();
		rightCol.setPercentWidth(20.0 / 552.0 * 100.0);
		boardContainer.getColumnConstraints().addAll(sideCol, centerCol, rightCol);

		var topRow = new RowConstraints();
		topRow.setPercentHeight(20.0 / 552.0 * 100.0);
		var centerRow = new RowConstraints();
		centerRow.setPercentHeight(512.0 / 552.0 * 100.0);
		var bottomRow = new RowConstraints();
		bottomRow.setPercentHeight(20.0 / 552.0 * 100.0);
		bottomRow.setValignment(javafx.geometry.VPos.CENTER);
		boardContainer.getRowConstraints().addAll(topRow, centerRow, bottomRow);

		var topRightSpacer = new javafx.scene.layout.Region();
		topRightSpacer.setMinSize(0, 0);
		boardContainer.add(topRightSpacer, 2, 0);

		for (var i = 0; i < 8; i++)
		{
			var column = new ColumnConstraints();
			column.setPercentWidth(12.5);
			board.getColumnConstraints().add(column);
			var row = new RowConstraints();
			row.setPercentHeight(12.5);
			board.getRowConstraints().add(row);
		}
		rankLabels.setMinSize(0, 0);
		rankLabels.setPrefSize(0, 0);
		rankLabels.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		javafx.scene.layout.GridPane.setHgrow(rankLabels, javafx.scene.layout.Priority.ALWAYS);
		javafx.scene.layout.GridPane.setVgrow(rankLabels, javafx.scene.layout.Priority.ALWAYS);
		var rankCol = new ColumnConstraints();
		rankCol.setPercentWidth(100.0);
		rankCol.setHalignment(javafx.geometry.HPos.CENTER);
		rankLabels.getColumnConstraints().add(rankCol);
		for (var i = 0; i < 8; i++)
		{
			var row = new RowConstraints();
			row.setPercentHeight(12.5);
			row.setValignment(javafx.geometry.VPos.CENTER);
			rankLabels.getRowConstraints().add(row);
			var label = new Label();
			label.setMinSize(0, 0);
			label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			label.setAlignment(javafx.geometry.Pos.CENTER);
			label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
			label.setStyle("-fx-text-fill: #2d241e; -fx-font-weight: bold;");
			javafx.scene.layout.GridPane.setHalignment(label, javafx.geometry.HPos.CENTER);
			javafx.scene.layout.GridPane.setValignment(label, javafx.geometry.VPos.CENTER);
			javafx.scene.layout.GridPane.setHgrow(label, javafx.scene.layout.Priority.ALWAYS);
			javafx.scene.layout.GridPane.setVgrow(label, javafx.scene.layout.Priority.ALWAYS);
			rankCoordinateLabels[i] = label;
			rankLabels.add(label, 0, i);
		}
		fileLabels.setMinSize(0, 0);
		fileLabels.setPrefSize(0, 0);
		fileLabels.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		javafx.scene.layout.GridPane.setHgrow(fileLabels, javafx.scene.layout.Priority.ALWAYS);
		javafx.scene.layout.GridPane.setVgrow(fileLabels, javafx.scene.layout.Priority.ALWAYS);
		var fileRow = new RowConstraints();
		fileRow.setPercentHeight(100.0);
		fileRow.setValignment(javafx.geometry.VPos.CENTER);
		fileLabels.getRowConstraints().add(fileRow);
		for (var i = 0; i < 8; i++)
		{
			var col = new ColumnConstraints();
			col.setPercentWidth(12.5);
			col.setHalignment(javafx.geometry.HPos.CENTER);
			fileLabels.getColumnConstraints().add(col);
			var label = new Label();
			label.setMinSize(0, 0);
			label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			label.setAlignment(javafx.geometry.Pos.CENTER);
			label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
			label.setStyle("-fx-text-fill: #2d241e; -fx-font-weight: bold;");
			javafx.scene.layout.GridPane.setHalignment(label, javafx.geometry.HPos.CENTER);
			javafx.scene.layout.GridPane.setValignment(label, javafx.geometry.VPos.CENTER);
			javafx.scene.layout.GridPane.setHgrow(label, javafx.scene.layout.Priority.ALWAYS);
			javafx.scene.layout.GridPane.setVgrow(label, javafx.scene.layout.Priority.ALWAYS);
			fileCoordinateLabels[i] = label;
			fileLabels.add(label, i, 0);
		}
		for (var row = 0; row < 8; row++)
		{
			for (var col = 0; col < 8; col++)
			{
				var index = game.white() ? row * 8 + col : 63 - row * 8 - col;
				var button = new Button();
				button.setMinSize(0, 0);
				button.setPrefSize(0, 0);
				button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
				button.setOnAction(_ -> select(index));
				squares[index] = button;
				board.add(button, col, row);
			}
		}
		abort.setOnAction(_ -> act(game.status().equals("ACTIVE") ? "abort" : game.status().equals("INCOMING") ? "decline" : "leave"));
		resign.setOnAction(_ -> act("resign"));
		draw.setOnAction(_ -> act("draw"));
		newGame.setOnAction(_ -> client.invite(game.peer()).subscribe(value -> Platform.runLater(() -> update(value)), this::error));
		positionDetails.setOnAction(_ -> {
			if (debugDialog != null)
			{
				debugDialog.getDialogPane().getScene().getWindow().requestFocus();
				return;
			}
			debugDialog = new Alert(Alert.AlertType.INFORMATION);
			debugDialog.initOwner(board.getScene().getWindow());
			debugDialog.setTitle(bundle.getString("chess.position"));
			debugDialog.setHeaderText(bundle.getString("chess.position"));
			debugText = new TextArea(debugReport());
			debugText.setEditable(false);
			debugText.setPrefSize(850, 500);
			debugDialog.setResizable(true);
			var copy = new ButtonType(bundle.getString("chess.copy-report"), ButtonBar.ButtonData.LEFT);
			debugDialog.getButtonTypes().add(copy);
			debugDialog.getDialogPane().lookupButton(copy).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
				var content = new javafx.scene.input.ClipboardContent();
				content.putString(debugReport());
				javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
				event.consume();
			});
			debugDialog.getDialogPane().setContent(debugText);
			debugDialog.setOnHidden(_ -> {
				debugDialog = null;
				debugText = null;
			});
			debugDialog.show();
		});
		firstMove.setOnAction(_ -> review(0));
		previousMove.setOnAction(_ -> review(displayPly() - 1));
		nextMove.setOnAction(_ -> review(displayPly() + 1));
		latestMove.setOnAction(_ -> review(game.moves().size()));
		firstMove.setTooltip(new Tooltip(bundle.getString("chess.first")));
		previousMove.setTooltip(new Tooltip(bundle.getString("chess.previous")));
		nextMove.setTooltip(new Tooltip(bundle.getString("chess.next")));
		latestMove.setTooltip(new Tooltip(bundle.getString("chess.latest")));
		var numberColumn = new TableColumn<MoveRow, String>("#");
		numberColumn.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyStringWrapper(Integer.toString(value.getValue().number())));
		numberColumn.setMinWidth(30);
		numberColumn.setMaxWidth(42);
		whiteMoveColumn = new TableColumn<>(bundle.getString("chess.side-white"));
		whiteMoveColumn.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyStringWrapper(value.getValue().white()));
		blackMoveColumn = new TableColumn<>(bundle.getString("chess.side-black"));
		blackMoveColumn.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyStringWrapper(value.getValue().black()));
		moves.getColumns().setAll(numberColumn, whiteMoveColumn, blackMoveColumn);
		for (var column : moves.getColumns())
		{
			column.setSortable(false);
			column.setReorderable(false);
			column.setStyle("-fx-alignment: CENTER;");
		}
		moves.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
		moves.getSelectionModel().setCellSelectionEnabled(true);
		moves.getSelectionModel().getSelectedCells().addListener((javafx.collections.ListChangeListener<TablePosition>) _ -> {
			if (updatingMoves || moves.getSelectionModel().getSelectedCells().isEmpty()) return;
			var cell = moves.getSelectionModel().getSelectedCells().getFirst();
			if (cell.getTableColumn() == numberColumn) return;
			var ply = cell.getRow() * 2 + (cell.getTableColumn() == blackMoveColumn ? 2 : 1);
			if (ply <= game.moves().size()) review(ply);
		});
		loadHistory.setOnAction(_ -> loadHistory());
		update(game);
	}

	private int displayPly()
	{
		return reviewPly < 0 ? game.moves().size() : Math.min(reviewPly, game.moves().size());
	}

	private void review(int ply)
	{
		selected = null;
		reviewPly = ply >= game.moves().size() ? -1 : Math.max(0, ply);
		update(game);
		moves.scrollTo(Math.max(0, (displayPly() - 1) / 2));
	}

	private void loadHistory()
	{
		browseHistory(board.getScene().getWindow(), client, bundle, soundPlayer, chessSettings, avatarClient, avatarCache);
	}

	public static void browseHistory(javafx.stage.Window owner, ChessClient client, ResourceBundle bundle,
			io.xeres.ui.support.sound.SoundPlayerService soundPlayer, io.xeres.ui.support.chess.ChessSettings chessSettings,
			io.xeres.ui.client.GeneralClient avatarClient, io.xeres.ui.custom.asyncimage.ImageCache avatarCache)
	{
		client.history().subscribe(saved -> Platform.runLater(() -> showHistory(owner, saved, client, bundle, soundPlayer, chessSettings, avatarClient, avatarCache)), ChessWindowController::historyError);
	}

	private static void historyError(Throwable failure)
	{
		Platform.runLater(() -> new Alert(Alert.AlertType.ERROR, failure.getMessage()).show());
	}

	private static void showHistory(javafx.stage.Window owner, java.util.List<io.xeres.common.dto.chess.ChessHistorySummaryDTO> saved, ChessClient client, ResourceBundle bundle,
			io.xeres.ui.support.sound.SoundPlayerService soundPlayer, io.xeres.ui.support.chess.ChessSettings chessSettings,
			io.xeres.ui.client.GeneralClient avatarClient, io.xeres.ui.custom.asyncimage.ImageCache avatarCache)
	{
		var dialog = new Dialog<io.xeres.common.dto.chess.ChessHistorySummaryDTO>();
		dialog.initOwner(owner);
		dialog.setTitle(bundle.getString("chess.load-history"));
		dialog.setResizable(true);
		var table = new TableView<io.xeres.common.dto.chess.ChessHistorySummaryDTO>();
		table.getItems().setAll(saved);
		table.setPrefSize(800, 450);
		table.setFixedCellSize(40);
		table.setPlaceholder(new Label(bundle.getString("chess.history-empty")));
		var date = new TableColumn<io.xeres.common.dto.chess.ChessHistorySummaryDTO, String>(bundle.getString("chess.history-date"));
		date.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyStringWrapper(java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT).withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.parse(value.getValue().startedAt()))));
		var white = new TableColumn<io.xeres.common.dto.chess.ChessHistorySummaryDTO, io.xeres.common.dto.chess.ChessHistorySummaryDTO>(bundle.getString("chess.side-white"));
		white.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyObjectWrapper<>(value.getValue()));
		white.setCellFactory(_ -> new ChessHistoryIdentityCell(true, avatarClient, avatarCache));
		white.setComparator(java.util.Comparator.comparing(io.xeres.common.dto.chess.ChessHistorySummaryDTO::whiteName));
		var black = new TableColumn<io.xeres.common.dto.chess.ChessHistorySummaryDTO, io.xeres.common.dto.chess.ChessHistorySummaryDTO>(bundle.getString("chess.side-black"));
		black.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyObjectWrapper<>(value.getValue()));
		black.setCellFactory(_ -> new ChessHistoryIdentityCell(false, avatarClient, avatarCache));
		black.setComparator(java.util.Comparator.comparing(io.xeres.common.dto.chess.ChessHistorySummaryDTO::blackName));
		var count = new TableColumn<io.xeres.common.dto.chess.ChessHistorySummaryDTO, String>(bundle.getString("chess.history-moves"));
		count.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyStringWrapper(Integer.toString((value.getValue().moves() + 1) / 2)));
		var result = new TableColumn<io.xeres.common.dto.chess.ChessHistorySummaryDTO, String>(bundle.getString("chess.history-result"));
		result.setCellValueFactory(value -> new javafx.beans.property.ReadOnlyStringWrapper(bundle.getString("chess.status." + value.getValue().status())));
		table.getColumns().setAll(date, white, black, result, count);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
		var open = new ButtonType(bundle.getString("chess.history-open"), ButtonBar.ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().setAll(open, ButtonType.CANCEL);
		dialog.getDialogPane().lookupButton(open).disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
		dialog.getDialogPane().setContent(table);
		table.setRowFactory(_ -> {
			var row = new TableRow<io.xeres.common.dto.chess.ChessHistorySummaryDTO>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && !row.isEmpty())
				{
					dialog.setResult(row.getItem());
					dialog.close();
				}
			});
			return row;
		});
		dialog.setResultConverter(button -> button == open ? table.getSelectionModel().getSelectedItem() : null);
		dialog.showAndWait().ifPresent(item -> client.history(item.id()).subscribe(game -> Platform.runLater(() -> ChessGameReviewWindow.open(game, item, bundle, chessSettings)), ChessWindowController::historyError));
	}

	private String debugReport()
	{
		return "Xeres chess debug report\nStatus: " + game.status() + "\nLocal side: " + (game.white() ? "White" : "Black") +
				"\nFEN: " + game.fen() + "\nHash: " + game.hash() + "\nDetail: " + game.detail() +
				"\n\nMoves (UCI):\n" + String.join(" ", game.moves()) +
				"\n\nEvents (UTC, last 1000; TX QUEUED does not confirm opponent receipt):\n" + String.join("\n", game.debugEvents());
	}

	public void update(ChessGameDTO value)
	{
		if (value.moves().size() < game.moves().size()) reviewPly = -1;
		if (value.status().equals("ACTIVE"))
		{
			resultDismissed = false;
		}
		if (!value.fen().equals(game.fen()))
		{
			selected = null;
		}
		var moveSound = ChessMoveSound.forUpdate(game, value);
		if (moveSound != null)
		{
			soundPlayer.play(moveSound);
		}
		game = value;
		if (debugText != null && !debugText.getText().equals(debugReport()))
		{
			debugText.setText(debugReport());
		}
		opponentColor.setText(bundle.getString(game.white() ? "chess.side-black" : "chess.side-white"));
		ownColor.setText(bundle.getString(game.white() ? "chess.side-white" : "chess.side-black"));
		opponentName.setText(game.name());
		opponentName.setTooltip(new Tooltip(game.peer()));
		ownName.setTooltip(new Tooltip(game.localIdentity()));
		status.setText(game.status().equals("ACTIVE")
				? bundle.getString(game.white() == game.whiteToMove() ? "chess.your-turn" : "chess.their-turn") +
						(game.inCheck() ? " \u2014 " + bundle.getString("chess.check") : "")
				: bundle.getString("chess.status." + game.status()));
		if (!lastDetail.equals(game.detail()))
		{
			noticeTimer.stop();
			lastDetail = game.detail();
			noticeExpired = false;
			if (!game.status().equals("DRAW") && !game.outgoingRematch() && bundle.containsKey("chess.notice." + lastDetail))
			{
				noticeTimer.playFromStart();
			}
		}
		detail.setText(game.detail().equals("CONNECTION_INTERRUPTED") ? bundle.getString("chess.connection") : game.detail());
		if (bundle.containsKey("chess.notice." + game.detail()))
		{
			detail.setText(bundle.getString("chess.notice." + game.detail()));
		}
		if (noticeExpired)
		{
			detail.setText("");
		}
		detail.setVisible(!detail.getText().isEmpty());
		var entries = new java.util.ArrayList<MoveRow>();
		for (var i = 0; i < game.moves().size(); i += 2)
		{
			entries.add(new MoveRow(i / 2 + 1, game.moves().get(i), i + 1 < game.moves().size() ? game.moves().get(i + 1) : ""));
		}
		updatingMoves = true;
		if (!moves.getItems().equals(entries)) moves.getItems().setAll(entries);
		var ply = displayPly();
		if (ply == 0) moves.getSelectionModel().clearSelection();
		else moves.getSelectionModel().select((ply - 1) / 2, ply % 2 == 1 ? whiteMoveColumn : blackMoveColumn);
		updatingMoves = false;
		var active = !archive && game.status().equals("ACTIVE");
		abort.setDisable(archive || pending || !(active || game.status().equals("INCOMING") || game.status().equals("OUTGOING")));
		resign.setDisable(pending || !active);
		draw.setDisable(pending || !active || game.outgoingDraw());
		newGame.setDisable(archive || pending || active || game.status().equals("INCOMING") || game.status().equals("OUTGOING"));
		updateCoordinates();
		paint();
		refreshPrompt();
	}

	private void updateCoordinates()
	{
		for (var i = 0; i < 8; i++)
		{
			rankCoordinateLabels[i].setText(String.valueOf(game.white() ? 8 - i : 1 + i));
			fileCoordinateLabels[i].setText(String.valueOf((char) (game.white() ? 'a' + i : 'h' - i)));
		}
	}

	public void showPlayerProfiles(io.xeres.ui.client.GeneralClient generalClient, io.xeres.ui.custom.asyncimage.ImageCache imageCache,
			io.xeres.ui.client.IdentityClient identityClient, String localName)
	{
		avatarClient = generalClient;
		avatarCache = imageCache;
		ownName.setText(localName);
		addAvatar(opponentCard, game.peer(), generalClient, imageCache);
		addAvatar(ownCard, game.localIdentity(), generalClient, imageCache);
		identityClient.findByGxsId(io.xeres.common.id.GxsId.fromString(game.localIdentity())).next()
				.subscribe(identity -> Platform.runLater(() -> ownName.setText(identity.getName())), _ -> { });
	}

	private void addAvatar(javafx.scene.layout.VBox card, String identity,
			io.xeres.ui.client.GeneralClient generalClient, io.xeres.ui.custom.asyncimage.ImageCache imageCache)
	{
		var avatar = new io.xeres.ui.custom.asyncimage.AsyncImageView(url -> generalClient.getImage(url).block(), imageCache);
		avatar.setPreserveRatio(true);
		var avatarSize = Bindings.min(128, boardContainer.heightProperty().multiply(0.25));
		avatar.fitWidthProperty().bind(avatarSize);
		avatar.fitHeightProperty().bind(avatarSize);
		card.getChildren().add(1, avatar);
		avatar.setUrl(io.xeres.common.util.RemoteUtils.getControlUrl() + io.xeres.common.rest.PathConfig.IDENTITIES_PATH + "/image?find=true&gxsId=" + identity);
	}

	@Override
	public void onShown()
	{
		chessSettings.themeProperty().addListener(themeListener);
		paint();
		board.getScene().getWindow().setOnCloseRequest(event -> {
			if (archive || !java.util.List.of("ACTIVE", "INCOMING", "OUTGOING", "DESYNCHRONIZED").contains(game.status()))
			{
				return;
			}
			event.consume();
			if (pending)
			{
				return;
			}
			pending = true;
			closePrompt();
			update(game);
			client.action(game.peer(), game.status().equals("ACTIVE") ? "abort" : "leave")
					.timeout(java.time.Duration.ofSeconds(10))
					.subscribe(value -> Platform.runLater(() -> {
						pending = false;
						update(value);
						board.getScene().getWindow().hide();
					}), this::error);
		});
		refreshPrompt();
	}

	@Override
	public void onHidden()
	{
		chessSettings.themeProperty().removeListener(themeListener);
		noticeTimer.stop();
		if (debugDialog != null)
		{
			debugDialog.close();
		}
		closePrompt();
	}

	private void closePrompt()
	{
		var previous = prompt;
		prompt = null;
		promptKind = null;
		if (previous != null)
		{
			if (previous.getResult() == null)
			{
				previous.setResult(ButtonType.CLOSE);
			}
			if (previous.getDialogPane() != null && previous.getDialogPane().getScene() != null && previous.getDialogPane().getScene().getWindow() != null)
			{
				previous.getDialogPane().getScene().getWindow().hide();
			}
			previous.close();
		}
	}

	private boolean isGameOver(String status)
	{
		return status.equals("CHECKMATE") || status.equals("DRAW") || status.equals("RESIGNED") || status.equals("OPPONENT_RESIGNED");
	}

	private void refreshPrompt()
	{
		var kind = game.incomingRematch()
				? "rematch"
				: game.status().equals("INCOMING")
				? "invite"
				: game.status().equals("ACTIVE") && game.incomingDraw()
				? "draw"
				: isGameOver(game.status()) && !game.outgoingRematch() && !resultDismissed
				? "result"
				: null;
		if (prompt != null && !java.util.Objects.equals(kind, promptKind))
		{
			closePrompt();
		}
		if (archive || kind == null || pending || prompt != null || board.getScene() == null || board.getScene().getWindow() == null || !board.getScene().getWindow().isShowing())
		{
			return;
		}
		var result = kind.equals("result");
		var rematchPrompt = kind.equals("rematch");
		var invitation = kind.equals("invite");

		var leaveBtn = new ButtonType(bundle.getString("chess.leave"), ButtonBar.ButtonData.LEFT);
		var rematchBtn = new ButtonType(bundle.getString("chess.rematch"), ButtonBar.ButtonData.RIGHT);
		var yes = new ButtonType(bundle.getString(rematchPrompt ? "chess.accept" : invitation ? "chess.accept" : "chess.accept-draw"), ButtonBar.ButtonData.OK_DONE);
		var no = new ButtonType(bundle.getString(rematchPrompt ? "chess.decline" : invitation ? "chess.decline" : "chess.decline-draw"), ButtonBar.ButtonData.CANCEL_CLOSE);

		var dialog = result
				? new Alert(Alert.AlertType.INFORMATION, "", leaveBtn, rematchBtn)
				: new Alert(Alert.AlertType.CONFIRMATION, game.name(), yes, no);
		dialog.initOwner(board.getScene().getWindow());

		if (result)
		{
			dialog.setTitle(bundle.getString("chess.game-over"));
			dialog.setHeaderText(null);
			dialog.setGraphic(null);
			String titleText;
			String messageText;
			if (game.status().equals("CHECKMATE"))
			{
				var won = game.white() != game.whiteToMove();
				titleText = bundle.getString(won ? "chess.you-won" : "chess.you-lost");
				messageText = bundle.getString("chess.by-checkmate");
			}
			else if (game.status().equals("OPPONENT_RESIGNED"))
			{
				titleText = bundle.getString("chess.you-won");
				messageText = bundle.getString("chess.won-message");
			}
			else if (game.status().equals("RESIGNED"))
			{
				titleText = bundle.getString("chess.you-lost");
				messageText = bundle.getString("chess.lost-message");
			}
			else if (game.status().equals("DRAW"))
			{
				titleText = bundle.getString("chess.status.DRAW");
				messageText = bundle.getString("chess.draw-ended");
			}
			else
			{
				titleText = bundle.getString("chess.game-over");
				messageText = bundle.getString("chess.status." + game.status());
			}

			var titleLabel = new Label(titleText);
			titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-alignment: center; -fx-text-alignment: center;");
			titleLabel.setMaxWidth(Double.MAX_VALUE);
			titleLabel.setAlignment(javafx.geometry.Pos.CENTER);

			var messageLabel = new Label(messageText);
			messageLabel.setStyle("-fx-font-size: 14px; -fx-alignment: center; -fx-text-alignment: center;");
			messageLabel.setMaxWidth(Double.MAX_VALUE);
			messageLabel.setAlignment(javafx.geometry.Pos.CENTER);

			var box = new javafx.scene.layout.VBox(10, titleLabel, messageLabel);
			box.setAlignment(javafx.geometry.Pos.CENTER);
			box.setPadding(new javafx.geometry.Insets(16, 24, 12, 24));
			box.setPrefWidth(320);
			dialog.getDialogPane().setContent(box);
		}
		else if (rematchPrompt)
		{
			dialog.setTitle(bundle.getString("chess.rematch"));
			dialog.setHeaderText(bundle.getString("chess.rematch-offer"));
		}
		else
		{
			dialog.setTitle(bundle.getString("chess.title"));
			dialog.setHeaderText(bundle.getString(invitation ? "chess.status.INCOMING" : "chess.draw-offer"));
		}

		prompt = dialog;
		promptKind = kind;
		dialog.setOnHidden(_ -> {
			if (prompt != dialog)
			{
				return;
			}
			prompt = null;
			promptKind = null;
			if (result)
			{
				resultDismissed = true;
				if (dialog.getResult() == rematchBtn)
				{
					act("rematch");
				}
				else if (dialog.getResult() == leaveBtn)
				{
					act("leave");
					if (board.getScene() != null && board.getScene().getWindow() != null)
					{
						board.getScene().getWindow().hide();
					}
				}
			}
			else if (rematchPrompt)
			{
				act(dialog.getResult() == yes ? "rematch_accept" : "rematch_decline");
			}
			else
			{
				act(invitation ? dialog.getResult() == yes ? "accept" : "decline" : dialog.getResult() == yes ? "draw_accept" : "draw_decline");
			}
		});
		dialog.setOnCloseRequest(_ -> {
			if (dialog.getResult() == null)
			{
				dialog.setResult(result ? leaveBtn : no);
			}
		});
		dialog.show();
	}

	private void paint()
	{
		var ply = displayPly();
		var historical = reviewPly >= 0 && game.positions().size() == game.moves().size() + 1;
		var position = historical ? game.positions().get(ply) : new io.xeres.common.dto.chess.ChessBoardDTO(game.squares(), game.whiteToMove(), game.inCheck());
		firstMove.setDisable(ply == 0 || game.positions().isEmpty());
		previousMove.setDisable(firstMove.isDisabled());
		nextMove.setDisable(ply == game.moves().size() || game.positions().isEmpty());
		latestMove.setDisable(nextMove.isDisabled());
		if (historical || archive) status.setText(bundle.getString("chess.review") + " " + ply + " / " + game.moves().size());
		for (var at = 0; at < 64; at++)
		{
			var square = square(at);
			var piece = position.squares().charAt(at);
			var button = squares[at];
			var display = game.white() ? at : 63 - at;
			GridPane.setRowIndex(button, display / 8);
			GridPane.setColumnIndex(button, display % 8);
			button.setText("");
			if (!Character.valueOf(piece).equals(button.getUserData()))
			{
				button.setUserData(piece);
				if (piece == '.')
				{
					button.setGraphic(null);
				}
				else
				{
					var artwork = new ChessPieceView(piece);
					var size = Bindings.min(button.widthProperty(), button.heightProperty()).multiply(0.95);
					artwork.prefWidthProperty().bind(size);
					artwork.prefHeightProperty().bind(size);
					artwork.maxWidthProperty().bind(size);
					artwork.maxHeightProperty().bind(size);
					button.setGraphic(artwork);
				}
			}
			button.setAccessibleText(square + " " + position.squares().charAt(at));
			button.setTooltip(new Tooltip(square));
			var target = !historical && !archive && selected != null && game.legalMoves().stream().anyMatch(move -> move.startsWith(selected + square));
			var checkedKing = position.inCheck() && piece == (position.whiteToMove() ? 'K' : 'k');
			var lastMove = ply == 0 ? "" : game.moves().get(ply - 1);
			var lastMoveSquare = lastMove.length() >= 4 &&
					(square.equals(lastMove.substring(0, 2)) || square.equals(lastMove.substring(2, 4)));
			var lightSquare = (at / 8 + at % 8) % 2 == 0;
			var background = checkedKing ? "#ef7777" : square.equals(selected) ? "#e9c46a" :
					lastMoveSquare ? (lightSquare ? "#cdd26a" : "#aaa23a") : lightSquare ? chessSettings.getTheme().light() : chessSettings.getTheme().dark();
			if (target)
			{
				// Percentage stops keep move markers proportional when the board resizes.
				background += piece == '.'
						? ", radial-gradient(center 50% 50%, radius 50%, rgba(0, 0, 0, 0.20) 0%, rgba(0, 0, 0, 0.20) 34%, transparent 36%, transparent 100%)"
						: ", radial-gradient(center 50% 50%, radius 50%, transparent 0%, transparent 78%, rgba(0, 0, 0, 0.24) 80%, rgba(0, 0, 0, 0.24) 98%, transparent 100%)";
			}
			button.setStyle("-fx-opacity: 1; -fx-padding: 0; -fx-text-fill: #18222d; -fx-background-radius: 0; -fx-background-color: " + background + ";");
			button.setDisable(archive || historical || pending || game.legalMoves().isEmpty());
		}
	}

	private void select(int index)
	{
		if (archive || reviewPly >= 0) return;
		var target = square(index);
		if (selected != null)
		{
			var options = game.legalMoves().stream().filter(move -> move.startsWith(selected + target)).toList();
			if (!options.isEmpty())
			{
				var move = options.getFirst();
				if (options.size() > 1)
				{
					var dialog = new ChoiceDialog<>("Q", "Q", "R", "B", "N");
					dialog.setHeaderText(bundle.getString("chess.promotion"));
					var promotion = dialog.showAndWait();
					if (promotion.isEmpty())
					{
						return;
					}
					move = selected + target + promotion.get().toLowerCase(java.util.Locale.ROOT);
				}
				selected = null;
				act(move);
				return;
			}
		}
		selected = game.legalMoves().stream().anyMatch(move -> move.startsWith(target)) ? target : null;
		paint();
	}

	private String square(int index)
	{
		return "" + (char) ('a' + index % 8) + (8 - index / 8);
	}

	private void act(String action)
	{
		pending = true;
		update(game);
		client.action(game.peer(), action).subscribe(value -> Platform.runLater(() -> {
			pending = false;
			update(value);
		}), this::error);
	}

	private void error(Throwable failure)
	{
		Platform.runLater(() -> {
			pending = false;
			update(game);
			noticeTimer.stop();
			var message = failure.getMessage();
			if (failure instanceof org.springframework.web.reactive.function.client.WebClientResponseException responseException)
			{
				var body = responseException.getResponseBodyAsString();
				if (body != null && !body.isBlank())
				{
					try
					{
						var tree = new tools.jackson.databind.json.JsonMapper().readTree(body);
						if (tree.has("message") && !tree.get("message").asText().isBlank())
						{
							message = tree.get("message").asText();
						}
					}
					catch (Exception ignored)
					{
					}
				}
			}
			detail.setText(bundle.getString("chess.error") + " " + message);
			detail.setVisible(true);
		});
	}
}
