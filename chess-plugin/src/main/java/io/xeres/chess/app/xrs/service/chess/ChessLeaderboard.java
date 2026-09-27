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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/// Decentralized chess leaderboard, a port of RetroChess' `RetroChessLeaderboard`.
///
/// Both clients must use the same rules, otherwise the same receipts produce different
/// leaderboards on RetroShare and on Xeres:
///
/// - every rated game produces one receipt per player (`signer` = white or black);
/// - a game only counts once both players' receipts with the same result are known;
/// - a receipt received from its signer over the signer's own tunnel is first-hand;
///   a receipt relayed by somebody else only counts once [#MIN_WITNESSES] distinct peers reported it;
/// - ratings are recomputed from scratch with Glicko-2, ordered by `finished_at` then `game_id`.
///
/// Wire format (GXS tunnel service `0xC4E5`): `leaderboard_receipt`, `leaderboard_sync_req`
/// (with `epoch`/`since` for incremental sync) and `leaderboard_sync` (batches of 10, the last
/// batch carries `epoch`, `seq` and `final`). The storage file uses the same JSON layout as
/// RetroChess' `retrochess_leaderboard.json`.
public class ChessLeaderboard
{
	private static final Logger log = LoggerFactory.getLogger(ChessLeaderboard.class);

	/// Network access, provided by the chess service.
	public interface Transport
	{
		/// Sends data over the chess tunnel of a peer. Returns false when there is no usable tunnel.
		boolean send(String peer, byte[] data);

		/// Peers with a usable tunnel whose chess presence is confirmed (RetroChess' `activeGxsTunnels()`).
		List<String> activePeers();

		/// True when the peer answered a presence probe recently (available, busy or playing).
		boolean isOnline(String peer);
	}

	static final double DEFAULT_RATING = 1500.0;
	static final double DEFAULT_RD = 350.0;
	static final double DEFAULT_VOLATILITY = 0.06;
	private static final double SCALE = 173.7178;
	private static final double TAU = 0.5;

	static final long SYNC_REQUEST_INTERVAL_MS = 5 * 60 * 1000L;
	static final long INCREMENTAL_SYNC_MIN_INTERVAL_MS = 60 * 1000L;
	static final long FULL_SYNC_MIN_INTERVAL_MS = 5 * 60 * 1000L;
	static final long LEGACY_FULL_SYNC_MIN_INTERVAL_MS = 30 * 60 * 1000L;
	static final int SYNC_BATCH_SIZE = 10;
	static final int MIN_WITNESSES = 2;
	static final int MAX_WITNESSES = 8;
	static final int MAX_RECEIPTS = 50000;
	static final int MAX_PENDING = 2000;
	private static final long COMMIT_DELAY_MS = 500;

	private static final Pattern GXS_ID = Pattern.compile("[0-9a-fA-F]{32}");
	private static final String NULL_GXS_ID = "0".repeat(32);

	private final JsonMapper mapper = JsonMapper.builder().build();
	private final Path file;
	private final Function<String, String> names;
	private final LongSupplier clockMs;
	private final LongSupplier nowSeconds;
	private Transport transport;

	private final Map<String, Receipt> receipts = new LinkedHashMap<>();
	private final Map<String, Receipt> pending = new LinkedHashMap<>();
	private final Map<String, Set<String>> witnesses = new HashMap<>();
	private final Set<String> gossiped = new LinkedHashSet<>();
	private Map<String, Player> players = new HashMap<>();

	private final Map<String, Long> lastSyncRequest = new HashMap<>();
	private final Map<String, Long> lastSyncResponse = new HashMap<>();
	private final Map<String, Long> lastFullSyncResponse = new HashMap<>();
	private final Map<String, SyncCursor> syncCursors = new HashMap<>();
	private final String syncEpoch = UUID.randomUUID().toString();
	private long nextSeq;
	private long lastPeriodicSync = Long.MIN_VALUE;

	private boolean loaded;
	private boolean saveNeeded;
	private boolean recomputeNeeded;
	private long commitAt = -1;

	static final class Receipt
	{
		final String gameId;
		final String white;
		final String black;
		final String result;
		final String signer;
		final long finishedAt;
		/// Local insertion order, used for incremental sync. Never saved nor sent.
		long seq;
		/// Peer this receipt was learned from, never echoed back to it by the sync.
		String learnedFrom = "";

