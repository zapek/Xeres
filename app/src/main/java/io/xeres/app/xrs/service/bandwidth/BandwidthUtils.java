/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.bandwidth;

import io.xeres.common.util.OsUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.util.Comparator;
import java.util.regex.Pattern;

final class BandwidthUtils
{
	private static final Pattern RATE = Pattern.compile("\"(\\d{1,15}\\.\\d)\\d{5}\"");

	private BandwidthUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Tries to find the maximum bandwidth of a host.
	 * <p>
	 * Note: this doesn't take into account any possible router on the LAN. It also doesn't take into account
	 * which interface is the default one.
	 *
	 * @return the maximum bandwidth or 0 if not found or not available
	 */
	public static long findBandwidth()
	{
		if (SystemUtils.IS_OS_WINDOWS)
		{
			var result = OsUtils.shellExecute("typeperf", "-sc", "1", "\"Network Interface(*)\\Current Bandwidth\"");
			return searchBandwidth(result);
		}
		// XXX: add Linux and MacOS
		return 0L;
	}

	static long searchBandwidth(String input)
	{
		if (!StringUtils.isBlank(input))
		{
			return Math.round(input.lines()
					.filter(line -> line.startsWith("\"") && line.endsWith("\""))
					.flatMap(s -> RATE.matcher(s).results())
					.map(matchResult -> matchResult.group(1))
					.map(Double::parseDouble)
					.max(Comparator.naturalOrder())
					.orElse(0.0));
		}
		return 0L;
	}
}
