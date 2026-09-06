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
import io.xeres.ui.FXTest;
import io.xeres.ui.client.ChessClient;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ChessWindowControllerTest extends FXTest
{
	@Test
	void invitationBoardLoadsInEverySupportedLanguage() throws Exception
	{
		var checked = new CompletableFuture<Void>();
		Platform.runLater(() -> {
			try
			{
				for (var language : List.of("en", "fr", "es", "ru", "zh"))
				{
					var bundle = ResourceBundle.getBundle("i18n.messages", Locale.forLanguageTag(language));
					var game = new ChessGameDTO("22".repeat(16), "Opponent", "11".repeat(16), "INCOMING", false,
							true, "rnbqkbnrpppppppp" + ".".repeat(32) + "PPPPPPPPRNBQKBNR", "initial", "hash", List.of(), List.of(), false, false, "", List.of(), false);
					var loader = new FXMLLoader(getClass().getResource("/view/chess/chess_window.fxml"), bundle);
					loader.setController(new ChessWindowController(mock(ChessClient.class), bundle, game));
					Parent root = loader.load();
					new Scene(root, 760, 720);
					root.applyCss();
					root.layout();
					var boardContainer = (GridPane) root.lookup("#boardContainer");
					var board = (GridPane) root.lookup("#board");
					var rankLabels = (GridPane) root.lookup("#rankLabels");
					var fileLabels = (GridPane) root.lookup("#fileLabels");
					assertEquals(64, board.getChildren().size());
					assertEquals(32, board.getChildren().stream().map(node -> (Button) node)
							.filter(button -> button.getGraphic() instanceof ChessPieceView).count());
					assertTrue(board.getChildren().stream().map(node -> (Button) node).allMatch(button -> button.getText().isEmpty()));
					assertTrue(boardContainer.getStyle().contains("#f4b886"));
					assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8"),
							rankLabels.getChildren().stream().map(node -> ((javafx.scene.control.Label) node).getText()).toList());
					assertEquals(List.of("h", "g", "f", "e", "d", "c", "b", "a"),
							fileLabels.getChildren().stream().map(node -> ((javafx.scene.control.Label) node).getText()).toList());
					assertTrue(rankLabels.getChildren().stream().allMatch(node ->
							Math.abs(((javafx.scene.control.Label) node).getWidth() - rankLabels.getWidth()) < 1.0));
					assertTrue(fileLabels.getChildren().stream().allMatch(node ->
							Math.abs(((javafx.scene.control.Label) node).getHeight() - fileLabels.getHeight()) < 1.0));
					var boardArea = (javafx.scene.layout.StackPane) root.lookup("#boardArea");
					assertJoinedLayout(root, boardContainer, boardArea);
					var initialSize = boardContainer.getWidth();
					root.resize(1100, 850);
					root.layout();
					assertTrue(boardContainer.getWidth() > initialSize);
					assertJoinedLayout(root, boardContainer, boardArea);
					for (var size : new int[][]{{1910, 1016}, {710, 650}, {900, 500}})
					{
						root.resize(size[0], size[1]);
						root.layout();
						assertJoinedLayout(root, boardContainer, boardArea);
					}
					var active = new ChessGameDTO(game.peer(), game.name(), game.localIdentity(), "ACTIVE", true,
							true, game.squares(), game.fen(), game.hash(), List.of(), List.of("e2e3", "e2e4"), false, false, "", List.of(), false);
					((ChessWindowController) loader.getController()).update(active);
					assertEquals(List.of("8", "7", "6", "5", "4", "3", "2", "1"),
							rankLabels.getChildren().stream().map(node -> ((javafx.scene.control.Label) node).getText()).toList());
					assertEquals(List.of("a", "b", "c", "d", "e", "f", "g", "h"),
							fileLabels.getChildren().stream().map(node -> ((javafx.scene.control.Label) node).getText()).toList());
					var source = (Button) board.getChildren().stream().filter(node -> node.getAccessibleText().startsWith("e2 ")).findFirst().orElseThrow();
					source.fire();
					root.applyCss();
					var target = (Button) board.getChildren().stream().filter(node -> node.getAccessibleText().startsWith("e4 ")).findFirst().orElseThrow();
					assertEquals(2, target.getBackground().getFills().size());
					assertInstanceOf(javafx.scene.paint.RadialGradient.class, target.getBackground().getFills().getLast().getFill());
					((ChessWindowController) loader.getController()).update(game);
					assertFalse(((Button) root.lookup("#abort")).isDisabled());
					assertNull(root.lookup("#accept"));
					assertNull(root.lookup("#acceptDraw"));
					assertNull(root.lookup("#fen"));
					assertTrue(((Button) root.lookup("#resign")).isDisabled());
					assertTrue(board.getChildren().stream().allMatch(node -> node.isDisabled()));
					assertTrue(board.getChildren().stream().allMatch(node -> node.getOpacity() == 1.0));
					assertEquals(7, GridPane.getRowIndex(board.getChildren().stream()
							.filter(node -> node.getAccessibleText().startsWith("a8 ")).findFirst().orElseThrow()));
				}
				checked.complete(null);
			}
			catch (Throwable failure)
			{
				checked.completeExceptionally(failure);
			}
		});
		checked.get(90, TimeUnit.SECONDS);
	}

	private void assertJoinedLayout(Parent root, GridPane board, javafx.scene.layout.StackPane area)
	{
		var sidebar = (javafx.scene.layout.VBox) root.lookup("#gameSidebar");
		var players = (javafx.scene.layout.VBox) root.lookup("#playerPanel");
		var playerBounds = players.localToScene(players.getLayoutBounds());
		var boardBounds = board.localToScene(board.getLayoutBounds());
		var sidebarBounds = sidebar.localToScene(sidebar.getLayoutBounds());
		var areaBounds = area.localToScene(area.getLayoutBounds());
		assertEquals(board.getWidth(), board.getHeight(), 1.0);
		assertEquals(16, boardBounds.getMinX() - playerBounds.getMaxX(), 1.0);
		assertEquals(boardBounds.getMinY(), playerBounds.getMinY(), 1.0);
		assertEquals(boardBounds.getMaxY(), playerBounds.getMaxY(), 1.0);
		assertEquals(16, sidebarBounds.getMinX() - boardBounds.getMaxX(), 1.0);
		assertEquals(boardBounds.getMinY(), sidebarBounds.getMinY(), 1.0);
		assertEquals(boardBounds.getMaxY(), sidebarBounds.getMaxY(), 1.0);
		assertEquals(areaBounds.getMinY(), boardBounds.getMinY(), 1.0);
	}
}