		Receipt(String gameId, String white, String black, String result, String signer, long finishedAt)
		{
			this.gameId = gameId == null ? "" : gameId;
			this.white = white == null ? "" : white;
			this.black = black == null ? "" : black;
			this.result = result == null ? "" : result;
			this.signer = signer == null ? "" : signer;
			this.finishedAt = finishedAt;
		}

		String key()
		{
			return canonicalKey(this) + '|' + signer;
		}
	}

	private record SyncCursor(String epoch, long seq)
	{
	}

	/// Leaderboard entry, same fields and provisional rule as RetroChess.
	public static final class Player
	{
		private final String id;
		private String name = "";
		private double rating = DEFAULT_RATING;
		private double rd = DEFAULT_RD;
		private double volatility = DEFAULT_VOLATILITY;
		private int wins;
		private int draws;
		private int losses;
		private long lastPlayed;

		Player(String id)
		{
			this.id = id;
		}

		private Player copy()
		{
			var copy = new Player(id);
			copy.name = name;
			copy.rating = rating;
			copy.rd = rd;
			copy.volatility = volatility;
			copy.wins = wins;
			copy.draws = draws;
			copy.losses = losses;
			copy.lastPlayed = lastPlayed;
			return copy;
		}

		public String id()
		{
			return id;
		}

		public String name()
		{
			return name;
		}

		public double rating()
		{
			return rating;
		}

		public double rd()
		{
			return rd;
		}

		public double volatility()
		{
			return volatility;
		}

		public int wins()
		{
			return wins;
		}

		public int draws()
		{
			return draws;
		}

		public int losses()
		{
			return losses;
		}

		public int games()
		{
			return wins + draws + losses;
		}

		public boolean provisional()
		{
			return games() < 10 || rd > 110.0;
		}

		/// Seconds since the epoch.
		public long lastPlayed()
		{
			return lastPlayed;
		}
	}

	public ChessLeaderboard(Path file, Function<String, String> names)
	{
		this(file, names, () -> System.nanoTime() / 1_000_000L, () -> System.currentTimeMillis() / 1000L);
	}

	ChessLeaderboard(Path file, Function<String, String> names, LongSupplier clockMs, LongSupplier nowSeconds)
	{
		this.file = file;
		this.names = names != null ? names : id -> id.substring(0, Math.min(12, id.length()));
		this.clockMs = clockMs;
		this.nowSeconds = nowSeconds;
	}

	public synchronized void setTransport(Transport transport)
	{
		this.transport = transport;
	}

	static boolean validResult(String result)
	{
		return "1-0".equals(result) || "0-1".equals(result) || "1/2-1/2".equals(result);
	}

	static String canonicalKey(Receipt r)
	{
		return r.gameId + '|' + r.white + '|' + r.black + '|' + r.result;
	}

	private static boolean validId(String id)
	{
		return id != null && GXS_ID.matcher(id).matches() && !id.equals(NULL_GXS_ID);
	}

	private static boolean valid(Receipt r)
	{
		return !r.gameId.isEmpty() && r.gameId.length() <= 128 && !r.white.equals(r.black)
				&& validId(r.white) && validId(r.black) && r.finishedAt > 0 && validResult(r.result)
				&& (r.signer.equals(r.white) || r.signer.equals(r.black));
	}

	/// Stores and publishes our own receipt for a finished rated game.
	///
	/// @param ownId our identity, must be white or black
	public synchronized void submitResult(String gameId, String white, String black, String result, String ownId)
	{
		ensureLoaded();
		if (gameId == null || gameId.isEmpty() || white == null || black == null || white.equals(black) || ownId == null
				|| (!ownId.equals(white) && !ownId.equals(black)) || !validResult(result))
		{
			return;
		}
		var receipt = new Receipt(gameId, white, black, result, ownId, nowSeconds.getAsLong());
		log.debug("Leaderboard: own result game={} result={}", gameId, result);
		// Our own receipt: the signer is our identity, so it is first-hand.
		consumeReceipt(receipt, ownId);
		broadcastReceipt(receipt, null);
		// Our own game: show the new rating right away.
		commitChanges();
	}

