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
import io.xeres.ui.FXTest;
import io.xeres.ui.client.ChessClient;
import io.xeres.ui.support.chess.ChessBoardTheme;
import io.xeres.ui.support.chess.ChessSettings;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChessWatchWindowControllerTest extends FXTest
{
	private final ChessActiveGameDTO match = new ChessActiveGameDTO("22".repeat(16), "match", "Alice", "33".repeat(16), "Bob", false);
	private final String initial = "rnbqkbnrpppppppp" + ".".repeat(32) + "PPPPPPPPRNBQKBNR";

	@Test
	void liveBoardUpdatesFlipsAndEndsWithoutPlayerControlsInAllLanguages() throws Exception
	{
		var completed = new CompletableFuture<Void>();
		Platform.runLater(() -> {
			try
			{
				for (var language : List.of("en", "fr", "es", "ru", "zh"))
				{
					var client = mock(ChessClient.class);
					var settings = settings();
					var bundle = ResourceBundle.getBundle("i18n.messages", Locale.forLanguageTag(language));
					var controller = new ChessWatchWindowController(client, match, bundle, settings);
					var root = load(controller, bundle);
					new Scene(root, 850, 590);
					controller.update(state(initial, List.of(), "LIVE"));
					root.applyCss();
					root.layout();
					var board = (GridPane) root.lookup("#watchBoard");
					assertEquals(64, board.getChildren().size());
					assertTrue(board.getChildren().stream().allMatch(Label.class::isInstance));
					assertEquals(board.getWidth(), board.getHeight(), 1);
					assertNull(root.lookup("#resign"));
					assertNull(root.lookup("#abort"));
					assertNull(root.lookup("#draw"));
					assertTrue(((Label) root.lookup("#watchPlayers")).getText().contains("Alice"));
					var moved = initial.toCharArray();
					moved[52] = '.';
					moved[36] = 'P';
					controller.update(state(new String(moved), List.of("e2e4"), "LIVE"));
					assertEquals("e4 P", board.getChildren().get(36).getAccessibleText());
					assertEquals(1, ((ListView<?>) root.lookup("#watchMoves")).getItems().size());
					((Button) root.lookup("#watchFlip")).fire();
					assertEquals("e4 P", board.getChildren().get(27).getAccessibleText());
					controller.update(state(new String(moved), List.of("e2e4"), "ENDED"));
					assertEquals(bundle.getString("chess.watch.ended"), ((Label) root.lookup("#watchStatus")).getText());
					verifyNoInteractions(client);
				}
				completed.complete(null);
			}
			catch (Throwable failure)
			{
				completed.completeExceptionally(failure);
			}
		});
		completed.get(90, TimeUnit.SECONDS);
	}

	@Test
	void closingDuringWatchRequestUnsubscribesWhenReplyArrives() throws Exception
	{
		var completed = new CompletableFuture<Void>();
		Platform.runLater(() -> {
			try
			{
				var client = mock(ChessClient.class);
				var response = Sinks.<ChessWatchDTO>one();
				when(client.watch(match.host(), match.gameId())).thenReturn(response.asMono());
				when(client.leaveWatch(match.host())).thenReturn(Mono.empty());
				var bundle = ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
				var controller = new ChessWatchWindowController(client, match, bundle, settings());
				load(controller, bundle);
				controller.onShown();
				controller.onHidden();
				response.tryEmitValue(state(initial, List.of(), "LIVE"));
				Platform.runLater(() -> {
					try
					{
						verify(client, times(2)).leaveWatch(match.host());
						verify(client, never()).action(anyString(), anyString());
						completed.complete(null);
					}
					catch (Throwable failure)
					{
						completed.completeExceptionally(failure);
					}
				});
			}
			catch (Throwable failure)
			{
				completed.completeExceptionally(failure);
			}
		});
		completed.get(90, TimeUnit.SECONDS);
	}

	private Parent load(ChessWatchWindowController controller, ResourceBundle bundle) throws Exception
	{
		var loader = new FXMLLoader(getClass().getResource("/view/chess/chess_watch_window.fxml"), bundle);
		// Same loading path as WindowManager's programmatically constructed chess windows.
		loader.setController(controller);
		return loader.load();
	}

	@Test
	void opensContactsNextMatchInExistingWindow() throws Exception
	{
		var completed = new CompletableFuture<Void>();
		Platform.runLater(() -> {
			try
			{
				var client = mock(ChessClient.class);
				var next = new ChessActiveGameDTO(match.host(), "next-match", "Alice", match.opponent(), "Bob", false);
				var nextState = new ChessWatchDTO(match.host(), "next-match", "Alice", "Bob", initial, true, 0, List.of(), -1, -1, "LIVE", "");
				when(client.watch(next.host(), next.gameId())).thenReturn(Mono.just(nextState));
				when(client.leaveWatch(next.host())).thenReturn(Mono.empty());
				var bundle = ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH);
				var controller = new ChessWatchWindowController(client, match, bundle, settings());
				var root = load(controller, bundle);
				controller.update(state(initial, List.of("e4"), "ENDED"));
				controller.showMatch(next);
				Platform.runLater(() -> {
					try
					{
						assertEquals(bundle.getString("chess.watch.white-turn"), ((Label) root.lookup("#watchStatus")).getText());
						assertTrue(((ListView<?>) root.lookup("#watchMoves")).getItems().isEmpty());
						verify(client).watch(next.host(), next.gameId());
						controller.onHidden();
						completed.complete(null);
					}
					catch (Throwable failure)
					{
						completed.completeExceptionally(failure);
					}
				});
			}
			catch (Throwable failure)
			{
				completed.completeExceptionally(failure);
			}
		});
		completed.get(90, TimeUnit.SECONDS);
	}

	private ChessWatchDTO state(String squares, List<String> moves, String status)
	{
		return new ChessWatchDTO(match.host(), match.gameId(), "Alice", "Bob", squares, moves.isEmpty(), moves.size(), moves,
				moves.isEmpty() ? -1 : 52, moves.isEmpty() ? -1 : 36, status, "");
	}

	private ChessSettings settings()
	{
		var settings = mock(ChessSettings.class);
		when(settings.getTheme()).thenReturn(ChessBoardTheme.BROWN);
		when(settings.themeProperty()).thenReturn(new SimpleObjectProperty<>(ChessBoardTheme.BROWN));
		return settings;
	}
}
