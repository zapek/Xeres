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

import java.time.Instant;
import java.util.regex.Pattern;

public class IdentityServiceStorage
{
	private static final Pattern SERVICE_STRING = Pattern.compile("^v2 \\{P:(.{1,1024}?)}\\{T:(.{1,1024}?)}\\{R:(.{1,1024}?)}$");

	private final Pgp pgp = new Pgp();
	private final Recognition recognition = new Recognition();
	private final Reputation reputation = new Reputation();

	private boolean success;

	public IdentityServiceStorage(long pgpIdentifier)
	{
		pgp.setPgpIdentifier(pgpIdentifier);
	}

	public IdentityServiceStorage(String storage)
	{
		success = in(storage);
	}

	private boolean in(String storage)
	{
		var matcher = SERVICE_STRING.matcher(storage);

		if (!matcher.matches())
		{
			return false;
		}

		pgp.load(matcher.group(1));
		if (!pgp.isSuccessful())
		{
			return false;
		}

		recognition.load(matcher.group(2));
		if (!recognition.isSuccessful())
		{
			return false;
		}

		reputation.load(matcher.group(3));
		if (!reputation.isSuccessful())
		{
			return false;
		}
		return true;
	}

	public String out()
	{
		return String.format("v2 {P:%s}{T:%s}{R:%s}", pgp.out(), recognition.out(), reputation.out());
	}

	public boolean isSuccessful()
	{
		return success;
	}

	public long getPgpIdentifier()
	{
		return pgp.getPgpIdentifier();
	}

	public void setPgpIdentifier(long pgpIdentifier)
	{
		pgp.setPgpIdentifier(pgpIdentifier);
	}

	public void updateIdScore(boolean pgpLinked, boolean pgpKnown)
	{
		reputation.updateIdScore(pgpLinked, pgpKnown);
	}

	public Instant computeNextValidationAttempt()
	{
		return pgp.computeNextValidationAttempt();
	}
}
