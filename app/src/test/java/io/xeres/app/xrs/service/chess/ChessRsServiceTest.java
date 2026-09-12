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
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.MessageService;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.gxstunnel.GxsTunnelRsService;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChessRsServiceTest
{
	private final GxsId own = GxsId.fromString("11".repeat(16));
	private final GxsId peer = GxsId.fromString("22".repeat(16));
	private final Location tunnel = mock(Location.class);
	private final GxsTunnelRsService tunnels = mock(GxsTunnelRsService.class);
	private final MessageService messages = mock(MessageService.class);
	private ChessRsService chess;
	private final ChessHistoryStore history = mock(ChessHistoryStore.class);

	@BeforeEach
	void setup()
	{
		var identities = mock(IdentityService.class);
		var identity = mock(IdentityGroupItem.class);
		when(identity.getGxsId()).thenReturn(own);
		when(identities.getOwnIdentity()).thenReturn(identity);
		when(identities.findByGxsId(peer)).thenReturn(Optional.empty());
		when(tunnels.requestSecuredTunnel(own, peer, 0xC4E5)).thenReturn(tunnel);
		when(tunnels.getGxsFromTunnel(tunnel)).thenReturn(peer);
		when(tunnels.sendData(eq(tunnel), eq(0xC4E5), any())).thenReturn(true);
		chess = new ChessRsService(mock(RsServiceRegistry.class), identities, JsonMapper.builder().build(), messages, history);
		assertEquals(0xC4E5, chess.onGxsTunnelInitialization(tunnels));
	}

	@Test
	void savesPlayedMovesWithoutManualAction() throws Exception
	{
		chess.invite(peer);
		receive("{\"type\":\"chess_accept\"}");
		chess.action(peer, "e2e4");
		verify(history, atLeastOnce()).save(anyString(), anyString(), anyString(), argThat(saved ->
				saved.moves().equals(List.of("e2e4")) && saved.positions().size() == 2));
	}

	@Test
	void checkmateIsExposedWithCheckState()
	{
		receive("{\"type\":\"chess_invite\"}");
		chess.action(peer, "accept");
		var position = new ChessPosition().move("f2f3");
		receive("{\"type\":\"game_action\",\"action\":\"move:1:53:45:-:" + position.hash() + "\"}");
		chess.action(peer, "e7e5");
		position = position.move("e7e5").move("g2g4");
		receive("{\"type\":\"game_action\",\"action\":\"move:3:54:38:-:" + position.hash() + "\"}");
		chess.action(peer, "d8h4");
		assertEquals("CHECKMATE", chess.list().getFirst().status());
		assertTrue(chess.list().getFirst().inCheck());
		assertTrue(chess.list().getFirst().legalMoves().isEmpty());
	}

	@Test
	void inviterIsWhiteAndCannotMoveUntilAccepted()
	{
		assertTrue(chess.invite(peer).white());
		assertThrows(IllegalArgumentException.class, () -> chess.action(peer, "e2e4"));
		receive("{\"type\":\"chess_accept\"}");
		assertEquals("ACTIVE", chess.list().getFirst().status());
		chess.action(peer, "e2e4");
		verify(tunnels).sendData(eq(tunnel), eq(0xC4E5), argThat(data ->
				new String(data, StandardCharsets.UTF_8).contains("move:1:52:36:-:952a5e992e65efab")));
	}

	@Test
	void unsolicitedAcceptanceDoesNotCreateGame()
	{
		receive("{\"type\":\"chess_accept\"}");
		assertTrue(chess.list().isEmpty());
	}

	@Test
	void accepterIsBlackAndReceivesVerifiedMove()
	{
		receive("{\"type\":\"chess_invite\"}");
		assertFalse(chess.list().getFirst().white());
		assertEquals("INCOMING", chess.list().getFirst().status());
		chess.action(peer, "accept");
		receive("{\"type\":\"game_action\",\"action\":\"move:1:52:36:-:952a5e992e65efab\"}");
		assertEquals("952a5e992e65efab", chess.list().getFirst().hash());
		assertEquals("ACTIVE", chess.list().getFirst().status());
		assertEquals(20, chess.list().getFirst().legalMoves().size());
		receive("{\"type\":\"game_action\",\"action\":\"move:1:52:36:-:952a5e992e65efab\"}");
		assertEquals("ACTIVE", chess.list().getFirst().status());
		assertEquals(1, chess.list().getFirst().moves().size());
	}

	@Test
	void hashMismatchPausesWithoutMutatingPosition()
	{
		receive("{\"type\":\"chess_invite\"}");
		chess.action(peer, "accept");
		var before = chess.list().getFirst().fen();
		receive("{\"type\":\"game_action\",\"action\":\"move:1:52:36:-:0000000000000000\"}");
		assertEquals("DESYNCHRONIZED", chess.list().getFirst().status());
		assertEquals(before, chess.list().getFirst().fen());
		var events = String.join("\n", chess.list().getFirst().debugEvents());
		assertTrue(events.contains("RX game_action move:1:"));
		assertTrue(events.contains("REJECTED Board hash mismatch"));
		assertFalse(events.contains("APPLIED"));
	}

	@Test
	void sequenceMismatchPausesGame()
	{
		receive("{\"type\":\"chess_invite\"}");
		chess.action(peer, "accept");
		receive("{\"type\":\"game_action\",\"action\":\"move:3:52:36:-:952a5e992e65efab\"}");
		assertEquals("DESYNCHRONIZED", chess.list().getFirst().status());
	}

	@Test
	void drawAcceptanceNeedsAnOfferAndSelfInviteIsRejected()
	{
		assertThrows(IllegalArgumentException.class, () -> chess.invite(own));
		chess.invite(peer);
		receive("{\"type\":\"chess_accept\"}");
		assertThrows(IllegalArgumentException.class, () -> chess.action(peer, "draw_accept"));
		chess.action(peer, "draw_offer");
		receive("{\"type\":\"game_action\",\"action\":\"draw_accept\"}");
		assertEquals("DRAW", chess.list().getFirst().status());
		assertEquals("DRAW_ACCEPTED_BY_OPPONENT", chess.list().getFirst().detail());
	}

	@Test
	void declinedDrawRemainsVisibleAcrossRefreshAndTunnelUpdates()
	{
		chess.invite(peer);
		receive("{\"type\":\"chess_accept\"}");
		chess.action(peer, "draw_offer");
		assertEquals("DRAW_OFFER_SENT", chess.list().getFirst().detail());
		receive("{\"type\":\"game_action\",\"action\":\"draw_decline\"}");
		assertEquals("ACTIVE", chess.list().getFirst().status());
		assertFalse(chess.list().getFirst().outgoingDraw());
		assertEquals("DRAW_DECLINED_BY_OPPONENT", chess.list().getFirst().detail());
	}

	@Test
	void drawActionSendsDrawOfferWhenSpecialRulesDoNotApply()
	{
		chess.invite(peer);
		receive("{\"type\":\"chess_accept\"}");
		chess.action(peer, "draw");
		assertEquals("DRAW_OFFER_SENT", chess.list().getFirst().detail());
		assertTrue(chess.list().getFirst().outgoingDraw());
		verify(tunnels).sendData(eq(tunnel), eq(0xC4E5), argThat(data ->
				new String(data, StandardCharsets.UTF_8).contains("\"action\":\"draw_offer\"")));
	}

	@Test
	void declineSendsRejectionAndAllowsAnotherInvitation()
	{
		receive("{\"type\":\"chess_invite\"}");
		assertEquals("DECLINED", chess.action(peer, "decline").status());
		verify(tunnels).sendData(eq(tunnel), eq(0xC4E5), argThat(data ->
				new String(data, StandardCharsets.UTF_8).equals("{\"type\":\"chess_reject\"}")));
		assertThrows(IllegalArgumentException.class, () -> chess.action(peer, "accept"));
		receive("{\"type\":\"chess_invite\"}");
		assertEquals("INCOMING", chess.list().getFirst().status());
	}

	@Test
	void failedRejectionKeepsInvitationForRetry()
	{
		receive("{\"type\":\"chess_invite\"}");
		when(tunnels.sendData(eq(tunnel), eq(0xC4E5), any())).thenReturn(false);
		assertThrows(IllegalArgumentException.class, () -> chess.action(peer, "decline"));
		assertEquals("INCOMING", chess.list().getFirst().status());
		when(tunnels.sendData(eq(tunnel), eq(0xC4E5), any())).thenReturn(true);
		assertEquals("DECLINED", chess.action(peer, "decline").status());
	}

	@Test
	void rejectionResolvesOutgoingInvitationAndIgnoresLateAcceptance()
	{
		chess.invite(peer);
		receive("{\"type\":\"chess_reject\"}");
		receive("{\"type\":\"chess_reject\"}");
		receive("{\"type\":\"chess_accept\"}");
		assertEquals("DECLINED", chess.list().getFirst().status());
		verify(tunnels, times(1)).cancelPendingData(tunnel, 0xC4E5);
		assertEquals("OUTGOING", chess.invite(peer).status());
	}

	@Test
	void rejectionCannotCreateOrCloseUnrelatedGame()
	{
		receive("{\"type\":\"chess_reject\"}");
		assertTrue(chess.list().isEmpty());
		receive("{\"type\":\"chess_invite\"}");
		receive("{\"type\":\"chess_reject\"}");
		assertEquals("INCOMING", chess.list().getFirst().status());
		chess.action(peer, "accept");
		receive("{\"type\":\"chess_reject\"}");
		assertEquals("ACTIVE", chess.list().getFirst().status());
		assertThrows(IllegalArgumentException.class, () -> chess.action(peer, "decline"));
	}

	@Test
	void legacyDeclineAndOutgoingCancellationRemainSupported()
	{
		chess.invite(peer);
		receive("{\"type\":\"player_leave\"}");
		assertEquals("DECLINED", chess.list().getFirst().status());
		chess.invite(peer);
		assertThrows(IllegalArgumentException.class, () -> chess.action(peer, "decline"));
		assertEquals("CLOSED", chess.action(peer, "leave").status());
		verify(tunnels).sendData(eq(tunnel), eq(0xC4E5), argThat(data ->
				new String(data, StandardCharsets.UTF_8).equals("{\"type\":\"player_leave\"}")));
	}

	@Test
	void idleSessionsDoNotPublishAndChangesArePushed()
	{
		chess.list();
		org.springframework.test.util.ReflectionTestUtils.invokeMethod(chess, "maintainSessions");
		verifyNoInteractions(messages);
		chess.invite(peer);
		verify(messages).sendToConsumers(eq("/topic/chess"),
				eq(io.xeres.common.message.MessageType.CHESS_GAMES), eq(chess.list()));
		clearInvocations(messages);
		chess.list();
		org.springframework.test.util.ReflectionTestUtils.invokeMethod(chess, "maintainSessions");
		verifyNoInteractions(messages);
		receive("{\"type\":\"chess_reject\"}");
		verify(messages).sendToConsumers(eq("/topic/chess"),
				eq(io.xeres.common.message.MessageType.CHESS_GAMES), eq(chess.list()));
	}

	@Test
	void incomingInvitationsAndMovesArePushedWithoutRestRequests()
	{
		receive("{\"type\":\"chess_invite\"}");
		verify(messages).sendToConsumers(eq("/topic/chess"),
				eq(io.xeres.common.message.MessageType.CHESS_GAMES), eq(chess.list()));
		chess.action(peer, "accept");
		clearInvocations(messages);
		receive("{\"type\":\"game_action\",\"action\":\"move:1:52:36:-:952a5e992e65efab\"}");
		verify(messages).sendToConsumers(eq("/topic/chess"),
				eq(io.xeres.common.message.MessageType.CHESS_GAMES), eq(chess.list()));
	}

	@Test
	void rematchOfferAndAcceptSwapsSidesAndResetsBoard()
	{
		chess.invite(peer);
		receive("{\"type\":\"chess_accept\"}");
		chess.action(peer, "resign");
		assertEquals("RESIGNED", chess.list().getFirst().status());

		var snapshot = chess.action(peer, "rematch");
		assertTrue(snapshot.outgoingRematch());
		assertEquals("WAITING_REMATCH", snapshot.detail());
		verify(tunnels).sendData(eq(tunnel), eq(0xC4E5), argThat(data ->
				new String(data, StandardCharsets.UTF_8).contains("\"type\":\"rematch\"") &&
				new String(data, StandardCharsets.UTF_8).contains("\"color\":0")));

		receive("{\"type\":\"rematch\",\"color\":0}");
		var restarted = chess.list().getFirst();
		assertEquals("ACTIVE", restarted.status());
		assertFalse(restarted.white());
		assertFalse(restarted.outgoingRematch());
		assertFalse(restarted.incomingRematch());
		assertTrue(restarted.moves().isEmpty());
		assertEquals("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", restarted.fen());
	}

	@Test
	void incomingRematchCanBeAcceptedAndSwapsBlackToWhite()
	{
		receive("{\"type\":\"chess_invite\"}");
		chess.action(peer, "accept");
		assertFalse(chess.list().getFirst().white());
		receive("{\"type\":\"game_action\",\"action\":\"resign\"}");
		assertEquals("OPPONENT_RESIGNED", chess.list().getFirst().status());

		receive("{\"type\":\"rematch\",\"color\":1}");
		assertTrue(chess.list().getFirst().incomingRematch());

		chess.action(peer, "rematch_accept");
		verify(tunnels).sendData(eq(tunnel), eq(0xC4E5), argThat(data ->
				new String(data, StandardCharsets.UTF_8).contains("\"type\":\"rematch\"") &&
				new String(data, StandardCharsets.UTF_8).contains("\"color\":1")));

		var restarted = chess.list().getFirst();
		assertEquals("ACTIVE", restarted.status());
		assertTrue(restarted.white());
		assertFalse(restarted.incomingRematch());
	}

	@Test
	void incomingRematchCanBeDeclined()
	{
		receive("{\"type\":\"chess_invite\"}");
		chess.action(peer, "accept");
		chess.action(peer, "resign");
		receive("{\"type\":\"rematch\",\"color\":1}");
		assertTrue(chess.list().getFirst().incomingRematch());

		chess.action(peer, "rematch_decline");
		assertFalse(chess.list().getFirst().incomingRematch());
		verify(tunnels).sendData(eq(tunnel), eq(0xC4E5), argThat(data ->
				new String(data, StandardCharsets.UTF_8).contains("\"action\":\"rematch_decline\"")));
	}

	@Test
	void remoteRematchDeclineSetsNotice()
	{
		chess.invite(peer);
		receive("{\"type\":\"chess_accept\"}");
		chess.action(peer, "resign");
		chess.action(peer, "rematch");
		assertTrue(chess.list().getFirst().outgoingRematch());

		receive("{\"type\":\"game_action\",\"action\":\"rematch_decline\"}");
		assertFalse(chess.list().getFirst().outgoingRematch());
		assertEquals("REMATCH_DECLINED", chess.list().getFirst().detail());
	}

	@Test
	void simultaneousRematchRequestsStartImmediately()
	{
		chess.invite(peer);
		receive("{\"type\":\"chess_accept\"}");
		chess.action(peer, "resign");
		chess.action(peer, "rematch");
		assertTrue(chess.list().getFirst().outgoingRematch());

		receive("{\"type\":\"rematch\",\"color\":0}");
		var restarted = chess.list().getFirst();
		assertEquals("ACTIVE", restarted.status());
		assertFalse(restarted.white());
		assertFalse(restarted.outgoingRematch());
	}

	@Test
	void outgoingInviteSupersedesPreviousIncomingInvite()
	{
		receive("{\"type\":\"chess_invite\"}");
		assertEquals("INCOMING", chess.list().getFirst().status());
		assertFalse(chess.list().getFirst().white());

		var outgoing = chess.invite(peer);
		assertEquals("OUTGOING", outgoing.status());
		assertTrue(outgoing.white());
		assertEquals("OUTGOING", chess.list().getFirst().status());
		assertTrue(chess.list().getFirst().white());
	}

	@Test
	void simultaneousInvitationsLowerIdStaysWhite()
	{
		// own < peer (11... < 22...)
		chess.invite(peer);
		assertEquals("OUTGOING", chess.list().getFirst().status());
		assertTrue(chess.list().getFirst().white());

		receive("{\"type\":\"chess_invite\"}");
		assertEquals("OUTGOING", chess.list().getFirst().status());
		assertTrue(chess.list().getFirst().white());
	}

	@Test
	void simultaneousInvitationsHigherIdBecomesBlackAndActive()
	{
		var lowerPeer = GxsId.fromString("00".repeat(16));
		when(tunnels.requestSecuredTunnel(own, lowerPeer, 0xC4E5)).thenReturn(tunnel);
		when(tunnels.getGxsFromTunnel(tunnel)).thenReturn(lowerPeer);

		// own > lowerPeer (11... > 00...)
		chess.invite(lowerPeer);
		assertEquals("OUTGOING", chess.list().getFirst().status());
		assertTrue(chess.list().getFirst().white());

		chess.onGxsTunnelDataReceived(tunnel, "{\"type\":\"chess_invite\"}".getBytes(StandardCharsets.UTF_8));
		assertEquals("ACTIVE", chess.list().getFirst().status());
		assertFalse(chess.list().getFirst().white());
		verify(tunnels).sendData(eq(tunnel), eq(0xC4E5), argThat(data ->
				new String(data, StandardCharsets.UTF_8).contains("\"type\":\"chess_accept\"")));
	}

	private void receive(String packet)
	{
		chess.onGxsTunnelDataReceived(tunnel, packet.getBytes(StandardCharsets.UTF_8));
	}
}
