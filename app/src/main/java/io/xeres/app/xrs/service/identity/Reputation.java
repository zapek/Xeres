/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.identity;

import java.util.regex.Pattern;

class Reputation
{
	private static final Pattern REPUTATION_PATTERN = Pattern.compile("^(-?\\d{1,10}) (-?\\d{1,10}) (-?\\d{1,10}) (-?\\d{1,10})$");

	private static final int PGP_KNOWN_SCORE = 50;
	private static final int PGP_UNKNOWN_SCORE = 20;
	private static final int ANON_SCORE = 5;

	private int overallScore;
	private int idScore;
	private int ownOpinion;
	private int peerOpinion;

	private boolean success;

	public Reputation()
	{
		overallScore = 5;
		idScore = 5;
		ownOpinion = 0;
		peerOpinion = 0;
	}

	public boolean load(String input)
	{
		success = in(input);
		return success;
	}

	private boolean in(String input)
	{
		var matcher = REPUTATION_PATTERN.matcher(input);
		if (matcher.matches())
		{
			overallScore = Integer.parseInt(matcher.group(1));
			idScore = Integer.parseInt(matcher.group(2));
			ownOpinion = Integer.parseInt(matcher.group(3));
			peerOpinion = Integer.parseInt(matcher.group(4));
			return true;
		}
		return false;
	}

	public String out()
	{
		return String.format("%d %d %d %d", overallScore, idScore, ownOpinion, peerOpinion);
	}

	public boolean isSuccessful()
	{
		return success;
	}

	public void updateIdScore(boolean pgpLinked, boolean pgpKnown)
	{
		if (pgpLinked)
		{
			if (pgpKnown)
			{
				idScore = PGP_KNOWN_SCORE;
			}
			else
			{
				idScore = PGP_UNKNOWN_SCORE;
			}
		}
		else
		{
			idScore = ANON_SCORE;
		}
		overallScore = idScore + ownOpinion + peerOpinion;
	}
}