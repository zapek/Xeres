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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Same rules as RetroChess' tests/leaderboard-sync.cpp.
class ChessLeaderboardTest
{
	private static final String WHITE = "aa".repeat(16);
	private static final String BLACK = "bb".repeat(16);
	private static final String RELAY = "cc".repeat(16);

	@TempDir
	Path directory;

	private final JsonMapper mapper = JsonMapper.builder().build();
	private final List<String> sent = new ArrayList<>();
	private final long[] clockMs = {0};
	private ChessLeaderboard leaderboard;

	@BeforeEach
	void setUp()
	{
		leaderboard = new ChessLeaderboard(directory.resolve("chess-leaderboard.json"), null, () -> clockMs[0], () -> 1_790_000_000L);
		leaderboard.setTransport(new ChessLeaderboard.Transport()
		{
			@Override
			public boolean send(String peer, byte[] data)
			{
				sent.add(peer + " " + new String(data, StandardCharsets.UTF_8));
				return true;
			}

			@Override
			public List<String> activePeers()
			{
				return List.of(BLACK, RELAY);
			}

			@Override
			public boolean isOnline(String peer)
			{
				return true;
			}
		});
	}

	private void receive(String from, String json)
	{
		leaderboard.handleTunnelData(from, mapper.readTree(json));
	}

	private static String receipt(String gameId, String result, String signer, long finishedAt)
	{
		return "{\"type\":\"leaderboard_receipt\",\"version\":1,\"game_id\":\"" + gameId + "\",\"white\":\"" + WHITE + "\",\"black\":\"" + BLACK +
				"\",\"result\":\"" + result + "\",\"signer\":\"" + signer + "\",\"finished_at\":" + finishedAt + "}";
	}

	@Test
	void gameCountsOnlyWhenBothPlayersReportedTheSameResult()
	{
		leaderboard.submitResult("g1", WHITE, BLACK, "1-0", WHITE);
		assertEquals(2, sent.size());
		assertTrue(sent.getFirst().contains("\"type\":\"leaderboard_receipt\""));
		assertTrue(leaderboard.players().isEmpty());

		receive(BLACK, receipt("g1", "1-0", BLACK, 1_790_000_005L));
		var players = leaderboard.players();
		assertEquals(2, players.size());
		assertEquals(WHITE, players.getFirst().id());
		assertEquals(1662.3109, players.getFirst().rating(), 0.0001);
		assertEquals(290.3190, players.getFirst().rd(), 0.0001);
	}

	@Test
	void relayedReceiptNeedsTwoWitnesses()
	{
		receive(RELAY, receipt("g1", "1-0", BLACK, 5));
		assertEquals(1, leaderboard.pendingCount());
		assertEquals(0, leaderboard.receiptCount());
		receive(RELAY, receipt("g1", "1-0", BLACK, 5));
		assertEquals(0, leaderboard.receiptCount());
		receive(WHITE, receipt("g1", "1-0", BLACK, 5));
		assertEquals(1, leaderboard.receiptCount());
		assertEquals(0, leaderboard.pendingCount());
	}

	@Test
	void conflictingClaimsAreExcluded()
	{
		receive(BLACK, receipt("g1", "0-1", BLACK, 5));
		leaderboard.submitResult("g1", WHITE, BLACK, "1-0", WHITE);
		assertTrue(leaderboard.players().isEmpty());
	}

	@Test
	void invalidReceiptsAreRejected()
	{
		assertFalse(leaderboard.consumeReceipt(new ChessLeaderboard.Receipt("g1", WHITE, "zz", "1-0", WHITE, 5), WHITE));
		assertFalse(leaderboard.consumeReceipt(new ChessLeaderboard.Receipt("g1", WHITE, BLACK, "2-0", WHITE, 5), WHITE));
		assertFalse(leaderboard.consumeReceipt(new ChessLeaderboard.Receipt("g1", WHITE, BLACK, "1-0", RELAY, 5), RELAY));
		assertFalse(leaderboard.consumeReceipt(new ChessLeaderboard.Receipt("", WHITE, BLACK, "1-0", WHITE, 5), WHITE));
		assertFalse(leaderboard.consumeReceipt(new ChessLeaderboard.Receipt("g1", WHITE, BLACK, "1-0", WHITE, 0), WHITE));
	}

	@Test
	void fullSyncIsBatchedThenIncremental()
	{
		for (var i = 0; i < 25; i++)
		{
			leaderboard.submitResult("g" + i, WHITE, BLACK, "1/2-1/2", WHITE);
		}
		sent.clear();
		receive(RELAY, "{\"type\":\"leaderboard_sync_req\",\"version\":1,\"epoch\":\"\",\"since\":0}");
		assertEquals(3, sent.size());
		assertFalse(sent.getFirst().contains("\"final\""));
		var last = mapper.readTree(sent.getLast().substring(RELAY.length() + 1));
		assertTrue(last.path("final").asBoolean(false));

		// A repeated full request is rate limited.
		sent.clear();
		receive(RELAY, "{\"type\":\"leaderboard_sync_req\",\"version\":1,\"epoch\":\"\",\"since\":0}");
		assertTrue(sent.isEmpty());

		// With the cursor only the new receipt is sent.
		clockMs[0] += 61_000;
		leaderboard.submitResult("new", WHITE, BLACK, "0-1", WHITE);
		sent.clear();
		receive(RELAY, "{\"type\":\"leaderboard_sync_req\",\"version\":1,\"epoch\":\"" + last.path("epoch").asString("") + "\",\"since\":" + last.path("seq").asLong(0) + "}");
		assertEquals(1, sent.size());
		assertTrue(sent.getFirst().contains("\"new\""));
		assertFalse(sent.getFirst().contains("\"g1\""));
	}

	@Test
	void receiptsSurviveARestart()
	{
		leaderboard.submitResult("g1", WHITE, BLACK, "1-0", WHITE);
		receive(BLACK, receipt("g1", "1-0", BLACK, 5));
		leaderboard.commitChanges();

		var restarted = new ChessLeaderboard(directory.resolve("chess-leaderboard.json"), null);
		assertEquals(2, restarted.receiptCount());
		assertEquals(2, restarted.players().size());
	}
}