	/// Accepts a receipt. The sender is the authenticated identity of the tunnel it arrived on.
	///
	/// @return true when the receipt was stored
	synchronized boolean consumeReceipt(Receipt r, String sender)
	{
		ensureLoaded();
		if (!valid(r))
		{
			log.debug("Leaderboard: rejected invalid receipt from {} game={}", sender, r.gameId);
			return false;
		}
		var key = r.key();
		// Duplicate posts cannot count twice. Keep the earliest timestamp regardless of arrival order.
		var existing = receipts.get(key);
		if (existing != null && existing.finishedAt <= r.finishedAt)
		{
			return false;
		}

		// Only the signer itself can vouch for its own receipt. Anything relayed by another peer is hearsay.
		var firstHand = sender != null && !sender.isEmpty() && sender.equals(r.signer);
		if (!firstHand)
		{
			if (sender == null || sender.isEmpty())
			{
				return false;
			}
			if (!pending.containsKey(key))
			{
				if (pending.size() >= MAX_PENDING)
				{
					return false;
				}
				pending.put(key, r);
			}
			var seen = witnesses.computeIfAbsent(key, _ -> new HashSet<>());
			if (seen.size() < MAX_WITNESSES)
			{
				seen.add(sender);
			}
			if (seen.size() < MIN_WITNESSES)
			{
				return false;
			}
			// Corroborated by enough distinct peers: accept the first version seen.
			var accepted = pending.remove(key);
			witnesses.remove(key);
			if (receipts.size() >= MAX_RECEIPTS && !receipts.containsKey(key))
			{
				return false;
			}
			storeReceipt(key, accepted, sender);
		}
		else
		{
			pending.remove(key);
			witnesses.remove(key);
			if (receipts.size() >= MAX_RECEIPTS && !receipts.containsKey(key))
			{
				return false;
			}
			storeReceipt(key, r, sender);
		}
		scheduleCommit(true);
		return true;
	}

	private void storeReceipt(String key, Receipt receipt, String from)
	{
		var stored = new Receipt(receipt.gameId, receipt.white, receipt.black, receipt.result, receipt.signer, receipt.finishedAt);
		stored.learnedFrom = from != null ? from : "";
		// A replaced receipt gets a new number too, so that peers pick up the correction incrementally.
		stored.seq = ++nextSeq;
		receipts.put(key, stored);
	}

	private void scheduleCommit(boolean ratingsChanged)
	{
		saveNeeded = true;
		if (ratingsChanged)
		{
			recomputeNeeded = true;
		}
		if (commitAt < 0)
		{
			commitAt = clockMs.getAsLong() + COMMIT_DELAY_MS;
		}
	}

	/// Saves and recomputes pending changes. Bursts of receipts are committed once.
	public synchronized void commitChanges()
	{
		commitAt = -1;
		var save = saveNeeded;
		var recompute = recomputeNeeded;
		saveNeeded = false;
		recomputeNeeded = false;
		if (save)
		{
			save();
		}
		if (recompute)
		{
			recompute();
		}
	}

	/// Periodic work: delayed commits and the 5 minutes sync requests. Called by the chess service.
	public synchronized void tick()
	{
		ensureLoaded();
		var now = clockMs.getAsLong();
		if (commitAt >= 0 && now >= commitAt)
		{
			commitChanges();
		}
		if (lastPeriodicSync == Long.MIN_VALUE || now - lastPeriodicSync >= SYNC_REQUEST_INTERVAL_MS)
		{
			lastPeriodicSync = now;
			synchronizeTunnels();
		}
	}

	private void recompute()
	{
		var result = new HashMap<String, Player>();
		var byGame = new HashMap<String, List<Receipt>>();
		for (var r : receipts.values())
		{
			byGame.computeIfAbsent(r.gameId, _ -> new ArrayList<>()).add(r);
		}
		var confirmed = new ArrayList<Receipt>();
		for (var list : byGame.values())
		{
			if (list.size() != 2)
			{
				continue;
			}
			found:
			for (var a : list)
			{
				for (var b : list)
				{
					if (a.signer.equals(a.white) && b.signer.equals(a.black) && canonicalKey(a).equals(canonicalKey(b)))
					{
						confirmed.add(a);
						break found;
					}
				}
			}
		}
		confirmed.sort(Comparator.<Receipt>comparingLong(r -> r.finishedAt).thenComparing(r -> r.gameId));
		for (var r : confirmed)
		{
			var w = result.computeIfAbsent(r.white, Player::new);
			var b = result.computeIfAbsent(r.black, Player::new);
			var score = "1-0".equals(r.result) ? 1.0 : ("0-1".equals(r.result) ? 0.0 : 0.5);
			updatePair(w, b, score);
			if (score == 1.0)
			{
				w.wins++;
				b.losses++;
			}
			else if (score == 0.0)
			{
				w.losses++;
				b.wins++;
			}
			else
			{
				w.draws++;
				b.draws++;
			}
			w.lastPlayed = r.finishedAt;
			b.lastPlayed = r.finishedAt;
		}
		for (var player : result.values())
		{
			String name = null;
			try
			{
				name = names.apply(player.id);
			}
			catch (RuntimeException e)
			{
				log.debug("Leaderboard: cannot resolve name of {}", player.id, e);
			}
			player.name = name == null || name.isBlank() ? player.id.substring(0, Math.min(12, player.id.length())) : name;
		}
		players = result;
	}

