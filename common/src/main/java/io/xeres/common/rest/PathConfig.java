/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
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

package io.xeres.common.rest;

public final class PathConfig
{
	private PathConfig()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static final String CONFIG_PATH = "/api/v1/config";
	public static final String PROFILES_PATH = "/api/v1/profiles";
	public static final String LOCATIONS_PATH = "/api/v1/locations";
	public static final String CONNECTIONS_PATH = "/api/v1/connections";
	public static final String NOTIFICATIONS_PATH = "/api/v1/notifications";
	public static final String CHAT_PATH = "/api/v1/chat";
	public static final String IDENTITIES_PATH = "/api/v1/identities";
	public static final String SETTINGS_PATH = "/api/v1/settings";
	public static final String GEOIP_PATH = "/api/v1/geoip";
	public static final String FORUMS_PATH = "/api/v1/forums";
	public static final String SHARES_PATH = "/api/v1/shares";
	public static final String FILES_PATH = "/api/v1/files";
	public static final String STATISTICS_PATH = "/api/v1/statistics";
}
