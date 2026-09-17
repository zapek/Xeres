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

package io.xeres.app.xrs.service.chess;

import io.xeres.app.service.IdentityService;
import io.xeres.common.dto.chess.ChessBoardDTO;
import io.xeres.common.dto.chess.ChessGameDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ChessRatingServiceTest
{
	@TempDir
	Path directory;

	private ChessHistoryStore historyStore;
	private IdentityService identityService;
	private ChessRatingService ratingService;

	private static final String PLAYER1_ID = "11".repeat(16);
	private static final String PLAYER2_ID = "22".repeat(16);

	@BeforeEach
	void setUp()
	{
		historyStore = new ChessHistoryStore(directory);
		identityService = mock(IdentityService.class);
		ratingService = new ChessRatingService(historyStore, identityService);
	}

	private ChessGameDTO createFinishedGame(String status, int moves)
	{
		var board = new ChessPosition().squares();
		return new ChessGameDTO(
				PLAYER2_ID,
				"Bob",
				PLAYER1_ID,
				status,
				true,
				true,
				board,
				"fen",
				"hash",
				List.of("e2e4"),
				List.of(),
				false,
				false,
				"",
				List.of(),
				false,
				false,
				false,
				List.of(new ChessBoardDTO(board, true, false), new ChessBoardDTO(board, false, false))
		);
	}

	@Test
	void initialRatingForUnknownPeer()
	{
		var rating = ratingService.getRating(PLAYER1_ID);
		assertNotNull(rating);
		assertEquals(1500, rating.rating());
		assertEquals(350, rating.rd());
		assertEquals(0, rating.games());
		assertEquals(0, rating.wins());
		assertEquals(0, rating.draws());
		assertEquals(0, rating.losses());
		assertEquals("Provisional", rating.status());
	}

	@Test
	void ratingUpdatesAfterWinAndLoss() throws Exception
	{
		var id = UUID.randomUUID().toString();
		// Odd number of moves (1 move) with CHECKMATE means White won.
		var game = createFinishedGame("CHECKMATE", 1);
		historyStore.save(id, "2026-09-17T18:00:00Z", "Alice", game);

		var leaderboard = ratingService.getLeaderboard();
		assertEquals(2, leaderboard.size());

		var winner = leaderboard.stream().filter(e -> e.peer().equals(PLAYER1_ID)).findFirst().orElseThrow();
		var loser = leaderboard.stream().filter(e -> e.peer().equals(PLAYER2_ID)).findFirst().orElseThrow();

		assertEquals(1, winner.rank());
		assertEquals(2, loser.rank());
		assertTrue(winner.rating() > 1500, "Winner rating should increase");
		assertTrue(loser.rating() < 1500, "Loser rating should decrease");
		assertTrue(winner.rd() < 350, "Winner RD should decrease");
		assertTrue(loser.rd() < 350, "Loser RD should decrease");

		assertEquals(1, winner.games());
		assertEquals(1, winner.wins());
		assertEquals(0, winner.losses());

		assertEquals(1, loser.games());
		assertEquals(0, loser.wins());
		assertEquals(1, loser.losses());

		assertEquals("Provisional", winner.status());
		assertEquals("Provisional", loser.status());
	}

	@Test
	void drawUpdatesBothPlayers() throws Exception
	{
		var id = UUID.randomUUID().toString();
		var game = createFinishedGame("DRAW", 20);
		historyStore.save(id, "2026-09-17T18:00:00Z", "Alice", game);

		var leaderboard = ratingService.getLeaderboard();
		assertEquals(2, leaderboard.size());

		for (var entry : leaderboard)
		{
			assertEquals(1, entry.games());
			assertEquals(0, entry.wins());
			assertEquals(1, entry.draws());
			assertEquals(0, entry.losses());
			assertEquals(1500, entry.rating());
			assertTrue(entry.rd() < 350);
		}
	}
}
