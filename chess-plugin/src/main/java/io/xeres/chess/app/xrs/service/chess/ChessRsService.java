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

import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.service.IdentityService;
import io.xeres.app.service.MessageService;
import io.xeres.common.message.MessageType;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.gxstunnel.GxsTunnelRsClient;
import io.xeres.app.xrs.service.gxstunnel.GxsTunnelRsService;
import io.xeres.app.xrs.service.gxstunnel.GxsTunnelStatus;
import io.xeres.app.xrs.service.identity.item.IdentityGroupItem;
import io.xeres.chess.common.dto.chess.ChessContactDTO;
import io.xeres.chess.common.dto.chess.ChessActiveGameDTO;
import io.xeres.chess.common.dto.chess.ChessWatchDTO;
import io.xeres.chess.common.dto.chess.ChessGameDTO;
import io.xeres.chess.common.dto.chess.ChessSeekDTO;
import io.xeres.chess.common.dto.chess.ChessTimeControl;
import io.xeres.common.id.GxsId;
import io.xeres.common.protocol.xrs.RsServiceType;
import io.xeres.common.util.ExecutorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import static io.xeres.chess.ChessPaths.chessDestination;

/// Optional identity chess plugin, speaking the RetroChess GXS protocol.
@Service
public class ChessRsService extends RsService implements GxsTunnelRsClient
{
	private static final Logger log = LoggerFactory.getLogger(ChessRsService.class);
	public static final int TUNNEL_SERVICE_ID = 0xC4E5;
	private final IdentityService identities;
	private final ObjectMapper mapper;
	private final MessageService messageService;
	private final ChessHistoryStore historyStore;
	private final ChessContactsStore contactsStore;
	private final ChessWatchSessions spectators;
	// Our open game (lobby seek), advertised to confirmed peers like RetroChess' setLobbySeek().
	private boolean lobbySeekActive;
	private ChessTimeControl lobbySeek = ChessTimeControl.UNLIMITED;
	/// RetroChess accepts a reported opponent clock up to this much above the local view.
	private static final long CLOCK_LATENCY_TOLERANCE_MS = 10_000;
	private final Map<GxsId, ContactPresenceState> presenceStates = new ConcurrentHashMap<>();
	private boolean chessBusy;
	private List<ChessGameDTO> publishedGames = List.of();
	private final Map<GxsId, Game> games = new LinkedHashMap<>();
	private GxsTunnelRsService tunnels;
	private ScheduledExecutorService maintenance;
	private final ChessLeaderboard leaderboard;
	// Tunnels opened (or adopted) for presence probes, per contact.
	private final Map<GxsId, Location> presenceTunnels = new HashMap<>();
	// Tunnels known to be able to talk (CAN_TALK seen or data received).
	private final Set<Location> readyTunnels = new HashSet<>();
	private Clock clock = Clock.systemUTC();
	// Set after the first presence pass: contacts created later are new and start with a fast probe.
	private boolean presenceStarted;

	// Presence schedule, identical to RetroChess (p3RetroChess.cc) so both clients
	// produce the same tunnel traffic. After PRESENCE_MAX_FAILURES failed probes a
	// contact is dormant and only re-checked hourly; an incoming probe wakes it up.
	static final int PRESENCE_MAX_FAILURES = 7;
	private static final long PRESENCE_DORMANT_RETRY_SECONDS = 3600;
	private static final long[] PRESENCE_RETRY_DELAYS = {15, 30, 60, 120, 240, 300};
	// Tie-break: when both sides have each other as contact, only the side with the lower
	// identity dials. The other one waits this long for the incoming tunnel before dialing.
	private static final long PRESENCE_YIELD_SECONDS = 75;
	// Tunnel discovery and its key exchange get their own budget; the reply timeout
	// only starts once the probe is actually sent over a working tunnel.
	private static final long PRESENCE_DISCOVERY_SECONDS = 120;
	private static final long PRESENCE_REPLY_SECONDS = 45;
	private static final long PRESENCE_REFRESH_SECONDS = 60;
	private static final long PRESENCE_STALE_SECONDS = 120;
	// Bound expensive tunnel discovery, not heartbeats on working tunnels.
	private static final int PRESENCE_MAX_IN_FLIGHT = 4;
	private static final List<String> ONLINE_STATUSES = List.of("available", "busy", "playing");

	static long presenceRetryDelay(int failures)
	{
		if (failures <= 0) return PRESENCE_RETRY_DELAYS[0];
		if (failures > PRESENCE_RETRY_DELAYS.length) return PRESENCE_DORMANT_RETRY_SECONDS;
		return PRESENCE_RETRY_DELAYS[failures - 1];
	}

	private static final class ContactPresenceState
	{
		private String status = "unknown";
		private Instant lastSeen;
		private Instant nextProbe;
		private Instant deadline;
		private Instant yieldUntil;
		private String nonce;
		private Location probeTunnel;
		private int failures;
		private String opponentId = "";
		private String opponentName = "";
		private String gameId = "";
		/// Open game advertised by this contact (chess_seek or presence reply).
		private boolean seeking;
		private ChessTimeControl seekTimeControl = ChessTimeControl.UNLIMITED;
	}

	@Override
	public void initialize()
	{
		maintenance = ExecutorUtils.createFixedRateExecutor(this::maintain, 2);
	}

	@Override
	public void cleanup()
	{
		ExecutorUtils.cleanupExecutor(maintenance);
	}

	private synchronized void maintain()
	{
		maintainSessions();
		tickChessPresence();
		spectators.maintain();
		if (leaderboard != null)
		{
			leaderboard.tick();
		}
	}

	private synchronized void maintainSessions()
	{
		var flagged = false;
		for (var game : games.values())
		{
			flagged |= flagIfExpired(game);
		}
		if (flagged)
		{
			publishGames();
		}
		for (var game : games.values())
		{
			saveHistory(game);
			if ((game.status.equals("OUTGOING") || game.status.equals("INCOMING")) && Duration.between(game.created, Instant.now()).toMinutes() >= 10)
			{
				tunnels.cancelPendingData(game.tunnel, TUNNEL_SERVICE_ID);
				game.status = "EXPIRED";
				publishGames();
			}
			if (finished(game) && game.finishedAt == null)
			{
				game.finishedAt = clock.instant();
			}
			var timeout = game.status.equals("CLOSED") ? Duration.ofSeconds(10) : Duration.ofMinutes(10);
			if (game.finishedAt != null && Duration.between(game.finishedAt, clock.instant()).compareTo(timeout) >= 0 && !game.released)
			{
				// Allow the final action to be acknowledged before detaching only chess.
				// A contact's tunnel stays open for presence probes, like in RetroChess.
				if (!game.tunnel.equals(presenceTunnels.get(game.peerGxsId)))
				{
					tunnels.releaseTunnelService(game.tunnel, TUNNEL_SERVICE_ID);
					readyTunnels.remove(game.tunnel);
				}
				game.released = true;
			}
		}
	}

	public ChessRsService(RsServiceRegistry registry, IdentityService identities, ObjectMapper mapper, MessageService messageService, ChessHistoryStore historyStore, ChessContactsStore contactsStore)
	{
		this(registry, identities, mapper, messageService, historyStore, contactsStore, null);
	}

