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

import io.xeres.common.id.Id;

import java.time.Instant;
import java.util.regex.Pattern;

class Pgp
{
	private static final Pattern VALIDATED = Pattern.compile("^K:1 I:(\\p{XDigit}{16})$");
	private static final Pattern UNVALIDATED_WITH_TIMESTAMP_ATTEMPTS_AND_ID = Pattern.compile("^K:0 T:(\\d{1,10}) C:(\\d{1,10}) I:(\\p{XDigit}{16})$");
	private static final Pattern UNVALIDATED_WITH_TIMESTAMP_AND_ATTEMPTS = Pattern.compile("^K:0 T:(\\d{1,10}) C:(\\d{1,10})$");
	private static final Pattern UNVALIDATED_WITH_TIMESTAMP = Pattern.compile("^K:0 T:(\\d{1,10})$");

	private boolean validated;
	private Instant lastCheck;
	private int checkAttempt;
	private long pgpIdentifier;

	private boolean success;

	public Pgp(long pgpIdentifier)
	{
		this.pgpIdentifier = pgpIdentifier;
		validated = true;
	}

	public Pgp(String input)
	{
		success = in(input);
	}

	private boolean in(String input)
	{
		var matcher = VALIDATED.matcher(input);
		if (matcher.matches())
		{
			validated = true;
			pgpIdentifier = Long.parseUnsignedLong(matcher.group(1), 16);
			return true;
		}
		matcher = UNVALIDATED_WITH_TIMESTAMP_ATTEMPTS_AND_ID.matcher(input);
		if (matcher.matches())
		{
			lastCheck = Instant.ofEpochSecond(Long.parseLong(matcher.group(1)));
			checkAttempt = Integer.parseUnsignedInt(matcher.group(2));
			pgpIdentifier = Long.parseUnsignedLong(matcher.group(3), 16);
			return true;
		}
		matcher = UNVALIDATED_WITH_TIMESTAMP_AND_ATTEMPTS.matcher(input);
		if (matcher.matches())
		{
			lastCheck = Instant.ofEpochSecond(Long.parseLong(matcher.group(1)));
			checkAttempt = Integer.parseUnsignedInt(matcher.group(2));
			return true;
		}
		matcher = UNVALIDATED_WITH_TIMESTAMP.matcher(input);
		if (matcher.matches())
		{
			lastCheck = Instant.ofEpochSecond(Long.parseLong(matcher.group(1)));
			return true;
		}
		return false;
	}

	public String out()
	{
		if (validated)
		{
			return "K:1 I:" + Id.toString(pgpIdentifier);
		}
		else
		{
			var sb = new StringBuilder("K:0");
			sb.append(" T:");
			sb.append(lastCheck.getEpochSecond());
			sb.append(" C:");
			sb.append(checkAttempt);
			if (pgpIdentifier != 0)
			{
				sb.append(" I:");
				sb.append(Id.toString(pgpIdentifier));
			}
			return sb.toString();
		}
	}

	public boolean isSuccessful()
	{
		return success;
	}
}
