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
import io.xeres.common.dto.chess.ChessHistorySummaryDTO;
import io.xeres.ui.support.chess.ChessSettings;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/// Read-only saved-game viewer. It has no chess client or network actions.
public final class ChessGameReviewWindow extends HBox
{
	private final ChessGameDTO game;
	private final ChessSettings settings;
	private final ResourceBundle bundle;
	private final Label[] squares = new Label[64];
	private final TableView<MoveRow> moves = new TableView<>();
	private final TableColumn<MoveRow, String> white = new TableColumn<>();
	private final TableColumn<MoveRow, String> black = new TableColumn<>();
	private final Label position = new Label();
	private final Button first = new Button("|\u25c0");
	private final Button previous = new Button("\u25c0");
	private final Button next = new Button("\u25b6");
	private final Button last = new Button("\u25b6|");
	private int ply;
	private boolean selecting;
	private record MoveRow(int number, String white, String black) { }

	public static void open(ChessGameDTO game, ChessHistorySummaryDTO summary, ResourceBundle bundle, ChessSettings settings)
	{
		var view = new ChessGameReviewWindow(game, summary, bundle, settings);
		var stage = new Stage();
		stage.setTitle(bundle.getString("chess.review") + ": " + summary.whiteName() + " vs " + summary.blackName());
		stage.setScene(new Scene(view, 850, 590));
		stage.setMinWidth(640);
		stage.setMinHeight(440);
		javafx.beans.value.ChangeListener<io.xeres.ui.support.chess.ChessBoardTheme> listener = (_, _, _) -> view.showPly(view.ply);
		settings.themeProperty().addListener(listener);
		stage.setOnHidden(_ -> settings.themeProperty().removeListener(listener));
		stage.show();
	}

