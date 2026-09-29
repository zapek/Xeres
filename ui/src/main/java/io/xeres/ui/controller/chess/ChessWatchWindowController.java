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

import io.xeres.common.dto.chess.ChessActiveGameDTO;
import io.xeres.common.dto.chess.ChessWatchDTO;
import io.xeres.ui.client.ChessClient;
import io.xeres.ui.controller.WindowController;
import io.xeres.ui.support.chess.ChessBoardTheme;
import io.xeres.ui.support.chess.ChessSettings;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.ResourceBundle;

/// Live, read-only board. No player actions are exposed by this controller.
public final class ChessWatchWindowController implements WindowController
{
	private static final Logger log = LoggerFactory.getLogger(ChessWatchWindowController.class);
	private final ChessClient client;
	private ChessActiveGameDTO match;
	private final ResourceBundle bundle;
	private final ChessSettings settings;
	private final Label[] squares = new Label[64];
	private final Label players = new Label();
	private final Label status = new Label();
	private final ListView<String> moves = new ListView<>();
	private final Button retry = new Button();
	private final Timeline refresh = new Timeline(new KeyFrame(Duration.seconds(1), _ -> poll()));
	private final ChangeListener<ChessBoardTheme> themeListener = (_, _, _) -> paint();
	private ChessWatchDTO game;
	private boolean flipped;
	private boolean closed;
	private boolean pending;
	private boolean started;
	@FXML private HBox content;

	public ChessWatchWindowController(ChessClient client, ChessActiveGameDTO match, ResourceBundle bundle, ChessSettings settings)
	{
		this.client = client;
		this.match = match;
		this.bundle = bundle;
		this.settings = settings;
	}

	@Override
	public void initialize()
	{
		var board = new GridPane();
		board.setId("watchBoard");
		var size = Bindings.max(0, Bindings.min(content.widthProperty().subtract(324), content.heightProperty().subtract(24)));
		board.setMinSize(0, 0);
		board.prefWidthProperty().bind(size);
		board.prefHeightProperty().bind(size);
		board.maxWidthProperty().bind(size);
		board.maxHeightProperty().bind(size);
		for (var i = 0; i < 8; i++)
		{
			var column = new ColumnConstraints();
			column.setPercentWidth(12.5);
			board.getColumnConstraints().add(column);
			var row = new RowConstraints();
			row.setPercentHeight(12.5);
			board.getRowConstraints().add(row);
		}
		for (var i = 0; i < 64; i++)
		{
			var square = new Label();
			square.setAlignment(Pos.CENTER);
			square.setMinSize(0, 0);
			square.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			squares[i] = square;
			board.add(square, i % 8, i / 8);
		}
		players.setId("watchPlayers");
		players.setWrapText(true);
		players.setText(match.playerName() + " vs " + match.opponentName());
		status.setId("watchStatus");
		status.setWrapText(true);
		status.setText(bundle.getString("chess.watch.waiting"));
		moves.setId("watchMoves");
		moves.setMinHeight(0);
		var flip = new Button(bundle.getString("chess.watch.flip"));
		flip.setId("watchFlip");
		flip.setOnAction(_ -> {
			flipped = !flipped;
			paint();
		});
		retry.setText(bundle.getString("chess.watch.retry"));
		retry.setId("watchRetry");
		retry.setVisible(false);
		retry.managedProperty().bind(retry.visibleProperty());
		retry.setOnAction(_ -> startWatching());
		var close = new Button(bundle.getString("close"));
		close.setId("watchClose");
		close.setCancelButton(true);
		close.setOnAction(_ -> content.getScene().getWindow().hide());
		var buttons = new FlowPane(8, 8, flip, retry, close);
		buttons.setAlignment(Pos.CENTER_RIGHT);
		var side = new VBox(12, players, status, moves, buttons);
		side.setMinWidth(280);
		side.setPrefWidth(280);
		side.setMaxWidth(280);
		side.prefHeightProperty().bind(size);
		side.maxHeightProperty().bind(size);
		VBox.setVgrow(moves, Priority.ALWAYS);
		content.getChildren().setAll(board, side);
		refresh.setCycleCount(Timeline.INDEFINITE);
		paint();
	}

	@Override
	public void onShown()
	{
		closed = false;
		settings.themeProperty().addListener(themeListener);
		startWatching();
		refresh.play();
	}