	@Autowired
	public ChessRsService(RsServiceRegistry registry, IdentityService identities, ObjectMapper mapper, MessageService messageService, ChessHistoryStore historyStore, ChessContactsStore contactsStore, ChessRatingService ratingService)
	{
		super(registry);
		this.identities = identities;
		this.mapper = mapper;
		this.messageService = messageService;
		this.historyStore = historyStore;
		this.contactsStore = contactsStore;
		spectators = new ChessWatchSessions(mapper, this::name);
		leaderboard = ratingService != null ? ratingService.leaderboard() : null;
		if (leaderboard != null)
		{
			leaderboard.setTransport(new LeaderboardTransport());
		}
	}

	/// For tests: presence timing uses this clock.
	synchronized void setClock(Clock clock)
	{
		this.clock = clock;
	}

	private final class LeaderboardTransport implements ChessLeaderboard.Transport
	{
		@Override
		public boolean send(String peer, byte[] data)
		{
			synchronized (ChessRsService.this)
			{
				var tunnel = readyTunnel(GxsId.fromString(peer));
				return tunnel != null && tunnels.sendData(tunnel, TUNNEL_SERVICE_ID, data);
			}
		}

		@Override
		public List<String> activePeers()
		{
			synchronized (ChessRsService.this)
			{
				return confirmedPeers().stream().map(GxsId::asString).toList();
			}
		}

		@Override
		public boolean isOnline(String peer)
		{
			synchronized (ChessRsService.this)
			{
				var state = presenceStates.get(GxsId.fromString(peer));
				return state != null && ONLINE_STATUSES.contains(state.status);
			}
		}
	}

	@Override
	public io.xeres.common.protocol.xrs.RsServiceDescriptor getServiceType()
	{
		return io.xeres.chess.ChessServiceType.CHESS;
	}

	@Override
	public boolean isEnabledByDefault()
	{
		return true;
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
		spectators.initialize(service);
		return TUNNEL_SERVICE_ID;
	}

	public synchronized ChessGameDTO invite(GxsId peer)
	{
		return invite(peer, ChessTimeControl.UNLIMITED, false);
	}