	ChessGameReviewWindow(ChessGameDTO game, ChessHistorySummaryDTO summary, ResourceBundle bundle, ChessSettings settings)
	{
		if (game.positions().size() != game.moves().size() + 1) throw new IllegalArgumentException("Incomplete game history");
		this.game = game;
		this.settings = settings;
		this.bundle = bundle;
		setPadding(new Insets(12));
		setSpacing(16);
		setAlignment(Pos.CENTER);
		var board = new GridPane();
		board.setId("reviewBoard");
		var size = Bindings.max(0, Bindings.min(widthProperty().subtract(320), heightProperty().subtract(24)));
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
		var players = new Label(bundle.getString("chess.side-white") + ": " + summary.whiteName() + "\n" +
				bundle.getString("chess.side-black") + ": " + summary.blackName() + "\n" +
				bundle.getString("chess.history-result") + ": " + result(game) + "\n" + bundle.getString("chess.status." + game.status()));
		players.setId("reviewPlayers");
		players.setWrapText(true);
		var number = new TableColumn<MoveRow, String>("#");
		number.setMinWidth(30);
		number.setMaxWidth(42);
		number.setCellValueFactory(value -> new ReadOnlyStringWrapper(Integer.toString(value.getValue().number())));
		white.setText(bundle.getString("chess.side-white"));
		white.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().white()));
		black.setText(bundle.getString("chess.side-black"));
		black.setCellValueFactory(value -> new ReadOnlyStringWrapper(value.getValue().black()));
		moves.getColumns().setAll(number, white, black);
		for (var column : moves.getColumns())
		{
			column.setSortable(false);
			column.setReorderable(false);
			column.setStyle("-fx-alignment: CENTER;");
		}
		for (var i = 0; i < game.moves().size(); i += 2)
		{
			moves.getItems().add(new MoveRow(i / 2 + 1, game.moves().get(i), i + 1 < game.moves().size() ? game.moves().get(i + 1) : ""));
		}
		moves.setId("reviewMoves");
		moves.setMinHeight(0);
		moves.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
		moves.getSelectionModel().setCellSelectionEnabled(true);
		moves.getSelectionModel().getSelectedCells().addListener((javafx.collections.ListChangeListener<TablePosition>) _ -> {
			if (selecting || moves.getSelectionModel().getSelectedCells().isEmpty()) return;
			var cell = moves.getSelectionModel().getSelectedCells().getFirst();
			if (cell.getTableColumn() == number) return;
			var index = cell.getRow() * 2 + (cell.getTableColumn() == black ? 2 : 1);
			if (index <= game.moves().size()) showPly(index);
		});
		first.setId("reviewFirst");
		previous.setId("reviewPrevious");
		next.setId("reviewNext");
		last.setId("reviewLast");
		first.setOnAction(_ -> showPly(0));
		previous.setOnAction(_ -> showPly(ply - 1));
		next.setOnAction(_ -> showPly(ply + 1));
		last.setOnAction(_ -> showPly(game.moves().size()));
		var buttons = java.util.List.of(first, previous, next, last);
		var labels = java.util.List.of("chess.first", "chess.previous", "chess.next", "chess.latest");
		for (var i = 0; i < buttons.size(); i++)
		{
			buttons.get(i).setTooltip(new Tooltip(bundle.getString(labels.get(i))));
			buttons.get(i).setAccessibleText(bundle.getString(labels.get(i)));
			buttons.get(i).setMinWidth(42);
		}
		var navigation = new HBox(12, first, previous, next, last);
		navigation.setAlignment(Pos.CENTER);
		position.setId("reviewPosition");
		var close = new Button(bundle.getString("close"));
		close.setId("reviewClose");
		close.setCancelButton(true);
		close.setOnAction(_ -> getScene().getWindow().hide());
		var footer = new HBox(close);
		footer.setAlignment(Pos.CENTER_RIGHT);
		var side = new VBox(8, players, moves, navigation, position, footer);
		side.setMinWidth(280);
		side.setPrefWidth(280);
		side.setMaxWidth(280);
		side.prefHeightProperty().bind(size);
		side.maxHeightProperty().bind(size);
		VBox.setVgrow(moves, Priority.ALWAYS);
		getChildren().addAll(board, side);
		showPly(0);
	}

	void showPly(int value)
	{
		ply = Math.clamp(value, 0, game.moves().size());
		var state = game.positions().get(ply);
		for (var i = 0; i < 64; i++)
		{
			var square = squares[i];
			var piece = state.squares().charAt(i);
			var color = (i / 8 + i % 8) % 2 == 0 ? settings.getTheme().light() : settings.getTheme().dark();
			square.setStyle("-fx-background-color: " + color + ";");
			square.setAccessibleText("" + (char) ('a' + i % 8) + (8 - i / 8) + " " + piece);
			if (!Character.valueOf(piece).equals(square.getUserData()))
			{
				square.setUserData(piece);
				if (piece == '.') square.setGraphic(null);
				else
				{
					var graphic = new ChessPieceView(piece);
					var size = Bindings.min(square.widthProperty(), square.heightProperty()).multiply(0.92);
					graphic.prefWidthProperty().bind(size);
					graphic.prefHeightProperty().bind(size);
					graphic.maxWidthProperty().bind(size);
					graphic.maxHeightProperty().bind(size);
					square.setGraphic(graphic);
				}
			}
		}
		selecting = true;
		if (ply == 0) moves.getSelectionModel().clearSelection();
		else
		{
			moves.getSelectionModel().select((ply - 1) / 2, ply % 2 == 1 ? white : black);
			moves.scrollTo((ply - 1) / 2);
		}
		selecting = false;
		position.setText(MessageFormat.format(bundle.getString("chess.review-position"), ply, game.moves().size()));
		first.setDisable(ply == 0);
		previous.setDisable(ply == 0);
		next.setDisable(ply == game.moves().size());
		last.setDisable(ply == game.moves().size());
	}

	private static String result(ChessGameDTO game)
	{
		return switch (game.status())
		{
			case "DRAW" -> "1/2-1/2";
			case "CHECKMATE" -> game.whiteToMove() ? "0-1" : "1-0";
			case "RESIGNED" -> game.white() ? "0-1" : "1-0";
			case "OPPONENT_RESIGNED" -> game.white() ? "1-0" : "0-1";
			default -> "*";
		};
	}
}
