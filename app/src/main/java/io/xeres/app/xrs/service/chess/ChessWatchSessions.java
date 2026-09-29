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
import io.xeres.common.dto.chess.ChessWatchDTO;
import io.xeres.common.id.GxsId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/// Spectator state is separate from playable games. Access is serialized by ChessRsService.
final class ChessWatchSessions
{
	private static final Logger log = LoggerFactory.getLogger(ChessWatchSessions.class);
	private final ObjectMapper mapper;
	private final Function<GxsId, String> names;
	private final Map<GxsId, Watch> watches = new HashMap<>();
	private final Map<String, Map<GxsId, Subscription>> subscribers = new HashMap<>();
	private GxsTunnelRsService tunnels;

	record HostedGame(String id, ChessGameDTO game) {}
	private record Subscription(Location tunnel, String requestedId) {}

	private static final class Watch
	{
		private final ChessActiveGameDTO match;
		private final Location tunnel;
		private Instant accessed = Instant.now();
		private Instant requested = Instant.now();
		private Instant received;
		private ChessPosition position;
		private int sequence;
		private List<String> moves = List.of();
		private String whiteName = "";
		private String blackName = "";
		private int lastFrom = -1;
		private int lastTo = -1;
		private String status = "WAITING";
		private String detail = "";

		private Watch(ChessActiveGameDTO match, Location tunnel)
		{
			this.match = match;
			this.tunnel = tunnel;
		}
	}

	ChessWatchSessions(ObjectMapper mapper, Function<GxsId, String> names)
	{
		this.mapper = mapper;
		this.names = names;
	}

	void initialize(GxsTunnelRsService tunnels)
	{
		this.tunnels = tunnels;
	}

	ChessWatchDTO watch(GxsId own, ChessActiveGameDTO match)
	{
		var host = GxsId.fromString(match.host());
		var existing = watches.get(host);
		if (existing != null && existing.match.gameId().equals(match.gameId()) && existing.match.opponent().equals(match.opponent()) &&
				List.of("WAITING", "LIVE").contains(existing.status))
		{
			return get(host);
		}
		if (existing != null) leave(host);
		if (watches.size() >= 16) throw new IllegalArgumentException("Too many watched games");
		if (tunnels == null) throw new IllegalStateException("Chess is waiting for the network");
		var tunnel = tunnels.getTunnel(own, host);
		if (tunnel == null) tunnel = tunnels.requestSecuredTunnel(own, host, ChessRsService.TUNNEL_SERVICE_ID);
		if (tunnel == null) throw new IllegalStateException("Chess tunnel unavailable");
		var watch = new Watch(match, tunnel);
		if (!send(tunnel, packet("chess_watch_req", match.gameId()))) throw new IllegalStateException("Chess tunnel unavailable");
		watches.put(host, watch);
		return snapshot(watch);
	}

	ChessWatchDTO get(GxsId host)
	{
		var watch = watches.get(host);
		if (watch == null) throw new IllegalArgumentException("No watched game with this host");
		watch.accessed = Instant.now();
		return snapshot(watch);
	}

	void leave(GxsId host)
	{
		var watch = watches.remove(host);
		if (watch != null)
		{
			// PR #25 indexes subscriptions by the host's opponent, even when the request used a game UUID.
			send(watch.tunnel, packet("chess_watch_leave", watch.match.opponent()));
			// Do not release a tunnel that may also carry a game or presence probes.
		}
	}

	void maintain()
	{
		var now = Instant.now();
		for (var host : List.copyOf(watches.keySet()))
		{
			var watch = watches.get(host);
			if (watch.accessed.plusSeconds(60).isBefore(now))
			{
				leave(host);
				continue;
			}
			if (!List.of("WAITING", "LIVE").contains(watch.status)) continue;
			if (watch.received == null && watch.requested.plusSeconds(45).isBefore(now) ||
					watch.received != null && watch.received.plusSeconds(90).isBefore(now))
			{
				watch.status = "UNAVAILABLE";
				send(watch.tunnel, packet("chess_watch_leave", watch.match.opponent()));
			}
			else if (watch.received != null && watch.requested.plusSeconds(30).isBefore(now))
			{
				requestState(watch);
			}
		}
		subscribers.values().removeIf(Map::isEmpty);
	}

