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

import io.xeres.common.dto.chess.ChessHistorySummaryDTO;
import io.xeres.ui.FXTest;
import io.xeres.ui.client.GeneralClient;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChessHistoryIdentityCellTest extends FXTest
{
	@Test
	void mapsIdentitiesAndClearsRecycledCells() throws Exception
	{
		var checked = new CompletableFuture<Void>();
		Platform.runLater(() -> {
			try
			{
				var client = mock(GeneralClient.class);
				when(client.getImage(anyString())).thenReturn(Mono.empty());
				var white = new ChessHistoryIdentityCell(true, client, null);
				var black = new ChessHistoryIdentityCell(false, client, null);
				var game = new ChessHistorySummaryDTO("id", "date", "Same name", "Same name", "DRAW", 2, "11".repeat(16), "22".repeat(16));
				white.updateItem(game, false);
				black.updateItem(game, false);
				assertNotNull(white.getGraphic());
				assertNotNull(black.getGraphic());
				assertEquals(game.whiteIdentity(), white.getTooltip().getText());
				assertEquals(game.blackIdentity(), black.getTooltip().getText());
				white.updateItem(null, true);
				assertNull(white.getGraphic());
				assertNull(white.getText());
				assertNull(white.getTooltip());
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
