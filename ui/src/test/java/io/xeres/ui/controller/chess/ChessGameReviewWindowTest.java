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

import io.xeres.common.dto.chess.*;
import io.xeres.ui.FXTest;
import io.xeres.ui.support.chess.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChessGameReviewWindowTest extends FXTest
{
	@Test
	void reviewsPositionsWithoutLiveGameControls() throws Exception
	{
		var checked = new CompletableFuture<Void>();
		Platform.runLater(() -> {
			try
			{
				var initial = "rnbqkbnrpppppppp" + ".".repeat(32) + "PPPPPPPPRNBQKBNR";
				var after = initial.toCharArray();
				after[52] = '.';
				after[36] = 'P';
				var game = new ChessGameDTO("22".repeat(16), "Black player", "11".repeat(16), "DRAW", true,
						false, new String(after), "fen", "hash", List.of("e2e4"), List.of(), false, false, "", List.of(), false,
						false, false, List.of(new ChessBoardDTO(initial, true, false), new ChessBoardDTO(new String(after), false, false)));
				var summary = new ChessHistorySummaryDTO("id", "date", "White player", "Black player", "DRAW", 1);
				var settings = mock(ChessSettings.class);
				when(settings.getTheme()).thenReturn(ChessBoardTheme.BROWN);
				var view = new ChessGameReviewWindow(game, summary, ResourceBundle.getBundle("i18n.messages", Locale.ENGLISH), settings);
				new Scene(view, 850, 590);
				view.applyCss();
				view.layout();
				var board = (GridPane) view.lookup("#reviewBoard");
				assertEquals(64, board.getChildren().size());
				assertTrue(board.getChildren().stream().allMatch(Label.class::isInstance));
				assertEquals(board.getWidth(), board.getHeight(), 1);
				assertNull(view.lookup("#abort"));
				assertNull(view.lookup("#draw"));
				assertNull(view.lookup("#resign"));
				assertTrue(((Label) view.lookup("#reviewPlayers")).getText().contains("1/2-1/2"));
				assertTrue(((Button) view.lookup("#reviewFirst")).isDisabled());
				((Button) view.lookup("#reviewNext")).fire();
				assertEquals("e4 P", board.getChildren().get(36).getAccessibleText());
				assertTrue(((Button) view.lookup("#reviewLast")).isDisabled());
				((Button) view.lookup("#reviewFirst")).fire();
				assertEquals("e4 .", board.getChildren().get(36).getAccessibleText());
				view.showPly(999);
				assertEquals("e4 P", board.getChildren().get(36).getAccessibleText());
				view.resize(1000, 700);
				view.layout();
				assertEquals(board.getWidth(), board.getHeight(), 1);
				checked.complete(null);
			}
			catch (Throwable failure)
			{
				checked.completeExceptionally(failure);
			}
		});
		checked.get(90, TimeUnit.SECONDS);
	}
}
