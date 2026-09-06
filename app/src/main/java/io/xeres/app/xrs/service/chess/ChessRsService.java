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
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.service.IdentityService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.gxstunnel.GxsTunnelRsClient;
import io.xeres.app.xrs.service.gxstunnel.GxsTunnelRsService;
import io.xeres.app.xrs.service.gxstunnel.GxsTunnelStatus;
import io.xeres.common.dto.chess.ChessGameDTO;
import io.xeres.common.id.GxsId;
import io.xeres.common.protocol.xrs.RsServiceType;
import io.xeres.common.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

/// Built-in identity chess, speaking the RetroChess GXS protocol.
@Service
public class ChessRsService extends RsService implements GxsTunnelRsClient
{
	private static final Logger log = LoggerFactory.getLogger(ChessRsService.class);
	public static final int TUNNEL_SERVICE_ID = 0xC4E5;
	private final IdentityService identities;
	private final ObjectMapper mapper;
	private final Map<GxsId, Game> games = new LinkedHashMap<>();
	private GxsTunnelRsService tunnels;
	private ScheduledExecutorService maintenance;

	@Override
	public void initialize()
	{
		maintenance = ExecutorUtils.createFixedRateExecutor(this::maintainSessions, 2);
	}

	@Override
	public void cleanup()
	{
		ExecutorUtils.cleanupExecutor(maintenance);
	}

	private synchronized void maintainSessions()
	{
		for (var game : games.values())
		{
			if ((game.status.equals("OUTGOING") || game.status.equals("INCOMING")) && Duration.between(game.created, Instant.now()).toMinutes() >= 10)
			{
				tunnels.cancelPendingData(game.tunnel, TUNNEL_SERVICE_ID);
				game.status = "EXPIRED";
			}
			if (finished(game) && game.finishedAt == null)
			{
				game.finishedAt = Instant.now();
			}
			if (game.finishedAt != null && Duration.between(game.finishedAt, Instant.now()).toSeconds() >= 10 && !game.released)
			{
				// Allow the final action to be acknowledged before detaching only chess.
				tunnels.releaseTunnelService(game.tunnel, TUNNEL_SERVICE_ID);
				game.released = true;
			}
		}
	}

	public ChessRsService(RsServiceRegistry registry, IdentityService identities, ObjectMapper mapper)
	{
		super(registry);
		this.identities = identities;
		this.mapper = mapper;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return RsServiceType.RETRO_CHESS;
	}

	@Override
	public RsServiceType getMasterServiceType()
	{
		return RsServiceType.GXS_TUNNELS;
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		// Identity games are carried by authenticated GXS tunnels.
	}

	@Override
	public synchronized int onGxsTunnelInitialization(GxsTunnelRsService service)
	{
		tunnels = service;
		return TUNNEL_SERVICE_ID;
	}

	public synchronized ChessGameDTO invite(GxsId peer)
	{
		if (tunnels == null)
		{
			throw new IllegalStateException("Chess is waiting for the network");
		}
		var own = identities.getOwnIdentity().getGxsId();
		if (peer.equals(own))
		{
			throw new IllegalArgumentException("Cannot invite yourself");
		}
		var existing = games.get(peer);
		if (existing != null && !finished(existing))
		{
			return snapshot(existing);
		}
		makeRoom();
		var game = new Game(peer, own, name(peer), true, "OUTGOING");
		games.put(peer, game);
		try
		{
			game.tunnel = existing != null && !existing.released ? existing.tunnel : tunnels.requestSecuredTunnel(own, peer, TUNNEL_SERVICE_ID);
			if (game.tunnel == null)
			{
				throw new IllegalStateException("Chess tunnel already in use");
			}
			send(game, "chess_invite", "");
			return snapshot(game);
		}
		catch (RuntimeException e)
		{
			games.remove(peer);
			throw e;
		}
	}

	public synchronized List<ChessGameDTO> list()
	{
		maintainSessions();
		return games.values().stream().map(this::snapshot).toList();
	}

