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

package io.xeres.chess.app.xrs.service.chess;

import io.xeres.app.service.IdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ChessRatingServiceTest
{
	@TempDir
	Path directory;

	private ChessRatingService ratingService;

	private static final String PLAYER1_ID = "11".repeat(16);
	private static final String PLAYER2_ID = "22".repeat(16);

	@BeforeEach
	void setUp()
	{
		ratingService = new ChessRatingService(new ChessHistoryStore(directory.resolve("history")), mock(IdentityService.class), directory);
	}

	/// Both players' receipts, as they arrive over their own tunnels.
	private void playRated(String gameId, String result, long finishedAt)
	{
		var leaderboard = ratingService.leaderboard();
		leaderboard.consumeReceipt(new ChessLeaderboard.Receipt(gameId, PLAYER1_ID, PLAYER2_ID, result, PLAYER1_ID, finishedAt), PLAYER1_ID);
		leaderboard.consumeReceipt(new ChessLeaderboard.Receipt(gameId, PLAYER1_ID, PLAYER2_ID, result, PLAYER2_ID, finishedAt), PLAYER2_ID);
	}

	@Test
	void initialRatingForUnknownPeer()
	{
		var rating = ratingService.getRating(PLAYER1_ID);
		assertNotNull(rating);
		assertEquals(1500, rating.rating());
		assertEquals(350, rating.rd());
		assertEquals(0, rating.games());
		assertEquals("Provisional", rating.status());
	}

	@Test
	void ratingUpdatesAfterWinAndLoss()
	{
		playRated("game-1", "1-0", 1_790_000_000L);

		var leaderboard = ratingService.getLeaderboard();
		assertEquals(2, leaderboard.size());

		var winner = leaderboard.getFirst();
		var loser = leaderboard.getLast();
		assertEquals(PLAYER1_ID, winner.peer());
		assertEquals(1, winner.rank());
		assertEquals(2, loser.rank());
		// Same numbers as RetroChess for one decisive game between two new players.
		assertEquals(1662, winner.rating());
		assertEquals(1338, loser.rating());
		assertEquals(290, winner.rd());
		assertEquals(1, winner.wins());
		assertEquals(1, loser.losses());
		assertEquals("Provisional", winner.status());
		assertNotNull(winner.lastPlayed());
	}

	@Test
	void drawUpdatesBothPlayers()
	{
		playRated("game-1", "1/2-1/2", 1_790_000_000L);

		for (var entry : ratingService.getLeaderboard())
		{
			assertEquals(1, entry.games());
			assertEquals(1, entry.draws());
			assertEquals(1500, entry.rating());
			assertTrue(entry.rd() < 350);
		}
	}

	@Test
	void singleReceiptDoesNotCount()
	{
		ratingService.leaderboard().consumeReceipt(new ChessLeaderboard.Receipt("game-1", PLAYER1_ID, PLAYER2_ID, "1-0", PLAYER1_ID, 1_790_000_000L), PLAYER1_ID);
		assertTrue(ratingService.getLeaderboard().isEmpty());
	}
}
