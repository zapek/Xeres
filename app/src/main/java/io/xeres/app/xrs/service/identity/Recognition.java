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

class Recognition
{
	private static final Pattern RECOGNITION_PATTERN = Pattern.compile("^F:(\\d{1,10}) P:(\\d{1,10}) T:(\\d{1,10})$");

	private int flags;
	private Instant publish;
	private Instant lastCheck;

	private boolean success;

	public Recognition()
	{
		flags = 0;
		publish = Instant.EPOCH;
		lastCheck = Instant.EPOCH;
	}

	public boolean load(String input)
	{
		success = in(input);
		return success;
	}

	private boolean in(String input)
	{
		var matcher = RECOGNITION_PATTERN.matcher(input);
		if (matcher.matches())
		{
			flags = Integer.parseUnsignedInt(matcher.group(1));
			publish = Instant.ofEpochSecond(Long.parseLong(matcher.group(2)));
			lastCheck = Instant.ofEpochSecond(Long.parseLong(matcher.group(3)));
			return true;
		}
		return false;
	}

	public String out()
	{
		return String.format("F:%d P:%d T:%d", flags, publish.getEpochSecond(), lastCheck.getEpochSecond());
	}

	public boolean isSuccessful()
	{
		return success;
	}
}
