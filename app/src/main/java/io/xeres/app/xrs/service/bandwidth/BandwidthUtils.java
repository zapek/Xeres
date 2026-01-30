/*
 * Copyright (c) 2025-2026 by David Gerber - https://zapek.com
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
	private BandwidthUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/// Tries to find the maximum bandwidth of a host.
	///
	/// Note: this doesn't take into account any possible router on the LAN.
	///
	/// @return the maximum bandwidth in bps, or 0 if not found or not available
	public static long findBandwidth()
	{
		if (SystemUtils.IS_OS_WINDOWS)
		{
			var result = OsUtils.shellExecute("powershell.exe", "-Command", "(Get-Counter '\\Network Interface(*)\\Current Bandwidth').CounterSamples.CookedValue");
			return searchBandwidthOnWindows(result);
		}
		else if (SystemUtils.IS_OS_LINUX)
		{
			// Get default interface
			var iface = OsUtils.shellExecute("sh", "-c", "ip route show default | awk '/default/ {print $5}'").trim();
			if (!iface.isEmpty())
			{
				var result = OsUtils.shellExecute("cat", "/sys/class/net/" + iface + "/speed");
				return searchBandwidthOnLinux(result);
			}
		}
		else if (SystemUtils.IS_OS_MAC)
		{
			// Use en0 as default, or detect default interface
			var result = OsUtils.shellExecute("ifconfig", "en0");
			return searchBandwidthOnMac(result);
		}
		return 0L;
	}

	static long searchBandwidthOnWindows(String input)
	{
		if (!StringUtils.isBlank(input))
		{
			return input.lines()
					.map(Long::parseLong)
					.max(Comparator.naturalOrder())
					.orElse(0L);
		}
		return 0L;
	}

	static long searchBandwidthOnLinux(String input)
	{
		if (!StringUtils.isBlank(input) && input.matches("\\d+"))
		{
			return Long.parseLong(input.trim()) * 1_000_000L; // Convert Mbps to bps
		}
		return 0L;
	}

	static long searchBandwidthOnMac(String input)
	{
		if (!StringUtils.isBlank(input))
		{
			// Find "media:" line and extract speed
			var mediaLine = input.lines()
					.filter(line -> line.contains("media:"))
					.findFirst();
			if (mediaLine.isPresent())
			{
				var matcher = Pattern.compile("(\\d+)baseT").matcher(mediaLine.get());
				if (matcher.find())
				{
					return Long.parseLong(matcher.group(1)) * 1_000_000L; // Convert Mbps to bps
				}
			}
		}
		return 0L;
	}
}