	private void startWatching()
	{
		if (pending || closed) return;
		pending = true;
		var requestedMatch = match;
		retry.setVisible(false);
		status.setText(bundle.getString("chess.watch.waiting"));
		// Let the start request finish even if closed, then unregister the resulting subscription.
		client.watch(match.host(), match.gameId()).timeout(java.time.Duration.ofSeconds(15)).subscribe(value -> Platform.runLater(() -> {
			pending = false;
			started = true;
			if (closed) leave();
			else if (requestedMatch != match) startWatching();
			else update(value);
		}), failure -> Platform.runLater(() -> requestFailed(requestedMatch, failure)));
	}

	private void poll()
	{
		if (!started || pending || closed || game != null && !java.util.List.of("WAITING", "LIVE").contains(game.status())) return;
		pending = true;
		var requestedMatch = match;
		client.watchedGame(match.host()).timeout(java.time.Duration.ofSeconds(10)).subscribe(value -> Platform.runLater(() -> {
			pending = false;
			if (closed) return;
			if (requestedMatch != match) startWatching();
			else update(value);
		}), failure -> Platform.runLater(() -> requestFailed(requestedMatch, failure)));
	}

	public void showMatch(ChessActiveGameDTO value)
	{
		if (match.gameId().equals(value.gameId()) && match.opponent().equals(value.opponent())) return;
		match = value;
		game = null;
		started = false;
		players.setText(value.playerName() + " vs " + value.opponentName());
		moves.getItems().clear();
		paint();
		startWatching();
	}

	private void requestFailed(ChessActiveGameDTO requestedMatch, Throwable failure)
	{
		pending = false;
		if (!closed && requestedMatch != match) startWatching();
		else failed(failure);
	}

	private void failed(Throwable failure)
	{
		pending = false;
		started = false;
		log.debug("Unable to watch chess game", failure);
		if (closed) return;
		status.setText(bundle.getString("chess.watch.unavailable"));
		retry.setVisible(true);
	}

	void update(ChessWatchDTO value)
	{
		game = value;
		if (!value.whiteName().isBlank())
		{
			players.setText(bundle.getString("chess.side-white") + ": " + value.whiteName() + "\n" +
					bundle.getString("chess.side-black") + ": " + value.blackName());
		}
		var key = switch (value.status())
		{
			case "LIVE" -> value.whiteToMove() ? "chess.watch.white-turn" : "chess.watch.black-turn";
			case "ENDED" -> "chess.watch.ended";
			case "UNAVAILABLE" -> "chess.watch.unavailable";
			default -> "chess.watch.waiting";
		};
		status.setText(bundle.getString(key));
		retry.setVisible(value.status().equals("UNAVAILABLE"));
		var rows = new ArrayList<String>();
		for (var i = 0; i < value.moves().size(); i += 2)
		{
			rows.add((i / 2 + 1) + ".  " + value.moves().get(i) + (i + 1 < value.moves().size() ? "    " + value.moves().get(i + 1) : ""));
		}
		if (!moves.getItems().equals(rows))
		{
			moves.getItems().setAll(rows);
			if (!rows.isEmpty()) moves.scrollTo(rows.size() - 1);
		}
		paint();
	}

	private void paint()
	{
		for (var i = 0; i < 64; i++)
		{
			var index = flipped ? 63 - i : i;
			var square = squares[i];
			var piece = game == null || game.squares().length() != 64 ? '.' : game.squares().charAt(index);
			var color = (index / 8 + index % 8) % 2 == 0 ? settings.getTheme().light() : settings.getTheme().dark();
			var highlight = game != null && (index == game.lastFrom() || index == game.lastTo());
			square.setStyle("-fx-background-color: " + color + ";" + (highlight ? "-fx-border-color: #c4a000; -fx-border-width: 3;" : ""));
			square.setAccessibleText("" + (char) ('a' + index % 8) + (8 - index / 8) + " " + piece);
			if (!Character.valueOf(piece).equals(square.getUserData()))
			{
				square.setUserData(piece);
				if (piece == '.') square.setGraphic(null);
				else
				{
					var graphic = new ChessPieceView(piece);
					var size = Bindings.min(square.widthProperty(), square.heightProperty()).multiply(0.9);
					graphic.prefWidthProperty().bind(size);
					graphic.prefHeightProperty().bind(size);
					graphic.maxWidthProperty().bind(size);
					graphic.maxHeightProperty().bind(size);
					square.setGraphic(graphic);
				}
			}
		}
	}

	@Override
	public void onHidden()
	{
		closed = true;
		refresh.stop();
		settings.themeProperty().removeListener(themeListener);
		leave();
	}

	private void leave()
	{
		client.leaveWatch(match.host()).subscribe(_ -> {}, failure -> log.debug("Unable to leave chess watch", failure));
	}
}