	void connectionChanged(GxsId peer, Location tunnel, GxsTunnelStatus status)
	{
		var watch = watches.get(peer);
		if (watch != null && watch.tunnel.equals(tunnel) && List.of("WAITING", "LIVE").contains(watch.status))
		{
			if (status == GxsTunnelStatus.REMOTELY_CLOSED) watch.status = "UNAVAILABLE";
			else if (status == GxsTunnelStatus.CAN_TALK) requestState(watch);
			else watch.status = "WAITING";
		}
		if (status == GxsTunnelStatus.REMOTELY_CLOSED)
		{
			for (var entries : subscribers.values())
			{
				var subscription = entries.get(peer);
				if (subscription != null && subscription.tunnel().equals(tunnel)) entries.remove(peer);
			}
		}
	}

	void handle(GxsId peer, Location tunnel, JsonNode packet, List<HostedGame> games)
	{
		if (packet.path("version").asInt(0) != 1) return;
		var type = packet.path("type").asString();
		var id = packet.path("game_id").asString("");
		if (id.length() > 256) return;
		if (type.equals("chess_watch_req"))
		{
			var match = games.stream().filter(g -> g.game().status().equals("ACTIVE") && matches(g, id)).findFirst();
			if (match.isEmpty())
			{
				var end = packet("chess_watch_end", id);
				end.put("reason", "Game not active");
				send(tunnel, end);
				return;
			}
			var game = match.get();
			var entries = subscribers.computeIfAbsent(game.id(), _ -> new HashMap<>());
			if (entries.size() >= 32 && !entries.containsKey(peer)) return;
			entries.put(peer, new Subscription(tunnel, id));
			send(tunnel, state(game, id));
			return;
		}
		if (type.equals("chess_watch_leave"))
		{
			for (var game : games)
			{
				if (!matches(game, id)) continue;
				var entries = subscribers.get(game.id());
				if (entries != null) entries.remove(peer);
			}
			return;
		}
		var watch = watches.get(peer);
		if (watch == null || !watch.tunnel.equals(tunnel) || !List.of("WAITING", "LIVE").contains(watch.status) ||
				!(id.equals(watch.match.gameId()) || id.equals(watch.match.opponent()))) return;
		switch (type)
		{
			case "chess_watch_state" -> receiveState(watch, packet);
			case "chess_watch_action" ->
			{
				try
				{
					receiveAction(watch, packet.path("action").asString(""));
				}
				catch (IllegalArgumentException e)
				{
					// Recover a missed/out-of-order move without touching any playable game.
					requestState(watch);
				}
			}
			case "chess_watch_end" ->
			{
				watch.status = "ENDED";
				var reason = packet.path("reason").asString("");
				watch.detail = reason.substring(0, Math.min(256, reason.length()));
				watch.received = Instant.now();
			}
			default -> { }
		}
	}

	private void receiveState(Watch watch, JsonNode packet)
	{
		var whiteId = packet.path("white_id").asString("");
		var blackId = packet.path("black_id").asString("");
		if (!(whiteId.equalsIgnoreCase(watch.match.host()) && blackId.equalsIgnoreCase(watch.match.opponent()) ||
				blackId.equalsIgnoreCase(watch.match.host()) && whiteId.equalsIgnoreCase(watch.match.opponent()))) return;
		var sequence = packet.path("sequence").asInt(-1);
		if (sequence < watch.sequence || sequence > 20000) return;
		var position = ChessPosition.fromFen(packet.path("fen").asString(""));
		var history = new ArrayList<String>();
		var moves = packet.path("moves");
		if (moves.isArray())
		{
			if (moves.size() > 20000) return;
			for (var move : moves)
			{
				var value = move.asString("");
				if (value.length() > 64) return;
				history.add(value);
			}
		}
		watch.position = position;
		watch.sequence = sequence;
		watch.moves = List.copyOf(history);
		watch.whiteName = displayName(packet.path("white_name").asString(""), whiteId);
		watch.blackName = displayName(packet.path("black_name").asString(""), blackId);
		watch.lastFrom = square(packet.path("last_from").asInt(-1));
		watch.lastTo = square(packet.path("last_to").asInt(-1));
		watch.received = Instant.now();
		watch.status = "LIVE";
		watch.detail = "";
	}

	private void receiveAction(Watch watch, String action)
	{
		if (watch.position == null)
		{
			requestState(watch);
			return;
		}
		if (action.startsWith("move:"))
		{
			var parts = action.split(":", -1);
			if (parts.length != 6) throw new IllegalArgumentException("Unsequenced spectator move");
			var sequence = Integer.parseInt(parts[1]);
			if (sequence <= watch.sequence) return;
			if (sequence != watch.sequence + 1 || !parts[4].matches("[-QRBH]")) throw new IllegalArgumentException("Invalid spectator move");
			var from = Integer.parseInt(parts[2]);
			var to = Integer.parseInt(parts[3]);
			var uci = ChessPosition.square(from) + ChessPosition.square(to) +
					(parts[4].equals("-") ? "" : parts[4].equals("H") ? "n" : parts[4].toLowerCase(Locale.ROOT));
			var next = watch.position.move(uci);
			if (!next.hash().equals(parts[5])) throw new IllegalArgumentException("Spectator position mismatch");
			watch.position = next;
			watch.sequence = sequence;
			var moves = new ArrayList<>(watch.moves);
			moves.add(uci);
			watch.moves = List.copyOf(moves);
			watch.lastFrom = from;
			watch.lastTo = to;
		}
		else if (List.of("resign", "abort", "draw_accept", "draw_repetition", "draw_fifty_move").contains(action))
		{
			watch.status = "ENDED";
		}
		watch.received = Instant.now();
	}

