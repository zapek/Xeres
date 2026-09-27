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

import io.xeres.app.application.environment.DataDirLocator;
import io.xeres.app.service.IdentityService;
import io.xeres.chess.common.dto.chess.ChessLeaderboardEntryDTO;
import io.xeres.common.id.GxsId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/// Leaderboard and Glicko-2 ratings, built from the decentralized receipts of [ChessLeaderboard]
/// exactly like RetroChess does (both players must have reported the same result).
@Service
public class ChessRatingService
{
	private static final Logger log = LoggerFactory.getLogger(ChessRatingService.class);
	private static final String FILE_NAME = "chess-leaderboard.json";

	public static final double DEFAULT_RATING = ChessLeaderboard.DEFAULT_RATING;
	public static final double DEFAULT_RD = ChessLeaderboard.DEFAULT_RD;
	public static final double DEFAULT_VOLATILITY = ChessLeaderboard.DEFAULT_VOLATILITY;

	private final ChessHistoryStore historyStore;
	private final IdentityService identityService;
	private final Path folder;
	private ChessLeaderboard leaderboard;

	@Autowired
	public ChessRatingService(ChessHistoryStore historyStore, IdentityService identityService)
	{
		this(historyStore, identityService, null);
	}

	ChessRatingService(ChessHistoryStore historyStore, IdentityService identityService, Path folder)
	{
		this.historyStore = historyStore;
		this.identityService = identityService;
		this.folder = folder;
	}

	/// The receipt store shared with the chess network service.
	public synchronized ChessLeaderboard leaderboard()
	{
		if (leaderboard == null)
		{
			leaderboard = new ChessLeaderboard(file(), this::displayName);
		}
		return leaderboard;
	}

	private Path file()
	{
		if (folder != null)
		{
			return folder.resolve(FILE_NAME);
		}
		var data = DataDirLocator.getDataDir();
		if (data == null)
		{
			log.warn("Data directory is unavailable, the chess leaderboard will not be saved");
			return null;
		}
		return Path.of(data, FILE_NAME);
	}

	/// Identity name, else the name seen in the game history, else a short identifier (like RetroChess).
	private String displayName(String id)
	{
		try
		{
			var gxsId = GxsId.fromString(id);
			if (identityService != null && !gxsId.isNullIdentifier())
			{
				var name = identityService.findByGxsId(gxsId).map(identity -> identity.getName()).orElse(null);
				if (name != null && !name.isBlank())
				{
					return name;
				}
			}
		}
		catch (RuntimeException e)
		{
			log.debug("Cannot resolve chess identity {}", id, e);
		}
		if (historyStore != null)
		{
			try
			{
				for (var game : historyStore.list())
				{
					if (id.equalsIgnoreCase(game.whiteIdentity()) && game.whiteName() != null && !game.whiteName().isBlank())
					{
						return game.whiteName();
					}
					if (id.equalsIgnoreCase(game.blackIdentity()) && game.blackName() != null && !game.blackName().isBlank())
					{
						return game.blackName();
					}
				}
			}
			catch (IOException | RuntimeException e)
			{
				log.debug("Cannot read chess history for names", e);
			}
		}
		return id.substring(0, Math.min(12, id.length()));
	}

	public List<ChessLeaderboardEntryDTO> getLeaderboard()
	{
		var result = new ArrayList<ChessLeaderboardEntryDTO>();
		var rank = 1;
		for (var player : leaderboard().players())
		{
			result.add(toDto(rank++, player));
		}
		return Collections.unmodifiableList(result);
	}

	public ChessLeaderboardEntryDTO getRating(String peer)
	{
		var player = leaderboard().player(peer);
		if (player.isPresent())
		{
			return toDto(0, player.get());
		}
		return new ChessLeaderboardEntryDTO(0, peer, displayName(peer), (int) Math.round(DEFAULT_RATING), (int) Math.round(DEFAULT_RD),
				0, 0, 0, 0, "Provisional", null);
	}

	private static ChessLeaderboardEntryDTO toDto(int rank, ChessLeaderboard.Player player)
	{
		return new ChessLeaderboardEntryDTO(rank, player.id(), player.name(), (int) Math.round(player.rating()), (int) Math.round(player.rd()),
				player.games(), player.wins(), player.draws(), player.losses(), player.provisional() ? "Provisional" : "Active",
				player.lastPlayed() > 0 ? Instant.ofEpochSecond(player.lastPlayed()).toString() : null);
	}
}