	private static void updatePair(Player a, Player b, double scoreA)
	{
		var oldA = a.copy();
		var oldB = b.copy();
		next(a, oldA, oldB, scoreA);
		next(b, oldB, oldA, 1.0 - scoreA);
	}

	private static void next(Player target, Player p, Player op, double score)
	{
		var mu = (p.rating - DEFAULT_RATING) / SCALE;
		var phi = p.rd / SCALE;
		var muJ = (op.rating - DEFAULT_RATING) / SCALE;
		var phiJ = op.rd / SCALE;
		var g = 1.0 / Math.sqrt(1.0 + 3.0 * phiJ * phiJ / (3.14159265358979323846 * 3.14159265358979323846));
		var e = 1.0 / (1.0 + Math.exp(-g * (mu - muJ)));
		var variance = 1.0 / (g * g * e * (1.0 - e));
		var delta = variance * g * (score - e);
		var volatility = newVolatility(phi, delta, variance, p.volatility);
		var phiStar = Math.sqrt(phi * phi + volatility * volatility);
		var phiPrime = 1.0 / Math.sqrt(1.0 / (phiStar * phiStar) + 1.0 / variance);
		var muPrime = mu + phiPrime * phiPrime * g * (score - e);
		target.volatility = volatility;
		target.rating = DEFAULT_RATING + SCALE * muPrime;
		target.rd = SCALE * phiPrime;
	}

	private static double newVolatility(double phi, double delta, double variance, double sigma)
	{
		var a = Math.log(sigma * sigma);
		java.util.function.DoubleUnaryOperator f = x -> {
			var ex = Math.exp(x);
			return ex * (delta * delta - phi * phi - variance - ex)
					/ (2.0 * Math.pow(phi * phi + variance + ex, 2.0)) - (x - a) / (TAU * TAU);
		};
		var bigA = a;
		double bigB;
		if (delta * delta > phi * phi + variance)
		{
			bigB = Math.log(delta * delta - phi * phi - variance);
		}
		else
		{
			var k = 1;
			while (f.applyAsDouble(a - k * TAU) < 0.0)
			{
				++k;
			}
			bigB = a - k * TAU;
		}
		var fA = f.applyAsDouble(bigA);
		var fB = f.applyAsDouble(bigB);
		while (Math.abs(bigB - bigA) > 0.000001)
		{
			var c = bigA + (bigA - bigB) * fA / (fB - fA);
			var fC = f.applyAsDouble(c);
			if (fC * fB <= 0.0)
			{
				bigA = bigB;
				fA = fB;
			}
			else
			{
				fA /= 2.0;
			}
			bigB = c;
			fB = fC;
		}
		return Math.exp(bigA / 2.0);
	}

	/// Players with at least one confirmed game, best rating first.
	public synchronized List<Player> players()
	{
		ensureLoaded();
		if (recomputeNeeded)
		{
			commitChanges();
		}
		var list = new ArrayList<>(players.values());
		list.sort(Comparator.comparingDouble(Player::rating).reversed().thenComparing(Comparator.comparingInt(Player::games).reversed()));
		return list;
	}

	public synchronized Optional<Player> player(String id)
	{
		ensureLoaded();
		if (recomputeNeeded)
		{
			commitChanges();
		}
		return Optional.ofNullable(players.get(id));
	}

	synchronized int receiptCount()
	{
		ensureLoaded();
		return receipts.size();
	}

	synchronized int pendingCount()
	{
		return pending.size();
	}

	// ---------------------------------------------------------------------------------------
	// Network
	// ---------------------------------------------------------------------------------------

	private ObjectNode receiptJson(Receipt r)
	{
		var node = mapper.createObjectNode();
		node.put("game_id", r.gameId);
		node.put("white", r.white);
		node.put("black", r.black);
		node.put("result", r.result);
		node.put("signer", r.signer);
		node.put("finished_at", r.finishedAt);
		return node;
	}

