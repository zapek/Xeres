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

import io.xeres.app.service.IdentityService;
import io.xeres.common.dto.chess.ChessHistorySummaryDTO;
import io.xeres.common.dto.chess.ChessLeaderboardEntryDTO;
import io.xeres.common.id.GxsId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class ChessRatingService
{
	private static final Logger log = LoggerFactory.getLogger(ChessRatingService.class);

	public static final double DEFAULT_RATING = 1500.0;
	public static final double DEFAULT_RD = 350.0;
	public static final double DEFAULT_VOLATILITY = 0.06;
	private static final double TAU = 0.5;
	private static final double SCALE = 173.7178;
	private static final double EPSILON = 0.000001;

	private final ChessHistoryStore historyStore;
	private final IdentityService identityService;

	public ChessRatingService(ChessHistoryStore historyStore, IdentityService identityService)
	{
		this.historyStore = historyStore;
		this.identityService = identityService;
	}

	public static class PlayerStats
	{
		private final String peer;
		private String name;
		private double rating = DEFAULT_RATING;
		private double rd = DEFAULT_RD;
		private double vol = DEFAULT_VOLATILITY;
		private int wins;
		private int draws;
		private int losses;
		private String lastPlayed;

		public PlayerStats(String peer, String name)
		{
			this.peer = peer;
			this.name = name;
		}

		public String getPeer()
		{
			return peer;
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			if (name != null && !name.isBlank())
			{
				this.name = name;
			}
		}

		public int getRating()
		{
			return (int) Math.round(rating);
		}

		public int getRd()
		{
			return (int) Math.round(rd);
		}

		public int getWins()
		{
			return wins;
		}

		public int getDraws()
		{
			return draws;
		}

		public int getLosses()
		{
			return losses;
		}

		public int getGames()
		{
			return wins + draws + losses;
		}

		public String getStatus()
		{
			return (getGames() < 10 || rd > 110) ? "Provisional" : "Active";
		}

		public String getLastPlayed()
		{
			return lastPlayed;
		}
	}

	public synchronized List<ChessLeaderboardEntryDTO> getLeaderboard()
	{
		var players = computePlayerStats();
		var list = new ArrayList<>(players.values().stream()
				.filter(p -> p.getGames() > 0)
				.toList());

		list.sort((a, b) -> {
			int cmp = Integer.compare(b.getRating(), a.getRating());
			if (cmp != 0) return cmp;
			return Integer.compare(b.getGames(), a.getGames());
		});

		var result = new ArrayList<ChessLeaderboardEntryDTO>();
		int rank = 1;
		for (var player : list)
		{
			result.add(new ChessLeaderboardEntryDTO(
					rank++,
					player.getPeer(),
					player.getName(),
					player.getRating(),
					player.getRd(),
					player.getGames(),
					player.getWins(),
					player.getDraws(),
					player.getLosses(),
					player.getStatus(),
					player.getLastPlayed()
			));
		}
		return Collections.unmodifiableList(result);
	}

	public synchronized ChessLeaderboardEntryDTO getRating(String peer)
	{
		var players = computePlayerStats();
		var player = players.get(peer);
		if (player != null)
		{
			return new ChessLeaderboardEntryDTO(
					0,
					player.getPeer(),
					player.getName(),
					player.getRating(),
					player.getRd(),
					player.getGames(),
					player.getWins(),
					player.getDraws(),
					player.getLosses(),
					player.getStatus(),
					player.getLastPlayed()
			);
		}

		String name = peer;
		try
		{
			var id = GxsId.fromString(peer);
			if (!id.isNullIdentifier())
			{
				name = identityService.findByGxsId(id).map(i -> i.getName()).orElse(peer);
			}
		}
		catch (Exception ignored)
		{
		}

		return new ChessLeaderboardEntryDTO(
				0,
				peer,
				name,
				(int) Math.round(DEFAULT_RATING),
				(int) Math.round(DEFAULT_RD),
				0,
				0,
				0,
				0,
				"Provisional",
				null
		);
	}

	public synchronized Map<String, PlayerStats> computePlayerStats()
	{
		var statsMap = new HashMap<String, PlayerStats>();
		List<ChessHistorySummaryDTO> history;
		try
		{
			history = historyStore.list();
		}
		catch (IOException e)
		{
			log.warn("Failed to read chess history for ratings", e);
			history = List.of();
		}

		var chronological = new ArrayList<>(history);
		chronological.sort(Comparator.comparing(ChessHistorySummaryDTO::startedAt));

		for (var game : chronological)
		{
			var whiteId = game.whiteIdentity();
			var blackId = game.blackIdentity();
			if (whiteId == null || whiteId.isBlank() || blackId == null || blackId.isBlank())
			{
				continue;
			}

			var white = statsMap.computeIfAbsent(whiteId, id -> new PlayerStats(id, game.whiteName()));
			white.setName(game.whiteName());
			var black = statsMap.computeIfAbsent(blackId, id -> new PlayerStats(id, game.blackName()));
			black.setName(game.blackName());

			Double whiteScore = null;
			Double blackScore = null;

			var status = game.status();
			if ("DRAW".equals(status))
			{
				whiteScore = 0.5;
				blackScore = 0.5;
				white.draws++;
				black.draws++;
			}
			else if ("CHECKMATE".equals(status))
			{
				// Odd number of plies means White delivered checkmate; even means Black did.
				boolean whiteWon = (game.moves() % 2 != 0);
				whiteScore = whiteWon ? 1.0 : 0.0;
				blackScore = whiteWon ? 0.0 : 1.0;
				if (whiteWon)
				{
					white.wins++;
					black.losses++;
				}
				else
				{
					black.wins++;
					white.losses++;
				}
			}
			else if ("OPPONENT_RESIGNED".equals(status) || "RESIGNED".equals(status))
			{
				// If status is OPPONENT_RESIGNED from White's perspective or Black's perspective
				// In ChessHistorySummaryDTO, if white was local and opponent resigned, White won.
				// However, if we check moves count or resignation, we can award win to the player who didn't resign.
				boolean whiteWon = "OPPONENT_RESIGNED".equals(status);
				whiteScore = whiteWon ? 1.0 : 0.0;
				blackScore = whiteWon ? 0.0 : 1.0;
				if (whiteWon)
				{
					white.wins++;
					black.losses++;
				}
				else
				{
					black.wins++;
					white.losses++;
				}
			}

			if (whiteScore != null)
			{
				updateGlicko2(white, black, whiteScore, blackScore);
				white.lastPlayed = game.startedAt();
				black.lastPlayed = game.startedAt();
			}
		}

		return statsMap;
	}

	private void updateGlicko2(PlayerStats player1, PlayerStats player2, double score1, double score2)
	{
		double mu1 = (player1.rating - DEFAULT_RATING) / SCALE;
		double phi1 = player1.rd / SCALE;
		double sigma1 = player1.vol;

		double mu2 = (player2.rating - DEFAULT_RATING) / SCALE;
		double phi2 = player2.rd / SCALE;
		double sigma2 = player2.vol;

		// Update Player 1
		double newMu1 = computeNewMu(mu1, phi1, sigma1, mu2, phi2, score1);
		double newPhi1 = computeNewPhi(mu1, phi1, sigma1, mu2, phi2, score1);
		double newSigma1 = computeNewVolatility(mu1, phi1, sigma1, mu2, phi2, score1);

		// Update Player 2
		double newMu2 = computeNewMu(mu2, phi2, sigma2, mu1, phi1, score2);
		double newPhi2 = computeNewPhi(mu2, phi2, sigma2, mu1, phi1, score2);
		double newSigma2 = computeNewVolatility(mu2, phi2, sigma2, mu1, phi1, score2);

		player1.rating = Math.max(100.0, newMu1 * SCALE + DEFAULT_RATING);
		player1.rd = Math.max(30.0, Math.min(DEFAULT_RD, newPhi1 * SCALE));
		player1.vol = newSigma1;

		player2.rating = Math.max(100.0, newMu2 * SCALE + DEFAULT_RATING);
		player2.rd = Math.max(30.0, Math.min(DEFAULT_RD, newPhi2 * SCALE));
		player2.vol = newSigma2;
	}

	private double g(double phi)
	{
		return 1.0 / Math.sqrt(1.0 + 3.0 * phi * phi / (Math.PI * Math.PI));
	}

	private double e(double mu, double muJ, double phiJ)
	{
		return 1.0 / (1.0 + Math.exp(-g(phiJ) * (mu - muJ)));
	}

	private double computeVariance(double mu, double muJ, double phiJ)
	{
		double gVal = g(phiJ);
		double eVal = e(mu, muJ, phiJ);
		return 1.0 / (gVal * gVal * eVal * (1.0 - eVal));
	}

	private double computeDelta(double mu, double phi, double muJ, double phiJ, double score)
	{
		double v = computeVariance(mu, muJ, phiJ);
		double gVal = g(phiJ);
		double eVal = e(mu, muJ, phiJ);
		return v * gVal * (score - eVal);
	}

	private double computeNewVolatility(double mu, double phi, double sigma, double muJ, double phiJ, double score)
	{
		double v = computeVariance(mu, muJ, phiJ);
		double delta = computeDelta(mu, phi, muJ, phiJ, score);
		double a = Math.log(sigma * sigma);

		double A = a;
		double B;
		if (delta * delta > phi * phi + v)
		{
			B = Math.log(delta * delta - phi * phi - v);
		}
		else
		{
			int k = 1;
			while (f(a - k * TAU, delta, phi, v, a) < 0)
			{
				k++;
			}
			B = a - k * TAU;
		}

		double fA = f(A, delta, phi, v, a);
		double fB = f(B, delta, phi, v, a);

		while (Math.abs(B - A) > EPSILON)
		{
			double C = A + (A - B) * fA / (fB - fA);
			double fC = f(C, delta, phi, v, a);
			if (fC * fB <= 0)
			{
				A = B;
				fA = fB;
			}
			else
			{
				fA = fA / 2.0;
			}
			B = C;
			fB = fC;
		}

		return Math.exp(A / 2.0);
	}

	private double f(double x, double delta, double phi, double v, double a)
	{
		double expX = Math.exp(x);
		double num = expX * (delta * delta - phi * phi - v - expX);
		double denom = 2.0 * Math.pow(phi * phi + v + expX, 2);
		return (num / denom) - ((x - a) / (TAU * TAU));
	}

	private double computeNewPhi(double mu, double phi, double sigma, double muJ, double phiJ, double score)
	{
		double newSigma = computeNewVolatility(mu, phi, sigma, muJ, phiJ, score);
		double phiStar = Math.sqrt(phi * phi + newSigma * newSigma);
		double v = computeVariance(mu, muJ, phiJ);
		return 1.0 / Math.sqrt(1.0 / (phiStar * phiStar) + 1.0 / v);
	}

	private double computeNewMu(double mu, double phi, double sigma, double muJ, double phiJ, double score)
	{
		double newPhi = computeNewPhi(mu, phi, sigma, muJ, phiJ, score);
		double gVal = g(phiJ);
		double eVal = e(mu, muJ, phiJ);
		return mu + newPhi * newPhi * gVal * (score - eVal);
	}
}
