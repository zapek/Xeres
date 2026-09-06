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
	@FXML private Label players;
	@FXML private Label status;
	@FXML private Label detail;
	@FXML private TextArea moves;
	@FXML private Button abort;
	@FXML private Button resign;
	@FXML private Button draw;
	@FXML private MenuItem newGame;
	@FXML private MenuItem positionDetails;
	private final ChessClient client;
	private final ResourceBundle bundle;
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
	private final javafx.animation.PauseTransition noticeTimer = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(6));

	public ChessWindowController(ChessClient client, ResourceBundle bundle, ChessGameDTO game)
	{
		this.client = client;
		this.bundle = bundle;
		this.game = game;
	}

	@Override
	public void initialize()
	{
		noticeTimer.setOnFinished(_ -> {
			noticeExpired = true;
			detail.setText("");
			detail.setVisible(false);
			detail.setManaged(false);
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
		boardContainer.styleProperty().bind(Bindings.concat("-fx-background-color: #f4b886; -fx-font-size: ", Bindings.max(9, boardSize.multiply(0.024)), "px;"));
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
		abort.setOnAction(_ -> act(game.status().equals("ACTIVE") ? "abort" : "decline"));
		resign.setOnAction(_ -> act("resign"));
		draw.setOnAction(_ -> {
			var offer = bundle.getString("chess.draw");
			var choice = new ChoiceDialog<>(offer, offer, bundle.getString("chess.repetition"), bundle.getString("chess.fifty"));
			choice.initOwner(board.getScene().getWindow());
			choice.setHeaderText(bundle.getString("chess.draw-menu"));
			choice.showAndWait().ifPresent(value -> act(value.equals(offer) ? "draw_offer" : value.equals(bundle.getString("chess.repetition")) ? "draw_repetition" : "draw_fifty_move"));
		});
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
		update(game);
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
		if (!value.fen().equals(game.fen()))
		{
			selected = null;
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
		players.setText(game.name() + " \u2014 " + bundle.getString(game.white() ? "chess.white" : "chess.black"));
		players.setTooltip(new Tooltip(game.localIdentity() + " \u2192 " + game.peer()));
		status.setText(game.status().equals("ACTIVE")
				? bundle.getString(game.white() == game.whiteToMove() ? "chess.your-turn" : "chess.their-turn") +
						(game.inCheck() ? " \u2014 " + bundle.getString("chess.check") : "")
				: bundle.getString("chess.status." + game.status()));
		if (!lastDetail.equals(game.detail()))
		{
			noticeTimer.stop();
			lastDetail = game.detail();
			noticeExpired = false;
			if (bundle.containsKey("chess.notice." + lastDetail))
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
		detail.setManaged(detail.isVisible());
		var history = new StringBuilder();
		for (var i = 0; i < game.moves().size(); i++)
		{
			if (i % 2 == 0)
			{
				history.append(i / 2 + 1).append(". ");
			}
			history.append(game.moves().get(i)).append(i % 2 == 0 ? "  " : "\n");
		}
		moves.setText(history.toString());
		var active = game.status().equals("ACTIVE");
		abort.setDisable(pending || !(active || game.status().equals("INCOMING") || game.status().equals("OUTGOING")));
		resign.setDisable(pending || !active);
		draw.setDisable(pending || !active || game.outgoingDraw());
		newGame.setDisable(pending || active || game.status().equals("INCOMING") || game.status().equals("OUTGOING"));
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
		board.getScene().getWindow().setOnCloseRequest(event -> {
			if (!java.util.List.of("ACTIVE", "INCOMING", "OUTGOING", "DESYNCHRONIZED").contains(game.status()))
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
			previous.close();
		}
	}

	private void refreshPrompt()
	{
		var kind = game.status().equals("INCOMING") ? "invite" : game.status().equals("ACTIVE") && game.incomingDraw() ? "draw" : null;
		if (prompt != null && !java.util.Objects.equals(kind, promptKind))
		{
			closePrompt();
		}
		if (kind == null || pending || prompt != null || board.getScene() == null || board.getScene().getWindow() == null || !board.getScene().getWindow().isShowing())
		{
			return;
		}
		var invitation = kind.equals("invite");
		var yes = new ButtonType(bundle.getString(invitation ? "chess.accept" : "chess.accept-draw"), ButtonBar.ButtonData.OK_DONE);
		var no = new ButtonType(bundle.getString(invitation ? "chess.decline" : "chess.decline-draw"), ButtonBar.ButtonData.CANCEL_CLOSE);
		var dialog = new Alert(Alert.AlertType.CONFIRMATION, game.name(), yes, no);
		dialog.initOwner(board.getScene().getWindow());
		dialog.setTitle(bundle.getString("chess.title"));
		dialog.setHeaderText(bundle.getString(invitation ? "chess.status.INCOMING" : "chess.draw-offer"));
		prompt = dialog;
		promptKind = kind;
		dialog.setOnHidden(_ -> {
			if (prompt != dialog)
			{
				return;
			}
			prompt = null;
			promptKind = null;
			act(invitation ? dialog.getResult() == yes ? "accept" : "decline" : dialog.getResult() == yes ? "draw_accept" : "draw_decline");
		});
		dialog.show();
	}

	private void paint()
	{
		for (var at = 0; at < 64; at++)
		{
			var square = square(at);
			var piece = game.squares().charAt(at);
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
			button.setAccessibleText(square + " " + game.squares().charAt(at));
			button.setTooltip(new Tooltip(square));
			var target = selected != null && game.legalMoves().stream().anyMatch(move -> move.startsWith(selected + square));
			var checkedKing = game.inCheck() && piece == (game.whiteToMove() ? 'K' : 'k');
			var lastMove = game.moves().isEmpty() ? "" : game.moves().getLast();
			var lastMoveSquare = lastMove.length() >= 4 &&
					(square.equals(lastMove.substring(0, 2)) || square.equals(lastMove.substring(2, 4)));
			var lightSquare = (at / 8 + at % 8) % 2 == 0;
			var background = checkedKing ? "#ef7777" : square.equals(selected) ? "#e9c46a" :
					lastMoveSquare ? (lightSquare ? "#cdd26a" : "#aaa23a") : lightSquare ? "#f0d9b5" : "#b58863";
			if (target)
			{
				// Percentage stops keep move markers proportional when the board resizes.
				background += piece == '.'
						? ", radial-gradient(center 50% 50%, radius 50%, rgba(0, 0, 0, 0.20) 0%, rgba(0, 0, 0, 0.20) 34%, transparent 36%, transparent 100%)"
						: ", radial-gradient(center 50% 50%, radius 50%, transparent 0%, transparent 78%, rgba(0, 0, 0, 0.24) 80%, rgba(0, 0, 0, 0.24) 98%, transparent 100%)";
			}
			button.setStyle("-fx-opacity: 1; -fx-padding: 0; -fx-text-fill: #18222d; -fx-background-radius: 0; -fx-background-color: " + background + ";");
			button.setDisable(pending || game.legalMoves().isEmpty());
		}
	}

	private void select(int index)
	{
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
			detail.setText(bundle.getString("chess.error") + " " + failure.getMessage());
			detail.setVisible(true);
			detail.setManaged(true);
		});
	}
}