	private static Receipt receiptFrom(JsonNode o)
	{
		return new Receipt(o.path("game_id").asString(""), o.path("white").asString(""), o.path("black").asString(""),
				o.path("result").asString(""), o.path("signer").asString(""), (long) o.path("finished_at").asDouble(0.0));
	}

	private boolean send(String peer, ObjectNode message)
	{
		if (transport == null)
		{
			return false;
		}
		try
		{
			return transport.send(peer, mapper.writeValueAsBytes(message));
		}
		catch (RuntimeException e)
		{
			log.debug("Leaderboard: failed to send {} to {}", message.path("type").asString(), peer, e);
			return false;
		}
	}

	/// Sends a receipt to every confirmed peer, except the one we learned it from.
	private void broadcastReceipt(Receipt r, String excludePeer)
	{
		if (transport == null)
		{
			return;
		}
		var key = r.key();
		gossiped.add(key);
		if (receipts.containsKey(key))
		{
			scheduleCommit(false);
		}
		var message = receiptJson(r);
		message.put("type", "leaderboard_receipt");
		message.put("version", 1);
		for (var peer : transport.activePeers())
		{
			if (excludePeer == null || !peer.equals(excludePeer))
			{
				send(peer, message);
			}
		}
	}

	private void sendSyncToPeer(String peer, JsonNode request)
	{
		if (transport == null || peer == null || receipts.isEmpty())
		{
			return;
		}
		// Incremental only when the peer quotes our current epoch and a sequence number we could have given it.
		var hasCursor = request.has("since") && request.has("epoch");
		var since = (long) request.path("since").asDouble(0.0);
		var incremental = hasCursor && syncEpoch.equals(request.path("epoch").asString("")) && since <= nextSeq;
		var now = clockMs.getAsLong();
		var lastAnswer = lastSyncResponse.get(peer);
		if (lastAnswer != null && now - lastAnswer < INCREMENTAL_SYNC_MIN_INTERVAL_MS)
		{
			return;
		}
		var fullInterval = hasCursor ? FULL_SYNC_MIN_INTERVAL_MS : LEGACY_FULL_SYNC_MIN_INTERVAL_MS;
		var lastFull = lastFullSyncResponse.get(peer);
		if (!incremental && lastFull != null && now - lastFull < fullInterval)
		{
			return;
		}
		lastSyncResponse.put(peer, now);
		if (!incremental)
		{
			lastFullSyncResponse.put(peer, now);
		}

		var toSend = new ArrayList<Receipt>();
		for (var r : receipts.values())
		{
			if ((!incremental || r.seq > since) && !peer.equals(r.learnedFrom))
			{
				toSend.add(r);
			}
		}
		// Nothing new: the peer is already up to date.
		if (toSend.isEmpty())
		{
			return;
		}
		toSend.sort(Comparator.comparingLong(r -> r.seq));
		log.debug("Leaderboard: sync answer to {} ({}): {} of {} results", peer, incremental ? "incremental" : "full", toSend.size(), receipts.size());
		for (var start = 0; start < toSend.size(); start += SYNC_BATCH_SIZE)
		{
			var end = Math.min(start + SYNC_BATCH_SIZE, toSend.size());
			var batch = mapper.createArrayNode();
			for (var i = start; i < end; i++)
			{
				batch.add(receiptJson(toSend.get(i)));
			}
			var message = mapper.createObjectNode();
			message.put("type", "leaderboard_sync");
			message.put("version", 1);
			message.set("receipts", batch);
			// The last batch carries the cursor the peer quotes next time.
			if (end == toSend.size())
			{
				message.put("epoch", syncEpoch);
				message.put("seq", nextSeq);
				message.put("final", true);
			}
			send(peer, message);
		}
	}

	private void sendSyncRequest(String peer)
	{
		if (transport == null || peer == null || !transport.isOnline(peer))
		{
			return;
		}
		var now = clockMs.getAsLong();
		var last = lastSyncRequest.get(peer);
		if (last != null && now - last < SYNC_REQUEST_INTERVAL_MS)
		{
			return;
		}
		var cursor = syncCursors.getOrDefault(peer, new SyncCursor("", 0));
		var request = mapper.createObjectNode();
		request.put("type", "leaderboard_sync_req");
		request.put("version", 1);
		// Ask only for what this peer has not sent us yet. An empty epoch requests the full history.
		request.put("epoch", cursor.epoch());
		request.put("since", cursor.seq());
		if (send(peer, request))
		{
			lastSyncRequest.put(peer, now);
		}
	}