	public synchronized ChessGameDTO action(GxsId peer, String action)
	{
		var game = games.get(peer);
		if (game == null)
		{
			throw new IllegalArgumentException("No chess game with this identity");
		}
		switch (action)
		{
			case "accept" ->
			{
				require(game.status.equals("INCOMING"), "No invitation to accept");
				send(game, "chess_accept", "");
				game.status = "ACTIVE";
			}
			case "leave", "decline" ->
			{
				tunnels.cancelPendingData(game.tunnel, TUNNEL_SERVICE_ID);
				send(game, "player_leave", "");
				game.status = "CLOSED";
			}
			case "abort", "resign", "draw_offer", "draw_accept", "draw_decline", "draw_repetition", "draw_fifty_move" ->
			{
				require(game.status.equals("ACTIVE"), "Game is not active");
				validateAction(game, action, false);
				send(game, "game_action", action);
				applyAction(game, action, false);
			}
			default ->
			{
				require(game.status.equals("ACTIVE") && game.white == game.position.isWhiteToMove(), "It is not your turn");
				var next = game.position.move(action);
				var promotion = action.length() == 5 ? Character.toUpperCase(action.charAt(4)) : '-';
				if (promotion == 'N')
				{
					promotion = 'H';
				}
				var packet = "move:" + (game.moves.size() + 1) + ":" + ChessPosition.index(action.substring(0, 2)) + ":" +
						ChessPosition.index(action.substring(2, 4)) + ":" + promotion + ":" + next.hash();
				send(game, "game_action", packet);
				commitMove(game, next, action);
			}
		}
		return snapshot(game);
	}

	@Override
	public synchronized boolean onGxsTunnelDataAuthorization(GxsId sender, Location tunnel, boolean clientSide)
	{
		return sender != null && !sender.equals(identities.getOwnIdentity().getGxsId());
	}

	@Override
	public synchronized void onGxsTunnelDataReceived(Location tunnel, byte[] data)
	{
		if (data.length > 2048)
		{
			return;
		}
		var peer = tunnels.getGxsFromTunnel(tunnel);
		if (peer == null)
		{
			return;
		}
		var game = games.get(peer);
		try
		{
			var packet = mapper.readTree(data);
			var type = packet.path("type").asString();
			if (type.equals("chess_invite"))
			{
				if (game != null && !finished(game))
				{
					// Simultaneous invitations: the lower identity remains the inviter.
					if (!game.status.equals("OUTGOING") || game.ownGxsId.compareTo(peer) < 0)
					{
						return;
					}
				}
				makeRoom();
				game = new Game(peer, identities.getOwnIdentity().getGxsId(), name(peer), false, "INCOMING");
				game.tunnel = tunnel;
				games.put(peer, game);
				return;
			}
			if (game == null || !tunnel.equals(game.tunnel))
			{
				return;
			}
			switch (type)
			{
				case "chess_accept" ->
				{
					if (game.status.equals("OUTGOING"))
					{
						game.status = "ACTIVE";
					}
				}
				case "player_leave" ->
				{
					if (!finished(game))
					{
						game.status = "CLOSED";
					}
				}
				case "game_action" ->
				{
					if (game.status.equals("ACTIVE"))
					{
						var action = packet.path("action").asString();
						if (action.startsWith("move:"))
						{
							receiveMove(game, action);
						}
						else
						{
							validateAction(game, action, true);
							applyAction(game, action, true);
						}
					}
				}
				case "rematch" -> send(game, "game_action", "rematch_decline");
				default -> log.debug("Ignoring unknown chess packet type {}", type);
			}
		}
		catch (RuntimeException e)
		{
			log.debug("Rejected chess packet: {}", e.getMessage());
			if (game != null && game.status.equals("ACTIVE"))
			{
				game.status = "DESYNCHRONIZED";
				game.detail = e.getMessage();
			}
		}
	}

	private void receiveMove(Game game, String action)
	{
		require(game.white != game.position.isWhiteToMove(), "Move received out of turn");
		var parts = action.split(":", -1);
		require(parts.length == 6 || parts.length == 4, "Malformed move");
		var verified = parts.length == 6;
		var offset = verified ? 1 : 0;
		if (verified)
		{
			require(Integer.parseInt(parts[1]) == game.moves.size() + 1, "Move sequence mismatch");
		}
		var promotion = parts[3 + offset];
		require(promotion.matches("[-QRBH]"), "Invalid promotion");
		var uci = ChessPosition.square(Integer.parseInt(parts[1 + offset])) + ChessPosition.square(Integer.parseInt(parts[2 + offset])) +
				(promotion.equals("-") ? "" : promotion.equals("H") ? "n" : promotion.toLowerCase(java.util.Locale.ROOT));
		var next = game.position.move(uci);
		if (verified)
		{
			require(next.hash().equals(parts[5]), "Board hash mismatch; game paused");
		}
		commitMove(game, next, uci);
	}

	private void commitMove(Game game, ChessPosition next, String uci)
	{
		game.position = next;
		game.moves.add(uci);
		game.incomingDraw = false;
		game.outgoingDraw = false;
		var count = game.repetitions.merge(next.repetitionKey(), 1, Integer::sum);
		if (next.legalMoves().isEmpty())
		{
			game.status = next.inCheck(next.isWhiteToMove()) ? "CHECKMATE" : "DRAW";
		}
		else if (next.insufficientMaterial() || next.halfmoveClock() >= 150 || count >= 5)
		{
			game.status = "DRAW";
		}
	}

