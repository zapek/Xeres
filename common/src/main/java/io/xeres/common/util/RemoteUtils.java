/*
 * Copyright (c) 2024-2025 by David Gerber - https://zapek.com
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

package io.xeres.common.util;

/**
 * Some utility class to get remote information for the client.
 */
public final class RemoteUtils
{
	private RemoteUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static String getHostnameAndPort()
	{
		return getHostname() + ":" + getControlPort();
	}

	private static String getHostname()
	{
		return System.getProperty("xrs.ui.address", "127.0.0.1");
	}

	private static int getControlPort()
	{
		return Integer.parseInt(System.getProperty("xrs.ui.port", "6232"));
	}

	public static String getControlUrl()
	{
		if ("true".equals(System.getProperty("server.ssl.enabled")))
		{
			return "https://" + getHostnameAndPort();
		}
		//noinspection HttpUrlsUsage
		return "http://" + getHostnameAndPort();
	}

	public static boolean isRemoteUiClient()
	{
		return "none".equals(System.getProperty("spring.main.web-application-type"));
	}
}
