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
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.gxstunnel.GxsTunnelRsService;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.common.id.GxsId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
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
	private ChessRsService chess;

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
		chess = new ChessRsService(mock(RsServiceRegistry.class), identities, JsonMapper.builder().build());
		assertEquals(0xC4E5, chess.onGxsTunnelInitialization(tunnels));
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
		chess.onGxsTunnelStatusChanged(tunnel, peer, io.xeres.app.xrs.service.gxstunnel.GxsTunnelStatus.CAN_TALK);
		assertEquals("DRAW_DECLINED_BY_OPPONENT", chess.list().getFirst().detail());
	}

	private void receive(String packet)
	{
		chess.onGxsTunnelDataReceived(tunnel, packet.getBytes(StandardCharsets.UTF_8));
	}
}