	/// Invites a player. `joinOpenGame` answers the player's open game with its time control.
	public synchronized ChessGameDTO invite(GxsId peer, ChessTimeControl timeControl, boolean joinOpenGame)
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
		if (existing != null && (existing.status.equals("ACTIVE") || existing.status.equals("OUTGOING")))
		{
			return snapshot(existing);
		}
		if (existing != null)
		{
			existing.released = true;
			existing.status = "CLOSED";
		}
		makeRoom();
		var game = new Game(peer, own, name(peer), true, "OUTGOING");
		game.gameId = UUID.randomUUID().toString();
		// Normal invitations are always unlimited, like RetroChess.
		game.timeControl = joinOpenGame && timeControl != null ? timeControl : ChessTimeControl.UNLIMITED;
		game.joinRequest = joinOpenGame;
		games.put(peer, game);
		if (contactsStore != null)
		{
			contactsStore.add(peer.asString());
		}
		try
		{
			var tunnel = tunnels.getTunnel(own, peer);
			if (tunnel == null)
			{
				tunnel = tunnels.requestSecuredTunnel(own, peer, TUNNEL_SERVICE_ID);
			}
			if (tunnel == null)
			{
				throw new IllegalStateException("Chess tunnel unavailable");
			}
			game.tunnel = tunnel;
			try
			{
				send(game, inviteMessage(game));
			}
			catch (RuntimeException e)
			{
				tunnels.releaseTunnelService(tunnel, TUNNEL_SERVICE_ID);
				tunnel = tunnels.requestSecuredTunnel(own, peer, TUNNEL_SERVICE_ID);
				if (tunnel == null)
				{
					throw e;
				}
				game.tunnel = tunnel;
				send(game, inviteMessage(game));
			}
			publishGames();
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

	public synchronized List<ChessActiveGameDTO> activeGames()
	{
		var result = new ArrayList<ChessActiveGameDTO>();
		var own = identities.getOwnIdentity().getGxsId().asString();
		for (var game : games.values())
		{
			if (game.status.equals("ACTIVE"))
			{
				result.add(new ChessActiveGameDTO(game.peerGxsId.asString(), game.historyId, name(game.ownGxsId),
						game.peerGxsId.asString(), game.name, true));
			}
		}
		var pairs = new HashSet<String>();
		for (var entry : presenceStates.entrySet())
		{
			var state = entry.getValue();
			var host = entry.getKey().asString();
			if (!List.of("playing", "checking").contains(state.status) || state.lastSeen == null || state.lastSeen.plusSeconds(PRESENCE_STALE_SECONDS).isBefore(clock.instant()) ||
					state.opponentId.isEmpty() || state.opponentId.equals(own) ||
					contactsStore == null || !contactsStore.contains(host)) continue;
			var pair = host.compareTo(state.opponentId) < 0 ? host + ":" + state.opponentId : state.opponentId + ":" + host;
			if (!pairs.add(pair)) continue;
			result.add(new ChessActiveGameDTO(host, state.gameId.isEmpty() ? "contact:" + pair : state.gameId,
					name(entry.getKey()), state.opponentId, state.opponentName.isBlank() ? state.opponentId : state.opponentName, false));
		}
		return result;
	}

	public synchronized ChessWatchDTO watch(GxsId host, String gameId)
	{
		var match = activeGames().stream().filter(g -> !g.local() && g.host().equals(host.asString()) && g.gameId().equals(gameId))
				.findFirst().orElseThrow(() -> new IllegalArgumentException("Contact game is no longer active"));
		return spectators.watch(identities.getOwnIdentity().getGxsId(), match);
	}

	public synchronized ChessWatchDTO watchedGame(GxsId host)
	{
		return spectators.get(host);
	}

	public synchronized void leaveWatch(GxsId host)
	{
		spectators.leave(host);
	}

	private List<ChessWatchSessions.HostedGame> hostedGames()
	{
		return games.values().stream().map(g -> new ChessWatchSessions.HostedGame(g.historyId, snapshot(g))).toList();
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
				send(game, acceptMessage(game));
				activate(game);
				if (contactsStore != null)
				{
					contactsStore.add(game.peerGxsId.asString());
				}
			}
			case "decline" ->
			{
				require(game.status.equals("INCOMING"), "No invitation to decline");
				send(game, "chess_reject", "");
				game.status = "DECLINED";
				game.detail = "";
			}
			case "leave" ->
			{
				tunnels.cancelPendingData(game.tunnel, TUNNEL_SERVICE_ID);
				send(game, "player_leave", "");
				game.incomingRematch = false;
				game.outgoingRematch = false;
				game.status = "CLOSED";
				game.detail = "";
			}
			case "rematch", "rematch_accept" ->
			{
				require(finished(game), "Game is still active");
				require(!game.status.equals("CLOSED"), "Game is closed");
				if (game.incomingRematch)
				{
					// Accepting echoes the game id proposed by the opponent.
					send(game, Map.of("type", "rematch", "color", !game.white ? 1 : 0, "game_id", game.gameId));
					resetGameForRematch(game);
				}
				else if (!game.outgoingRematch)
				{
					game.outgoingRematch = true;
					game.detail = "WAITING_REMATCH";
					game.pendingRematchId = UUID.randomUUID().toString();
					game.gameId = game.pendingRematchId;
					send(game, Map.of("type", "rematch", "color", !game.white ? 1 : 0, "game_id", game.gameId));
				}
			}
			case "rematch_decline" ->
			{
				require(game.incomingRematch, "No rematch offer to decline");
				game.incomingRematch = false;
				send(game, "game_action", "rematch_decline");
			}
			case "draw" ->
			{
				require(game.status.equals("ACTIVE"), "Game is not active");
				var resolved = "draw_offer";
				if (game.white == game.position.isWhiteToMove())
				{
					if (game.repetitions.getOrDefault(game.position.repetitionKey(), 0) >= 3)
					{
						resolved = "draw_repetition";
					}
					else if (game.position.halfmoveClock() >= 100)
					{
						resolved = "draw_fifty_move";
					}
				}
				validateAction(game, resolved, false);
				send(game, "game_action", resolved);
				applyAction(game, resolved, false);
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
				if (flagIfExpired(game))
				{
					// Our time ran out before this move.
					break;
				}
				var next = game.position.move(action);
				var promotion = action.length() == 5 ? Character.toUpperCase(action.charAt(4)) : '-';
				if (promotion == 'N')
				{
					promotion = 'H';
				}
				var packet = "move:" + (game.moves.size() + 1) + ":" + ChessPosition.index(action.substring(0, 2)) + ":" +
						ChessPosition.index(action.substring(2, 4)) + ":" + promotion + ":" + next.hash();
				var clocks = clocksAfterMove(game);
				if (game.clockStartedAt != null)
				{
					// Timed RetroChess move: both clocks travel with the move as a display hint.
					packet += ":" + clocks[0] + ":" + clocks[1];
				}
				send(game, "game_action", packet);
				switchClocks(game, clocks);
				commitMove(game, next, action);
			}
		}
		publishGames();
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
		if (data.length > 256 * 1024)
		{
			return;
		}
		var peer = tunnels.getGxsFromTunnel(tunnel);
		if (peer == null)
		{
			return;
		}
		// Receiving data proves the tunnel can talk.
		readyTunnels.add(tunnel);
		var game = games.get(peer);
		try
		{
			var packet = mapper.readTree(data);
			var type = packet.path("type").asString();
			if (type.startsWith("chess_watch_"))
			{
				// Spectator packets must never desynchronize a playable game with this peer.
				try
				{
					spectators.handle(peer, tunnel, packet, hostedGames());
				}
				catch (RuntimeException e)
				{
					log.debug("Rejected chess spectator packet: {}", e.getMessage());
				}
				return;
			}
			if (type.startsWith("leaderboard_"))
			{
				// Sync batches carry up to 10 receipts and may exceed the small packet limit below.
				if (leaderboard != null && data.length <= 64 * 1024)
				{
					leaderboard.handleTunnelData(peer.asString(), packet);
				}
				return;
			}
			if (data.length > 2048) return;
			if (type.equals("chess_presence_request"))
			{
				handlePresenceRequest(peer, tunnel, packet);
				return;
			}
			if (type.equals("chess_seek"))
			{
				handleSeek(peer, packet);
				return;
			}
			if (type.equals("chess_presence_reply"))
			{
				handlePresenceReply(peer, tunnel, packet);
				return;
			}
			if (type.equals("chess_invite"))
			{
				if (chessBusy && (game == null || !game.status.equals("OUTGOING")))
				{
					// Like RetroChess: a busy player answers chess_busy and shows no invitation.
					tunnels.sendData(tunnel, TUNNEL_SERVICE_ID, mapper.writeValueAsBytes(Map.of("type", "chess_busy")));
					return;
				}
				if (game != null && !finished(game))
				{
					if (game.status.equals("OUTGOING"))
					{
						// Simultaneous invitations: the lower identity remains the inviter (white).
						// The higher identity becomes black and automatically accepts.
						if (game.ownGxsId.compareTo(peer) < 0)
						{
							recordEvent(game, "RX chess_invite (simultaneous invite; remaining white)");
							return;
						}
						else
						{
							recordEvent(game, "RX chess_invite (simultaneous invite; becoming black & active)");
							game.white = false;
							game.tunnel = tunnel;
							game.gameId = packetGameId(packet);
							game.timeControl = ChessTimeControl.fromNetString(packet.path("tc").asString(""));
							send(game, acceptMessage(game));
							activate(game);
							return;
						}
					}
					else if (game.status.equals("INCOMING"))
					{
						// Refreshed/duplicate invitation: update tunnel and notify
						game.tunnel = tunnel;
						game.gameId = packetGameId(packet);
						game.timeControl = ChessTimeControl.fromNetString(packet.path("tc").asString(""));
						game.joinRequest = packet.path("join_open_game").asBoolean(false);
						recordEvent(game, "RX chess_invite (refreshed)");
						return;
					}
					else if (game.status.equals("ACTIVE"))
					{
						saveHistory(game);
						game.status = "CLOSED";
						recordEvent(game, "RX chess_invite (closed previous active game)");
					}
				}
				makeRoom();
				game = new Game(peer, identities.getOwnIdentity().getGxsId(), name(peer), false, "INCOMING");
				game.tunnel = tunnel;
				game.gameId = packetGameId(packet);
				game.timeControl = ChessTimeControl.fromNetString(packet.path("tc").asString(""));
				game.joinRequest = packet.path("join_open_game").asBoolean(false);
				games.put(peer, game);
				if (contactsStore != null)
				{
					contactsStore.add(peer.asString());
				}
				recordEvent(game, "RX chess_invite");
				return;
			}
			if (game == null || !tunnel.equals(game.tunnel))
			{
				return;
			}
			var actionStr = packet.has("action") ? " " + packet.path("action").asString() : "";
			var colorStr = packet.has("color") ? " color=" + packet.get("color") : "";
			recordEvent(game, "RX " + type + colorStr + actionStr);
			switch (type)
			{
				case "chess_accept" ->
				{
					if (game.status.equals("OUTGOING"))
					{
						// Like RetroChess, the accepting side's game id and time control are authoritative.
						game.gameId = packetGameId(packet);
						if (packet.has("tc"))
						{
							game.timeControl = ChessTimeControl.fromNetString(packet.path("tc").asString(""));
						}
						activate(game);
					}
				}
				case "chess_cancel" ->
				{
					if (game.status.equals("INCOMING"))
					{
						game.status = "DECLINED";
						game.detail = "";
						recordEvent(game, "RX chess_cancel");
					}
				}
				case "chess_busy" ->
				{
					// RetroChess answers an invitation with chess_busy when its player is busy.
					if (game.status.equals("OUTGOING"))
					{
						tunnels.cancelPendingData(game.tunnel, TUNNEL_SERVICE_ID);
						game.status = "DECLINED";
						game.detail = "";
					}
				}
				case "chess_reject" ->
				{
					if (game.status.equals("OUTGOING"))
					{
						tunnels.cancelPendingData(game.tunnel, TUNNEL_SERVICE_ID);
						game.status = "DECLINED";
						game.detail = "";
					}
				}
				case "player_leave" ->
				{
					if (!finished(game))
					{
						// Older clients signal an invitation decline with player_leave.
						var outgoingInvitation = game.status.equals("OUTGOING");
						if (outgoingInvitation)
						{
							tunnels.cancelPendingData(game.tunnel, TUNNEL_SERVICE_ID);
						}
						// RetroChess rates a game left mid-play as a win for the remaining player.
						game.opponentLeft = game.status.equals("ACTIVE");
						game.status = outgoingInvitation ? "DECLINED" : "CLOSED";
						game.detail = "";
					}
					else
					{
						game.incomingRematch = false;
						game.outgoingRematch = false;
						game.status = "CLOSED";
						game.detail = "";
					}
				}
				case "game_action" ->
				{
					var action = packet.path("action").asString();
					if (action.equals("rematch_decline"))
					{
						game.outgoingRematch = false;
						game.pendingRematchId = null;
						game.detail = "REMATCH_DECLINED";
					}
					else if (game.status.equals("ACTIVE"))
					{
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
				case "rematch" ->
				{
					if (finished(game))
					{
						// Crossed rematch requests: both sides keep the smaller game id.
						var incoming = packetGameId(packet);
						if (game.pendingRematchId != null && !incoming.isEmpty() && game.pendingRematchId.compareTo(incoming) < 0)
						{
							incoming = game.pendingRematchId;
						}
						game.gameId = incoming;
						game.pendingRematchId = null;
						if (game.outgoingRematch)
						{
							resetGameForRematch(game);
						}
						else
						{
							game.incomingRematch = true;
						}
					}
				}
				default -> log.debug("Ignoring unknown chess packet type {}", type);
			}
		}
		catch (RuntimeException e)
		{
			log.debug("Rejected chess packet: {}", e.getMessage());
			if (game != null)
			{
				recordEvent(game, "REJECTED " + e.getMessage());
			}
			if (game != null && game.status.equals("ACTIVE"))
			{
				game.status = "DESYNCHRONIZED";
				game.detail = e.getMessage();
			}
		}
		finally
		{
			publishGames();
		}
	}

	private void receiveMove(Game game, String action)
	{
		var parts = action.split(":", -1);
		// 8 parts: timed RetroChess move carrying the white and black clocks.
		require(parts.length == 8 || parts.length == 6 || parts.length == 4, "Malformed move");
		var verified = parts.length >= 6;
		var offset = verified ? 1 : 0;
		if (verified)
		{
			var seq = Integer.parseInt(parts[1]);
			if (seq == game.moves.size() && parts[5].equals(game.position.hash()))
			{
				log.debug("Ignoring duplicate chess move packet: sequence={}", seq);
				return;
			}
			require(seq == game.moves.size() + 1, "Move sequence mismatch (expected " + (game.moves.size() + 1) + ", got " + seq + ")");
		}
		require(game.white != game.position.isWhiteToMove(), "Move received out of turn");
		var promotion = parts[3 + offset];
		require(promotion.matches("[-QRBH]"), "Invalid promotion");
		var uci = ChessPosition.square(Integer.parseInt(parts[1 + offset])) + ChessPosition.square(Integer.parseInt(parts[2 + offset])) +
				(promotion.equals("-") ? "" : promotion.equals("H") ? "n" : promotion.toLowerCase(java.util.Locale.ROOT));
		var next = game.position.move(uci);
		if (verified)
		{
			require(next.hash().equals(parts[5]), "Board hash mismatch; game paused");
		}
		if (game.clockStartedAt != null)
		{
			var clocks = clocksAfterMove(game);
			if (parts.length == 8)
			{
				// The reported clocks are only a hint. Our own clock is never overwritten; the
				// opponent's is accepted but clamped so it can neither go below zero nor gain more
				// than one increment plus latency tolerance over our own measurement (RetroChess rules).
				var moverWhite = game.position.isWhiteToMove();
				var local = clocks[moverWhite ? 0 : 1];
				var reported = parseClock(parts[moverWhite ? 6 : 7], local);
				var upper = local + game.timeControl.incrementMs() + CLOCK_LATENCY_TOLERANCE_MS;
				clocks[moverWhite ? 0 : 1] = Math.clamp(reported, 0L, upper);
			}
			switchClocks(game, clocks);
		}
		commitMove(game, next, uci);
	}

	private static long parseClock(String value, long fallback)
	{
		try
		{
			return Long.parseLong(value);
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}

	private void commitMove(Game game, ChessPosition next, String uci)
	{
		game.position = next;
		game.moves.add(uci);
		game.positions.add(new io.xeres.chess.common.dto.chess.ChessBoardDTO(next.squares(), next.isWhiteToMove(), next.inCheck(next.isWhiteToMove())));
		recordEvent(game, "APPLIED " + uci + " sequence=" + game.moves.size() + " hash=" + next.hash() + " FEN=" + next.fen());
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
			// Only the player whose clock ran out declares it (RetroChess onClockExpired()).
			case "timeout" -> game.status = remote ? "OPPONENT_TIMEOUT" : "TIMEOUT";
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
		spectators.connectionChanged(destination, tunnel, status);
		presenceTunnelStatusChanged(tunnel, destination, status);
		var game = games.get(destination);
		if (game != null && tunnel.equals(game.tunnel) && !finished(game))
		{
			recordEvent(game, "CONNECTION " + status);
			game.detail = status == GxsTunnelStatus.CAN_TALK ? "" : "CONNECTION_INTERRUPTED";
			publishGames();
		}
	}

	private void send(Game game, Map<String, Object> packet)
	{
		var queued = tunnels.sendData(game.tunnel, TUNNEL_SERVICE_ID, mapper.writeValueAsBytes(packet));
		var action = packet.get("action");
		recordEvent(game, (queued ? "TX QUEUED " : "TX FAILED ") + packet.get("type") + (action != null ? " " + action : ""));
		require(queued, "Chess tunnel unavailable");
	}

	private void send(Game game, String type, String action)
	{
		var packet = action.isEmpty() ? Map.<String, Object>of("type", type) : Map.<String, Object>of("type", type, "action", action);
		send(game, packet);
	}

	private void resetGameForRematch(Game game)
	{
		saveHistory(game);
		game.historyId = java.util.UUID.randomUUID().toString();
		game.historyStarted = Instant.now().toString();
		game.savedHistory = null;
		game.white = !game.white;
		game.position = new ChessPosition();
		game.moves.clear();
		game.positions.clear();
		game.positions.add(new io.xeres.chess.common.dto.chess.ChessBoardDTO(new ChessPosition().squares(), true, false));
		game.repetitions.clear();
		game.repetitions.put(game.position.repetitionKey(), 1);
		activate(game);
		game.detail = "";
		game.drawNotice = "";
		game.incomingDraw = false;
		game.outgoingDraw = false;
		game.incomingRematch = false;
		game.outgoingRematch = false;
		game.finishedAt = null;
		game.released = false;
		game.resultSubmitted = false;
		game.opponentLeft = false;
		game.pendingRematchId = null;
		recordEvent(game, "REMATCH STARTED as " + (game.white ? "WHITE" : "BLACK") + " game=" + game.gameId);
	}

	private String name(GxsId peer)
	{
		return identities.findByGxsId(peer).map(identity -> identity.getName()).orElse(peer.asString());
	}

	private Map<String, Object> inviteMessage(Game game)
	{
		var message = new LinkedHashMap<String, Object>();
		message.put("type", "chess_invite");
		message.put("game_id", game.gameId);
		message.put("join_open_game", game.joinRequest);
		if (!game.timeControl.unlimited())
		{
			message.put("tc", game.timeControl.toNetString());
		}
		return message;
	}

	private Map<String, Object> acceptMessage(Game game)
	{
		var message = new LinkedHashMap<String, Object>();
		message.put("type", "chess_accept");
		message.put("game_id", game.gameId);
		if (!game.timeControl.unlimited())
		{
			message.put("tc", game.timeControl.toNetString());
		}
		return message;
	}

	/// The game starts: White's clock runs from now, and our open game is withdrawn.
	private void activate(Game game)
	{
		game.status = "ACTIVE";
		if (game.timeControl.unlimited())
		{
			game.clockStartedAt = null;
		}
		else
		{
			game.whiteMs = game.timeControl.initialMs();
			game.blackMs = game.timeControl.initialMs();
			game.clockStartedAt = clock.instant();
		}
		if (lobbySeekActive)
		{
			lobbySeekActive = false;
			lobbySeek = ChessTimeControl.UNLIMITED;
			broadcastSeek();
		}
	}

	private long clockElapsedMs(Game game)
	{
		return game.clockStartedAt == null ? 0 : Math.max(0, Duration.between(game.clockStartedAt, clock.instant()).toMillis());
	}

	/// Time left for a side now, counting down the running clock.
	private long remainingMs(Game game, boolean white)
	{
		var stored = white ? game.whiteMs : game.blackMs;
		if (game.clockStartedAt != null && white == game.position.isWhiteToMove())
		{
			stored -= clockElapsedMs(game);
		}
		return stored;
	}

	/// Clock values after the side to move made its move: its time is charged and it gets the increment.
	private long[] clocksAfterMove(Game game)
	{
		var moverWhite = game.position.isWhiteToMove();
		var mover = remainingMs(game, moverWhite) + game.timeControl.incrementMs();
		return moverWhite ? new long[]{mover, game.blackMs} : new long[]{game.whiteMs, mover};
	}

	private void switchClocks(Game game, long[] clocks)
	{
		if (game.clockStartedAt == null)
		{
			return;
		}
		game.whiteMs = clocks[0];
		game.blackMs = clocks[1];
		game.clockStartedAt = clock.instant();
	}

	/// Stops the clocks of a finished game, keeping the time left.
	private void freezeClocks(Game game)
	{
		if (game.clockStartedAt == null || game.status.equals("ACTIVE"))
		{
			return;
		}
		var white = game.position.isWhiteToMove();
		var left = Math.max(0, remainingMs(game, white));
		if (white) game.whiteMs = left;
		else game.blackMs = left;
		game.clockStartedAt = null;
	}

	/// Our clock ran out: like RetroChess the player whose time is over declares "timeout" and loses.
	private boolean flagIfExpired(Game game)
	{
		if (!game.status.equals("ACTIVE") || game.clockStartedAt == null || game.white != game.position.isWhiteToMove()
				|| remainingMs(game, game.white) > 0)
		{
			return false;
		}
		recordEvent(game, "CLOCK expired");
		try
		{
			send(game, "game_action", "timeout");
		}
		catch (RuntimeException e)
		{
			log.debug("Unable to send chess timeout to {}", game.peerGxsId, e);
		}
		applyAction(game, "timeout", false);
		freezeClocks(game);
		return true;
	}

	// ---------------------------------------------------------------------------------------
	// Open games (lobby seeks), RetroChess "chess_seek"
	// ---------------------------------------------------------------------------------------

	public synchronized ChessSeekDTO seek()
	{
		return new ChessSeekDTO(lobbySeekActive, lobbySeek.toNetString());
	}

	/// Creates our open game with this time control and advertises it to the confirmed peers.
	public synchronized ChessSeekDTO createSeek(ChessTimeControl timeControl)
	{
		require(!hasActiveGame(), "Finish the current game first");
		lobbySeekActive = true;
		lobbySeek = timeControl != null ? timeControl : ChessTimeControl.UNLIMITED;
		broadcastSeek();
		return seek();
	}

	public synchronized ChessSeekDTO cancelSeek()
	{
		if (lobbySeekActive)
		{
			lobbySeekActive = false;
			lobbySeek = ChessTimeControl.UNLIMITED;
			broadcastSeek();
		}
		return seek();
	}

	private void broadcastSeek()
	{
		if (tunnels == null)
		{
			return;
		}
		var message = new LinkedHashMap<String, Object>();
		message.put("type", "chess_seek");
		message.put("seeking", lobbySeekActive);
		if (lobbySeekActive)
		{
			message.put("tc", lobbySeek.toNetString());
		}
		var data = mapper.writeValueAsBytes(message);
		for (var peer : confirmedPeers())
		{
			var tunnel = readyTunnel(peer);
			if (tunnel != null)
			{
				tunnels.sendData(tunnel, TUNNEL_SERVICE_ID, data);
			}
		}
	}

	private void handleSeek(GxsId peer, tools.jackson.databind.JsonNode packet)
	{
		var state = presenceStates.get(peer);
		if (state == null)
		{
			return;
		}
		var value = packet.path("tc").asString("");
		var timeControl = ChessTimeControl.fromNetString(value.isEmpty() ? "unlimited" : value);
		state.seeking = packet.path("seeking").asBoolean(!timeControl.unlimited());
		state.seekTimeControl = state.seeking ? timeControl : ChessTimeControl.UNLIMITED;
	}

	/// Peers with a usable tunnel that may receive lobby and leaderboard data.
	private List<GxsId> confirmedPeers()
	{
		var peers = new LinkedHashSet<GxsId>(presenceTunnels.keySet());
		games.forEach((peer, game) -> {
			if (!game.released) peers.add(peer);
		});
		return peers.stream().filter(peer -> readyTunnel(peer) != null && chessPeerConfirmed(peer)).toList();
	}

	private static String packetGameId(tools.jackson.databind.JsonNode packet)
	{
		var id = packet.path("game_id").asString("");
		return id.length() <= 128 ? id : "";
	}

	/// Result of a finished rated game from White's point of view, or null when it is not rated.
	private static String ratedResult(Game game)
	{
		return switch (game.status)
		{
			// The side to move is checkmated.
			case "CHECKMATE" -> game.position.isWhiteToMove() ? "0-1" : "1-0";
			case "DRAW" -> "1/2-1/2";
			case "RESIGNED" -> game.white ? "0-1" : "1-0";
			case "OPPONENT_RESIGNED", "OPPONENT_TIMEOUT" -> game.white ? "1-0" : "0-1";
			case "TIMEOUT" -> game.white ? "0-1" : "1-0";
			case "CLOSED" -> game.opponentLeft ? (game.white ? "1-0" : "0-1") : null;
			default -> null;
		};
	}

	/// Publishes our leaderboard receipt once per finished game (RetroChess `submitRatedResult()`).
	private void submitRatedResults()
	{
		if (leaderboard == null)
		{
			return;
		}
		for (var game : games.values())
		{
			if (game.resultSubmitted || game.gameId.isEmpty())
			{
				continue;
			}
			var result = ratedResult(game);
			if (result == null)
			{
				continue;
			}
			game.resultSubmitted = true;
			var own = game.ownGxsId.asString();
			var opponent = game.peerGxsId.asString();
			recordEvent(game, "RATED RESULT " + result + " game=" + game.gameId);
			leaderboard.submitResult(game.gameId, game.white ? own : opponent, game.white ? opponent : own, result, own);
		}
	}

	private void publishGames()
	{
		games.values().forEach(this::freezeClocks);
		submitRatedResults();
		for (var game : games.values()) saveHistory(game);
		var snapshots = games.values().stream().map(this::snapshot).toList();
		if (!snapshots.equals(publishedGames))
		{
			spectators.publish(hostedGames());
			messageService.sendToConsumers(chessDestination(), "CHESS_GAMES", snapshots);
			publishedGames = snapshots;
		}
	}

	private void saveHistory(Game game)
	{
		if (game.savedHistory == null && !game.status.equals("ACTIVE") && game.moves.isEmpty()) return;
		if (game.status.equals("CLOSED") && game.savedHistory != null &&
				List.of("CHECKMATE", "DRAW", "RESIGNED", "OPPONENT_RESIGNED", "TIMEOUT", "OPPONENT_TIMEOUT").contains(game.savedHistory.status())) return;
		var value = snapshot(game);
		if (value.equals(game.savedHistory)) return;
		try
		{
			historyStore.save(game.historyId, game.historyStarted, name(game.ownGxsId), value);
			game.savedHistory = value;
		}
		catch (java.io.IOException | RuntimeException failure)
		{
			log.error("Unable to save chess history for {}", game.peerGxsId, failure);
		}
	}

	private ChessGameDTO snapshot(Game game)
	{
		return new ChessGameDTO(game.peerGxsId.asString(), game.name, game.ownGxsId.asString(), game.status, game.white,
				game.position.isWhiteToMove(), game.position.squares(), game.position.fen(), game.position.hash(), List.copyOf(game.moves),
				game.status.equals("ACTIVE") && game.white == game.position.isWhiteToMove() ? game.position.legalMoves() : List.of(),
				game.incomingDraw, game.outgoingDraw, game.detail.isEmpty() ? game.drawNotice : game.detail, List.copyOf(game.debugEvents),
				game.position.inCheck(game.position.isWhiteToMove()), game.incomingRematch, game.outgoingRematch, List.copyOf(game.positions),
				game.timeControl.toNetString(), game.whiteMs, game.blackMs, game.clockStartedAt != null ? game.clockStartedAt.toEpochMilli() : 0L);
	}

	private static void recordEvent(Game game, String event)
	{
		if (game.debugEvents.size() >= 1000)
		{
			game.debugEvents.removeFirst();
		}
		game.debugEvents.add(Instant.now() + " " + event);
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

	private void handlePresenceRequest(GxsId peer, Location tunnel, tools.jackson.databind.JsonNode packet)
	{
		var nonce = packet.path("nonce").asString("");
		var version = packet.path("version").asInt(0);
		if (version != 1 || nonce.isBlank() || nonce.length() > 64)
		{
			return;
		}
		String state = chessBusy ? "busy" : (hasActiveGame() ? "playing" : "available");
		var reply = new HashMap<String, Object>();
		reply.put("type", "chess_presence_reply");
		reply.put("version", 1);
		reply.put("nonce", nonce);
		reply.put("status", state);
		reply.put("seeking", lobbySeekActive && state.equals("available"));
		if (lobbySeekActive && state.equals("available"))
		{
			reply.put("tc", lobbySeek.toNetString());
		}
		games.values().stream().filter(g -> g.status.equals("ACTIVE")).findFirst().ifPresent(game -> {
			// Presence discovery also works when the other player is not the spectator's contact.
			reply.put("status", "playing");
			reply.put("opponent_id", game.peerGxsId.asString());
			reply.put("opponent_name", game.name);
			reply.put("game_id", game.historyId);
		});
		try
		{
			tunnels.sendData(tunnel, TUNNEL_SERVICE_ID, mapper.writeValueAsBytes(reply));
		}
		catch (RuntimeException e)
		{
			log.debug("Failed to send chess_presence_reply to {}", peer, e);
		}

		// A saved contact reaching us has a working tunnel already. Probe back on it now instead of
		// waiting through an offline retry delay. A nonce-matched reply is still required before
		// showing the contact as available. Probes never add strangers as contacts.
		if (contactsStore == null || !contactsStore.contains(peer.asString()))
		{
			return;
		}
		var now = clock.instant();
		var contact = presenceState(peer);
		var known = tunnelFor(peer);
		if (known != null && !known.equals(tunnel))
		{
			return;
		}
		var awaitingReply = contact.deadline != null && contact.deadline.isAfter(now) && contact.nonce != null;
		if (awaitingReply || !(contact.deadline != null || contact.nextProbe == null || !now.isBefore(contact.nextProbe) ||
				List.of("offline", "checking", "unknown").contains(contact.status)))
		{
			return;
		}
		if (known == null)
		{
			presenceTunnels.put(peer, tunnel);
		}
		readyTunnels.add(tunnel);
		contact.yieldUntil = null;
		if (contact.status.equals("offline") || contact.status.equals("unknown"))
		{
			contact.status = "checking";
		}
		sendProbe(peer, contact, tunnel);
	}

	private void handlePresenceReply(GxsId peer, Location tunnel, tools.jackson.databind.JsonNode packet)
	{
		var nonce = packet.path("nonce").asString("");
		var version = packet.path("version").asInt(0);
		var status = packet.path("status").asString("");
		if (version != 1 || nonce.isBlank() || nonce.length() > 64 || !ONLINE_STATUSES.contains(status))
		{
			return;
		}
		var contactState = presenceStates.get(peer);
		var now = clock.instant();
		// Only the reply to our own, still pending probe on the same tunnel counts.
		if (contactState == null || contactState.deadline == null || !now.isBefore(contactState.deadline)
				|| !nonce.equals(contactState.nonce) || !tunnel.equals(contactState.probeTunnel))
		{
			return;
		}
		contactState.status = status;
		contactState.lastSeen = now;
		contactState.nextProbe = now.plusSeconds(PRESENCE_REFRESH_SECONDS);
		contactState.deadline = null;
		contactState.nonce = null;
		contactState.failures = 0;
		contactState.opponentId = "";
		contactState.opponentName = "";
		contactState.gameId = "";
		contactState.seeking = status.equals("available") && packet.path("seeking").asBoolean(false);
		contactState.seekTimeControl = contactState.seeking
				? ChessTimeControl.fromNetString(packet.path("tc").asString("")) : ChessTimeControl.UNLIMITED;
		var opponent = packet.path("opponent_id").asString("");
		if (status.equals("playing") && opponent.matches("[0-9a-fA-F]{32}") &&
				!GxsId.fromString(opponent).isNullIdentifier() && !opponent.equalsIgnoreCase(peer.asString()))
		{
			contactState.opponentId = opponent.toLowerCase(Locale.ROOT);
			var opponentName = packet.path("opponent_name").asString("");
			if (opponentName.isBlank())
			{
				opponentName = identities.findByGxsId(GxsId.fromString(opponent)).map(IdentityGroupItem::getName).orElse("");
			}
			contactState.opponentName = opponentName.substring(0, Math.min(256, opponentName.length()));
			var gameId = packet.path("game_id").asString("");
			contactState.gameId = gameId.length() <= 256 ? gameId : "";
		}
		savePresence(peer, contactState);
		if (leaderboard != null)
		{
			// The peer is confirmed: fetch the leaderboard receipts we miss (rate limited).
			leaderboard.handleTunnelReady(peer.asString());
		}
	}

	/// Sends a probe on a working tunnel. The reply timeout starts now, not when the
	/// asynchronous tunnel connection was requested.
	private void sendProbe(GxsId peer, ContactPresenceState contact, Location tunnel)
	{
		contact.nonce = UUID.randomUUID().toString();
		contact.probeTunnel = tunnel;
		contact.deadline = clock.instant().plusSeconds(PRESENCE_REPLY_SECONDS);
		try
		{
			var payload = mapper.writeValueAsBytes(Map.of(
					"type", "chess_presence_request",
					"version", 1,
					"nonce", contact.nonce
			));
			tunnels.sendData(tunnel, TUNNEL_SERVICE_ID, payload);
		}
		catch (RuntimeException e)
		{
			log.debug("Failed to send chess_presence_request probe to {}", peer, e);
		}
	}

	private boolean hasActiveGame()
	{
		return games.values().stream().anyMatch(g -> "ACTIVE".equals(g.status));
	}

	/// The tunnel presence uses for this peer: our presence tunnel, else the tunnel of a live game.
	private Location tunnelFor(GxsId peer)
	{
		var tunnel = presenceTunnels.get(peer);
		if (tunnel != null)
		{
			return tunnel;
		}
		var game = games.get(peer);
		return game != null && !game.released ? game.tunnel : null;
	}

	private Location readyTunnel(GxsId peer)
	{
		var tunnel = tunnelFor(peer);
		return tunnel != null && readyTunnels.contains(tunnel) ? tunnel : null;
	}

	/// Games, invitations and watch requests always keep their tunnel usable.
	private boolean tunnelInUse(GxsId peer, Location tunnel)
	{
		var game = games.get(peer);
		return game != null && !game.released && tunnel.equals(game.tunnel) || spectators.involves(peer);
	}

	/// RetroChess' `chessPeerConfirmedLocked()`: leaderboard data only goes to peers with a
	/// game or watch, peers that are not presence contacts, or contacts that answered a probe.
	private boolean chessPeerConfirmed(GxsId peer)
	{
		var game = games.get(peer);
		if (game != null && !game.released || spectators.involves(peer))
		{
			return true;
		}
		if (contactsStore == null || !contactsStore.contains(peer.asString()))
		{
			return true;
		}
		var state = presenceStates.get(peer);
		return state != null && ONLINE_STATUSES.contains(state.status);
	}

	/// Stops using the presence tunnel of a peer. It is only released when no game or watch uses it.
	private void dropPresenceTunnel(GxsId peer)
	{
		var tunnel = presenceTunnels.remove(peer);
		if (tunnel == null || tunnelInUse(peer, tunnel))
		{
			return;
		}
		readyTunnels.remove(tunnel);
		try
		{
			tunnels.releaseTunnelService(tunnel, TUNNEL_SERVICE_ID);
		}
		catch (RuntimeException e)
		{
			log.debug("Failed to release chess presence tunnel of {}", peer, e);
		}
	}

	private void openPresenceTunnel(GxsId own, GxsId peer)
	{
		try
		{
			var existing = tunnels.getTunnel(own, peer);
			// Also registers chess on a tunnel another service opened already.
			var requested = tunnels.requestSecuredTunnel(own, peer, TUNNEL_SERVICE_ID);
			var tunnel = requested != null ? requested : existing;
			if (tunnel == null)
			{
				return;
			}
			presenceTunnels.put(peer, tunnel);
			if (existing != null)
			{
				// Joining an established tunnel: its CAN_TALK notification was sent before we joined.
				// Data is queued and retried by the tunnel service, and the reply timeout still applies.
				readyTunnels.add(tunnel);
			}
		}
		catch (RuntimeException e)
		{
			log.debug("Tunnel request for chess presence failed to {}", peer, e);
		}
	}

	private ContactPresenceState presenceState(GxsId peer)
	{
		return presenceStates.computeIfAbsent(peer, _ -> {
			var state = new ContactPresenceState();
			contactsStore.find(peer.asString()).ifPresent(saved -> {
				state.lastSeen = parseInstant(saved.lastSeen());
				state.failures = Math.min(saved.failureCount(), PRESENCE_MAX_FAILURES);
				if (state.failures > 0 && saved.nextProbeSeconds() > 0)
				{
					// A past time simply means one probe right away; if it fails the backoff continues.
					state.nextProbe = Instant.ofEpochSecond(saved.nextProbeSeconds());
				}
			});
			if (!presenceStarted && state.failures == 0 &&
					(state.lastSeen == null || Duration.between(state.lastSeen, clock.instant()).toSeconds() > 24 * 3600))
			{
				// Contacts known at startup that were not seen for a day (or never) get one probe
				// and go dormant if it fails, instead of the whole fast retry burst.
				state.failures = PRESENCE_MAX_FAILURES - 1;
			}
			return state;
		});
	}

	private static Instant parseInstant(String value)
	{
		if (value == null || value.isBlank())
		{
			return null;
		}
		try
		{
			return Instant.parse(value);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	private void savePresence(GxsId peer, ContactPresenceState state)
	{
		if (contactsStore != null)
		{
			contactsStore.updatePresence(peer.asString(), state.lastSeen != null ? state.lastSeen.toString() : "", state.failures,
					state.nextProbe != null ? state.nextProbe.getEpochSecond() : 0L);
		}
	}

	private void forgetContact(GxsId peer)
	{
		presenceStates.remove(peer);
		dropPresenceTunnel(peer);
	}

	/// A contact was added: probe it right away instead of waiting through an old backoff.
	public synchronized void contactAdded(GxsId peer)
	{
		var state = presenceStates.get(peer);
		if (state != null && state.deadline == null)
		{
			state.failures = 0;
			state.nextProbe = null;
		}
	}

	/// A contact was removed: stop probing it and release its presence tunnel.
	public synchronized void contactRemoved(GxsId peer)
	{
		forgetContact(peer);
	}

	private synchronized void tickChessPresence()
	{
		if (tunnels == null || contactsStore == null || !identities.hasOwnIdentity())
		{
			return;
		}
		var ownGxsId = identities.getOwnIdentity().getGxsId();
		var now = clock.instant();
		var contacts = new ArrayList<GxsId>();
		for (var id : contactsStore.getGxsIds())
		{
			var peer = GxsId.fromString(id);
			if (!peer.isNullIdentifier() && !peer.equals(ownGxsId))
			{
				contacts.add(peer);
			}
		}
		for (var peer : List.copyOf(presenceStates.keySet()))
		{
			if (!contacts.contains(peer))
			{
				forgetContact(peer);
			}
		}
		// Bound expensive tunnel discovery, not heartbeats on working tunnels.
		var inFlight = 0;
		for (var peer : contacts)
		{
			var state = presenceStates.get(peer);
			if (state != null && state.deadline != null && readyTunnel(peer) == null)
			{
				inFlight++;
			}
		}
		for (var peer : contacts)
		{
			var contact = presenceState(peer);
			var backoffChanged = false;
			if (contact.deadline != null && !now.isBefore(contact.deadline))
			{
				contact.deadline = null;
				contact.nonce = null;
				contact.status = "offline";
				contact.seeking = false;
				contact.seekTimeControl = ChessTimeControl.UNLIMITED;
				contact.failures = Math.min(contact.failures + 1, PRESENCE_MAX_FAILURES);
				contact.nextProbe = now.plusSeconds(presenceRetryDelay(contact.failures));
				backoffChanged = true;
				log.debug("Chess presence timeout for {}, failures={}, next probe in {}s", peer, contact.failures, presenceRetryDelay(contact.failures));
				if (readyTunnel(peer) == null)
				{
					inFlight--;
				}
				// Presence failures never tear down an invitation, game or watch request.
				dropPresenceTunnel(peer);
			}
			if (contact.lastSeen != null && Duration.between(contact.lastSeen, now).toSeconds() > PRESENCE_STALE_SECONDS
					&& ONLINE_STATUSES.contains(contact.status))
			{
				contact.status = "offline";
				contact.seeking = false;
				contact.seekTimeControl = ChessTimeControl.UNLIMITED;
				contact.opponentId = "";
				contact.opponentName = "";
				contact.gameId = "";
			}
			var needsTunnel = tunnelFor(peer) == null;
			var due = contact.deadline == null && (contact.nextProbe == null || !now.isBefore(contact.nextProbe));
			var yielding = false;
			if (!needsTunnel)
			{
				contact.yieldUntil = null;
			}
			else if (due && peer.compareTo(ownGxsId) < 0)
			{
				// Both sides dialing each other produce the same tunnel id and the second handshake
				// overwrites the first, making the tunnel flap. Let the lower identity dial.
				if (contact.yieldUntil == null)
				{
					contact.yieldUntil = now.plusSeconds(PRESENCE_YIELD_SECONDS);
				}
				yielding = now.isBefore(contact.yieldUntil);
			}
			if (due && !yielding && (readyTunnel(peer) != null || inFlight < PRESENCE_MAX_IN_FLIGHT))
			{
				contact.yieldUntil = null;
				contact.deadline = now.plusSeconds(PRESENCE_DISCOVERY_SECONDS);
				contact.nonce = null;
				if (readyTunnel(peer) == null)
				{
					inFlight++;
				}
				if (contact.status.equals("unknown") || contact.status.equals("offline"))
				{
					contact.status = "checking";
				}
				if (needsTunnel)
				{
					openPresenceTunnel(ownGxsId, peer);
				}
			}
			var ready = readyTunnel(peer);
			if (contact.deadline != null && contact.nonce == null && ready != null)
			{
				sendProbe(peer, contact, ready);
			}
			if (backoffChanged)
			{
				savePresence(peer, contact);
			}
		}
		presenceStarted = true;
		// Only presence and game tunnels are looked up; forget the others (strangers probing us).
		var tracked = new HashSet<Location>(presenceTunnels.values());
		games.values().forEach(game -> {
			if (game.tunnel != null) tracked.add(game.tunnel);
		});
		readyTunnels.retainAll(tracked);
	}

	/// Presence side of a tunnel status change (RetroChess' `notifyTunnelStatus()`).
	private void presenceTunnelStatusChanged(Location tunnel, GxsId peer, GxsTunnelStatus status)
	{
		if (status == GxsTunnelStatus.CAN_TALK)
		{
			readyTunnels.add(tunnel);
			var contact = presenceStates.get(peer);
			if (contact != null && contact.deadline != null && contact.nonce == null && tunnel.equals(tunnelFor(peer)))
			{
				sendProbe(peer, contact, tunnel);
			}
			if (leaderboard != null)
			{
				leaderboard.handleTunnelReady(peer.asString());
			}
			return;
		}
		if (status != GxsTunnelStatus.TUNNEL_DOWN && status != GxsTunnelStatus.REMOTELY_CLOSED)
		{
			return;
		}
		readyTunnels.remove(tunnel);
		if (!tunnel.equals(tunnelFor(peer)))
		{
			return;
		}
		var contact = presenceStates.get(peer);
		var now = clock.instant();
		if (contact != null)
		{
			contact.status = "offline";
				contact.seeking = false;
				contact.seekTimeControl = ChessTimeControl.UNLIMITED;
			contact.deadline = null;
			contact.nonce = null;
			contact.yieldUntil = null;
			if (status == GxsTunnelStatus.REMOTELY_CLOSED)
			{
				// Closed on purpose (plugin disabled, contact removed...): normal backoff instead of redialing in 15s.
				contact.failures = Math.min(contact.failures + 1, PRESENCE_MAX_FAILURES);
				contact.nextProbe = now.plusSeconds(presenceRetryDelay(contact.failures));
				savePresence(peer, contact);
			}
			else
			{
				// Network hiccup: the tunnel is being re-dug already.
				contact.nextProbe = now.plusSeconds(presenceRetryDelay(0));
			}
		}
		if (status == GxsTunnelStatus.REMOTELY_CLOSED)
		{
			// Close our side too, otherwise the tunnel keeps being rebuilt.
			dropPresenceTunnel(peer);
		}
	}

	public synchronized List<ChessContactDTO> contacts()
	{
		var own = (identities != null && identities.hasOwnIdentity()) ? identities.getOwnIdentity().getGxsId() : null;
		var result = new ArrayList<ChessContactDTO>();
		if (contactsStore == null)
		{
			return List.of();
		}
		for (var entry : contactsStore.list())
		{
			if (own != null && entry.gxsId().equalsIgnoreCase(own.asString()))
			{
				continue;
			}
			var peer = GxsId.fromString(entry.gxsId());
			var identity = identities.findByGxsId(peer);
			var name = identity.map(IdentityGroupItem::getName).orElse(entry.gxsId());
			var state = presenceStates.get(peer);
			var status = state != null ? state.status : "unknown";
			var game = games.get(peer);
			if (game != null && "ACTIVE".equals(game.status))
			{
				status = "playing";
			}
			var lastSeen = (state != null && state.lastSeen != null) ? state.lastSeen.toString() : entry.lastSeen();
			var seeking = state != null && state.seeking && "available".equals(status);
			result.add(new ChessContactDTO(entry.gxsId(), name, status, lastSeen, seeking,
					seeking ? state.seekTimeControl.toNetString() : "unlimited"));
		}
		return result;
	}

	public synchronized boolean isBusy()
	{
		return chessBusy;
	}

	public synchronized void setBusy(boolean busy)
	{
		this.chessBusy = busy;
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
		private boolean white;
		private final Instant created = Instant.now();
		private String historyId = java.util.UUID.randomUUID().toString();
		private String historyStarted = Instant.now().toString();
		private ChessGameDTO savedHistory;
		private Instant finishedAt;
		private boolean released;
		private final List<String> moves = new ArrayList<>();
		private final List<io.xeres.chess.common.dto.chess.ChessBoardDTO> positions = new ArrayList<>();
		private final List<String> debugEvents = new ArrayList<>();
		private final Map<String, Integer> repetitions = new HashMap<>();
		private ChessPosition position = new ChessPosition();
		private Location tunnel;
		private String status;
		private String detail = "";
		private String drawNotice = "";
		private boolean incomingDraw;
		private boolean outgoingDraw;
		private boolean incomingRematch;
		private boolean outgoingRematch;
		/// Network game id shared by both players (RetroChess `game_id`), used by leaderboard receipts.
		private String gameId = "";
		/// Id we proposed in our own rematch request; crossed requests keep the smaller one.
		private String pendingRematchId;
		private boolean resultSubmitted;
		private boolean opponentLeft;
		private ChessTimeControl timeControl = ChessTimeControl.UNLIMITED;
		/// Invitation that answers our open game ("join_open_game").
		private boolean joinRequest;
		private long whiteMs;
		private long blackMs;
		/// When the clock of the side to move started running, null when no clock runs.
		private Instant clockStartedAt;

		private Game(GxsId peer, GxsId own, String name, boolean white, String status)
		{
			this.peerGxsId = peer;
			this.ownGxsId = own;
			this.name = name;
			this.white = white;
			this.status = status;
			repetitions.put(position.repetitionKey(), 1);
			positions.add(new io.xeres.chess.common.dto.chess.ChessBoardDTO(position.squares(), true, false));
		}
	}
}