	private void validateAction(Game game, String action, boolean remote)
	{
		if (action.equals("draw_accept") || action.equals("draw_decline"))
		{
			require(remote ? game.outgoingDraw : game.incomingDraw, "No draw offer to answer");
		}
		if (action.equals("draw_repetition"))
		{
			require(game.repetitions.getOrDefault(game.position.repetitionKey(), 0) >= 3, "Position has not repeated three times");
		}
		if (action.equals("draw_fifty_move"))
		{
			require(game.position.halfmoveClock() >= 100, "Fifty-move rule does not apply");
		}
	}

	private void applyAction(Game game, String action, boolean remote)
	{
		switch (action)
		{
			case "resign" -> game.status = remote ? "OPPONENT_RESIGNED" : "RESIGNED";
			case "abort" -> game.status = "CLOSED";
			case "draw_offer" ->
			{
				game.drawNotice = remote ? "DRAW_OFFER_RECEIVED" : "DRAW_OFFER_SENT";
				if (remote)
				{
					game.incomingDraw = true;
				}
				else
				{
					game.outgoingDraw = true;
				}
			}
			case "draw_decline" ->
			{
				game.drawNotice = remote ? "DRAW_DECLINED_BY_OPPONENT" : "DRAW_DECLINED_BY_YOU";
				game.incomingDraw = false;
				game.outgoingDraw = false;
			}
			case "draw_accept" ->
			{
				game.drawNotice = remote ? "DRAW_ACCEPTED_BY_OPPONENT" : "DRAW_ACCEPTED_BY_YOU";
				game.incomingDraw = false;
				game.outgoingDraw = false;
				game.status = "DRAW";
			}
			case "draw_repetition", "draw_fifty_move" -> game.status = "DRAW";
			default -> log.debug("Ignoring unsupported chess action {}", action);
		}
	}

	@Override
	public synchronized void onGxsTunnelStatusChanged(Location tunnel, GxsId destination, GxsTunnelStatus status)
	{
		var game = games.get(destination);
		if (game != null && tunnel.equals(game.tunnel) && !finished(game))
		{
			game.detail = status == GxsTunnelStatus.CAN_TALK ? "" : "CONNECTION_INTERRUPTED";
		}
	}

	private void send(Game game, String type, String action)
	{
		var packet = action.isEmpty() ? Map.of("type", type) : Map.of("type", type, "action", action);
		require(tunnels.sendData(game.tunnel, TUNNEL_SERVICE_ID, mapper.writeValueAsBytes(packet)), "Chess tunnel unavailable");
	}

	private String name(GxsId peer)
	{
		return identities.findByGxsId(peer).map(identity -> identity.getName()).orElse(peer.asString());
	}

	private ChessGameDTO snapshot(Game game)
	{
		return new ChessGameDTO(game.peerGxsId.asString(), game.name, game.ownGxsId.asString(), game.status, game.white,
				game.position.isWhiteToMove(), game.position.squares(), game.position.fen(), game.position.hash(), List.copyOf(game.moves),
				game.status.equals("ACTIVE") && game.white == game.position.isWhiteToMove() ? game.position.legalMoves() : List.of(),
				game.incomingDraw, game.outgoingDraw, game.detail.isEmpty() ? game.drawNotice : game.detail);
	}

	private boolean finished(Game game)
	{
		return !List.of("INCOMING", "OUTGOING", "ACTIVE").contains(game.status);
	}

	private void makeRoom()
	{
		if (games.size() >= 64)
		{
			games.values().removeIf(game -> finished(game) && game.released);
		}
		require(games.size() < 64, "Too many chess sessions");
	}

	private static void require(boolean condition, String message)
	{
		if (!condition)
		{
			throw new IllegalArgumentException(message);
		}
	}

	private static final class Game
	{
		private final GxsId peerGxsId;
		private final GxsId ownGxsId;
		private final String name;
		private final boolean white;
		private final Instant created = Instant.now();
		private Instant finishedAt;
		private boolean released;
		private final List<String> moves = new ArrayList<>();
		private final Map<String, Integer> repetitions = new HashMap<>();
		private ChessPosition position = new ChessPosition();
		private Location tunnel;
		private String status;
		private String detail = "";
		private String drawNotice = "";
		private boolean incomingDraw;
		private boolean outgoingDraw;

		private Game(GxsId peer, GxsId own, String name, boolean white, String status)
		{
			this.peerGxsId = peer;
			this.ownGxsId = own;
			this.name = name;
			this.white = white;
			this.status = status;
			repetitions.put(position.repetitionKey(), 1);
		}
	}
}
