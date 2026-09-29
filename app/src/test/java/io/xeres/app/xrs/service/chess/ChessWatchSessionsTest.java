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

import io.xeres.app.database.model.location.Location;
import io.xeres.app.xrs.service.gxstunnel.GxsTunnelRsService;
import io.xeres.app.xrs.service.gxstunnel.GxsTunnelStatus;
import io.xeres.common.dto.chess.ChessActiveGameDTO;
import io.xeres.common.dto.chess.ChessGameDTO;
import io.xeres.common.id.GxsId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChessWatchSessionsTest
{
	private final GxsId own = GxsId.fromString("11".repeat(16));
	private final GxsId host = GxsId.fromString("22".repeat(16));
	private final GxsId opponent = GxsId.fromString("33".repeat(16));
	private final Location tunnel = mock(Location.class);
	private final GxsTunnelRsService tunnels = mock(GxsTunnelRsService.class);
	private final JsonMapper mapper = JsonMapper.builder().build();
	private final ChessWatchSessions watches = new ChessWatchSessions(mapper, GxsId::asString);
	private final ChessActiveGameDTO match = new ChessActiveGameDTO(host.asString(), "game-1", "Host", opponent.asString(), "Other player", false);

	@BeforeEach
	void setup()
	{
		watches.initialize(tunnels);
		when(tunnels.requestSecuredTunnel(own, host, ChessRsService.TUNNEL_SERVICE_ID)).thenReturn(tunnel);
		when(tunnels.sendData(eq(tunnel), eq(ChessRsService.TUNNEL_SERVICE_ID), any())).thenReturn(true);
	}

	@Test
	void joinsMidGameAndAcceptsRetroChessEndpointAliasesAndDuplicateMoves()
	{
		assertEquals("WAITING", watches.watch(own, match).status());
		var position = new ChessPosition().move("e2e4");
		state(match.gameId(), position, 1, List.of("e4"));
		assertEquals(position.squares(), watches.get(host).squares());
		assertEquals("LIVE", watches.get(host).status());
		var next = position.move("e7e5");
		var action = Map.of("type", "chess_watch_action", "version", 1, "game_id", opponent.asString(),
				"action", "move:2:12:28:-:" + next.hash());
		receive(action);
		receive(action);
		assertEquals(2, watches.get(host).sequence());
		assertEquals(next.squares(), watches.get(host).squares());
		state(opponent.asString(), next, 2, List.of("e4", "e5"));
		assertEquals(List.of("e4", "e5"), watches.get(host).moves());
		state(opponent.asString(), position, 1, List.of("e4"));
		assertEquals(2, watches.get(host).sequence());
		watches.leave(host);
		verify(tunnels).sendData(eq(tunnel), eq(ChessRsService.TUNNEL_SERVICE_ID), argThat(data ->
				new String(data, StandardCharsets.UTF_8).contains("chess_watch_leave") &&
						new String(data, StandardCharsets.UTF_8).contains(opponent.asString())));
		verify(tunnels, never()).releaseTunnelService(any(), anyInt());
	}

	@Test
	void ignoresUnsolicitedWrongHostWrongMatchAndWrongParticipants()
	{
		state(match.gameId(), new ChessPosition(), 0, List.of());
		assertThrows(IllegalArgumentException.class, () -> watches.get(host));
		watches.watch(own, match);
		state("other-game", new ChessPosition(), 0, List.of());
		var packet = mapper.valueToTree(statePacket(match.gameId(), new ChessPosition(), 0, List.of()));
		watches.handle(opponent, tunnel, packet, List.of());
		watches.handle(host, mock(Location.class), packet, List.of());
		var wrongPlayers = new java.util.HashMap<>(statePacket(match.gameId(), new ChessPosition(), 0, List.of()));
		wrongPlayers.put("black_id", own.asString());
		receive(wrongPlayers);
		assertEquals("WAITING", watches.get(host).status());
		assertEquals("", watches.get(host).squares());
	}

	@Test
	void rejectsInvalidFenAndRecoversFromInvalidMoveWithoutChangingBoard()
	{
		watches.watch(own, match);
		state(match.gameId(), new ChessPosition(), 0, List.of());
		var before = watches.get(host);
		receive(Map.of("type", "chess_watch_action", "version", 1, "game_id", match.gameId(),
				"action", "move:1:52:36:-:bad-hash"));
		assertEquals(before.squares(), watches.get(host).squares());
		assertEquals(0, watches.get(host).sequence());
		var invalid = new java.util.HashMap<>(statePacket(match.gameId(), new ChessPosition(), 0, List.of()));
		invalid.put("fen", "8/8/8/8/8/8/8/8 w - - 0 1");
		assertThrows(IllegalArgumentException.class, () -> receive(invalid));
		assertEquals(before.squares(), watches.get(host).squares());
	}

	@Test
	void endsAndDisconnectsOnlyTheSelectedWatch()
	{
		watches.watch(own, match);
		state(match.gameId(), new ChessPosition(), 0, List.of());
		receive(Map.of("type", "chess_watch_end", "version", 1, "game_id", opponent.asString(), "reason", "Game ended"));
		assertEquals("ENDED", watches.get(host).status());
		state(match.gameId(), new ChessPosition(), 0, List.of());
		assertEquals("ENDED", watches.get(host).status());
		watches.watch(own, match);
		watches.connectionChanged(host, tunnel, GxsTunnelStatus.REMOTELY_CLOSED);
		assertEquals("UNAVAILABLE", watches.get(host).status());
	}

	@Test
	void hostsRetroChessCanonicalRequestRelaysBothSidesAndUnregisters()
	{
		var initial = hosted(new ChessPosition(), List.of(), "ACTIVE");
		var pair = "contact_game:" + own.asString() + "_" + opponent.asString();
		watches.handle(host, tunnel, mapper.valueToTree(Map.of("type", "chess_watch_req", "version", 1, "game_id", pair)), List.of(initial));
		verify(tunnels).sendData(eq(tunnel), anyInt(), argThat(data -> new String(data, StandardCharsets.UTF_8).contains("chess_watch_state")));
		clearInvocations(tunnels);
		var moved = hosted(new ChessPosition().move("e2e4").move("e7e5"), List.of("e2e4", "e7e5"), "ACTIVE");
		watches.publish(List.of(moved));
		verify(tunnels).sendData(eq(tunnel), anyInt(), argThat(data -> new String(data, StandardCharsets.UTF_8).contains("\"sequence\":2")));
		watches.handle(host, tunnel, mapper.valueToTree(Map.of("type", "chess_watch_leave", "version", 1, "game_id", pair)), List.of(moved));
		clearInvocations(tunnels);
		watches.publish(List.of(moved));
		verifyNoInteractions(tunnels);
	}

	@Test
	void sendsFinalBoardAndEndWithoutTransferringSpectatorsToRematch()
	{
		var initial = hosted(new ChessPosition(), List.of(), "ACTIVE");
		watches.handle(host, tunnel, mapper.valueToTree(Map.of("type", "chess_watch_req", "version", 1, "game_id", "local-game")), List.of(initial));
		clearInvocations(tunnels);
		watches.publish(List.of(hosted(new ChessPosition(), List.of(), "RESIGNED")));
		verify(tunnels).sendData(eq(tunnel), anyInt(), argThat(data -> new String(data, StandardCharsets.UTF_8).contains("chess_watch_end")));
		clearInvocations(tunnels);
		watches.publish(List.of(initial));
		verifyNoInteractions(tunnels);
	}

	@Test
	void refusesUnknownHostedMatch()
	{
		watches.handle(host, tunnel, mapper.valueToTree(Map.of("type", "chess_watch_req", "version", 1, "game_id", "missing")), List.of());
		verify(tunnels).sendData(eq(tunnel), anyInt(), argThat(data -> new String(data, StandardCharsets.UTF_8).contains("Game not active")));
	}

	@Test
	void unansweredRequestsTimeOutAndAbandonedWatchesAreRemoved()
	{
		watches.watch(own, match);
		var sessions = (Map<?, ?>) org.springframework.test.util.ReflectionTestUtils.getField(watches, "watches");
		var session = sessions.get(host);
		org.springframework.test.util.ReflectionTestUtils.setField(session, "requested", java.time.Instant.now().minusSeconds(46));
		watches.maintain();
		assertEquals("UNAVAILABLE", watches.get(host).status());
		org.springframework.test.util.ReflectionTestUtils.setField(session, "accessed", java.time.Instant.now().minusSeconds(61));
		watches.maintain();
		assertThrows(IllegalArgumentException.class, () -> watches.get(host));
		verify(tunnels, never()).releaseTunnelService(any(), anyInt());
	}

	private ChessWatchSessions.HostedGame hosted(ChessPosition position, List<String> moves, String status)
	{
		return new ChessWatchSessions.HostedGame("local-game", new ChessGameDTO(opponent.asString(), "Other", own.asString(), status, true,
				position.isWhiteToMove(), position.squares(), position.fen(), position.hash(), moves, List.of(), false, false, "", List.of(), false));
	}

	private Map<String, Object> statePacket(String id, ChessPosition position, int sequence, List<String> moves)
	{
		return Map.of("type", "chess_watch_state", "version", 1, "game_id", id,
				"white_id", host.asString(), "black_id", opponent.asString(), "white_name", "Host", "black_name", "Other",
				"fen", position.fen(), "sequence", sequence, "moves", moves);
	}

	private void state(String id, ChessPosition position, int sequence, List<String> moves)
	{
		receive(statePacket(id, position, sequence, moves));
	}

	private void receive(Map<String, ?> packet)
	{
		watches.handle(host, tunnel, mapper.valueToTree(packet), List.of());
	}
}