	/// A chess tunnel to this peer became usable (or its presence was just confirmed).
	public synchronized void handleTunnelReady(String peer)
	{
		ensureLoaded();
		if (peer != null && !peer.isEmpty())
		{
			sendSyncRequest(peer);
		}
	}

	/// Handles `leaderboard_receipt`, `leaderboard_sync` and `leaderboard_sync_req`.
	///
	/// @param sender authenticated identity of the tunnel peer
	public synchronized void handleTunnelData(String sender, JsonNode message)
	{
		ensureLoaded();
		var type = message.path("type").asString("");
		switch (type)
		{
			case "leaderboard_sync_req" -> sendSyncToPeer(sender, message);
			case "leaderboard_receipt" ->
			{
				var r = receiptFrom(message);
				receiveRelayable(r, sender);
			}
			case "leaderboard_sync" ->
			{
				// Remember how far we got with this peer (sent on its last batch only).
				if (message.path("final").asBoolean(false) && !message.path("epoch").asString("").isEmpty())
				{
					syncCursors.put(sender, new SyncCursor(message.path("epoch").asString(""), (long) message.path("seq").asDouble(0.0)));
				}
				var array = message.path("receipts");
				if (array.isArray())
				{
					for (var value : array)
					{
						receiveRelayable(receiptFrom(value), sender);
					}
				}
			}
			default -> log.debug("Leaderboard: ignoring {}", type);
		}
	}

	private void receiveRelayable(Receipt r, String sender)
	{
		var key = r.key();
		var isNew = !receipts.containsKey(key) && !pending.containsKey(key);
		consumeReceipt(r, sender);
		// Relay newly seen receipts (also unconfirmed ones, so that peers further away can collect witnesses).
		if (isNew && (receipts.containsKey(key) || pending.containsKey(key)) && !gossiped.contains(key))
		{
			broadcastReceipt(r, sender);
		}
	}

	/// Asks every confirmed peer for the receipts it has and we have not seen yet.
	public synchronized void synchronizeTunnels()
	{
		ensureLoaded();
		if (transport == null)
		{
			return;
		}
		var active = transport.activePeers();
		var now = clockMs.getAsLong();
		lastSyncRequest.keySet().retainAll(active);
		lastSyncResponse.values().removeIf(time -> now - time >= INCREMENTAL_SYNC_MIN_INTERVAL_MS);
		lastFullSyncResponse.values().removeIf(time -> now - time >= LEGACY_FULL_SYNC_MIN_INTERVAL_MS);
		for (var peer : active)
		{
			sendSyncRequest(peer);
		}
	}

	// ---------------------------------------------------------------------------------------
	// Storage
	// ---------------------------------------------------------------------------------------

	private void ensureLoaded()
	{
		if (loaded)
		{
			return;
		}
		loaded = true;
		load();
	}

	private void load()
	{
		receipts.clear();
		gossiped.clear();
		if (file != null && Files.exists(file))
		{
			try
			{
				var root = mapper.readTree(Files.readAllBytes(file));
				var array = root.path("receipts");
				if (array.isArray())
				{
					for (var value : array)
					{
						var r = receiptFrom(value);
						if (valid(r))
						{
							storeReceipt(r.key(), r, "");
						}
					}
				}
				var sent = root.path("gossiped");
				if (sent.isArray())
				{
					for (var value : sent)
					{
						var key = value.asString("");
						if (!key.isEmpty())
						{
							gossiped.add(key);
						}
					}
				}
			}
			catch (IOException | RuntimeException e)
			{
				log.warn("Unable to read the chess leaderboard {}", file, e);
			}
		}
		recompute();
	}

	private void save()
	{
		if (file == null)
		{
			return;
		}
		var root = mapper.createObjectNode();
		root.put("version", 1);
		var array = root.putArray("receipts");
		receipts.values().forEach(r -> array.add(receiptJson(r)));
		var sent = root.putArray("gossiped");
		gossiped.forEach(sent::add);
		try
		{
			var parent = file.getParent();
			if (parent != null)
			{
				Files.createDirectories(parent);
			}
			var temporary = Files.createTempFile(parent != null ? parent : Path.of("."), ".chess-leaderboard-", ".tmp");
			try
			{
				Files.write(temporary, mapper.writeValueAsBytes(root));
				try
				{
					Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
				}
				catch (AtomicMoveNotSupportedException ignored)
				{
					Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			finally
			{
				Files.deleteIfExists(temporary);
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.error("Unable to save the chess leaderboard to {}", file, e);
		}
	}
}