	void publish(List<HostedGame> games)
	{
		for (var game : games)
		{
			var entries = subscribers.get(game.id());
			if (entries == null || entries.isEmpty()) continue;
			for (var subscription : entries.values())
			{
				send(subscription.tunnel(), state(game, subscription.requestedId()));
				if (!game.game().status().equals("ACTIVE"))
				{
					var end = packet("chess_watch_end", subscription.requestedId());
					end.put("reason", game.game().status());
					send(subscription.tunnel(), end);
				}
			}
			if (!game.game().status().equals("ACTIVE")) subscribers.remove(game.id());
		}
		// A replaced game must not attach its old spectators to a rematch.
		var retained = games.stream().map(HostedGame::id).toList();
		for (var id : List.copyOf(subscribers.keySet()))
		{
			if (retained.contains(id)) continue;
			for (var subscription : subscribers.remove(id).values())
			{
				var end = packet("chess_watch_end", subscription.requestedId());
				end.put("reason", "Game ended");
				send(subscription.tunnel(), end);
			}
		}
	}

	private Map<String, Object> state(HostedGame hosted, String id)
	{
		var game = hosted.game();
		var state = packet("chess_watch_state", id);
		state.put("white_id", game.white() ? game.localIdentity() : game.peer());
		state.put("black_id", game.white() ? game.peer() : game.localIdentity());
		var ownName = names.apply(GxsId.fromString(game.localIdentity()));
		state.put("white_name", game.white() ? ownName : game.name());
		state.put("black_name", game.white() ? game.name() : ownName);
		state.put("fen", game.fen());
		state.put("sequence", game.moves().size());
		state.put("moves", game.moves());
		var lastMove = game.moves().isEmpty() ? "" : game.moves().getLast();
		state.put("last_from", lastMove.isEmpty() ? -1 : ChessPosition.index(lastMove.substring(0, 2)));
		state.put("last_to", lastMove.isEmpty() ? -1 : ChessPosition.index(lastMove.substring(2, 4)));
		return state;
	}

	private static boolean matches(HostedGame game, String id)
	{
		var first = game.game().localIdentity();
		var second = game.game().peer();
		var pair = first.compareTo(second) < 0 ? first + ":" + second : second + ":" + first;
		return id.equals(game.id()) || id.equals(second) || id.equals("contact:" + pair) ||
				id.equals("contact_game:" + pair.replace(':', '_'));
	}

	private void requestState(Watch watch)
	{
		// Bound retries when several packets arrive ahead of a full snapshot.
		if (watch.requested.plusSeconds(2).isAfter(Instant.now())) return;
		send(watch.tunnel, packet("chess_watch_req", watch.match.gameId()));
		watch.requested = Instant.now();
	}

	private boolean send(Location tunnel, Map<String, Object> packet)
	{
		try
		{
			return tunnels != null && tunnels.sendData(tunnel, ChessRsService.TUNNEL_SERVICE_ID, mapper.writeValueAsBytes(packet));
		}
		catch (RuntimeException e)
		{
			log.debug("Unable to send chess spectator packet", e);
			return false;
		}
	}

	private static Map<String, Object> packet(String type, String id)
	{
		var packet = new HashMap<String, Object>();
		packet.put("type", type);
		packet.put("version", 1);
		packet.put("game_id", id);
		return packet;
	}

	private static int square(int value)
	{
		return value >= 0 && value < 64 ? value : -1;
	}

	private static String displayName(String value, String fallback)
	{
		return value.isBlank() ? fallback : value.substring(0, Math.min(256, value.length()));
	}

	private static ChessWatchDTO snapshot(Watch watch)
	{
		return new ChessWatchDTO(watch.match.host(), watch.match.gameId(), watch.whiteName, watch.blackName,
				watch.position == null ? "" : watch.position.squares(), watch.position == null || watch.position.isWhiteToMove(),
				watch.sequence, watch.moves, watch.lastFrom, watch.lastTo, watch.status, watch.detail);
	}
}
