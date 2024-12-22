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

package io.xeres.app.application.environment;

import io.xeres.common.properties.StartupProperties;
import io.xeres.common.properties.StartupProperties.Property;

import java.util.Optional;

import static io.xeres.common.properties.StartupProperties.Property.*;

/**
 * This utility class allows setting properties using the content of env variables.
 * This is especially useful when run from containers.
 */
public final class HostVariable
{
	/**
	 * The location of the data directory. Either an absolute or a relative path.
	 */
	private static final String XERES_DATA_DIR = "XERES_DATA_DIR";

	/**
	 * The control port of the server (that is, where the UI client is sending commands to).
	 */
	private static final String XERES_CONTROL_PORT = "XERES_CONTROL_PORT";

	/**
	 * The interface address to bind to (default: all).
	 */
	private static final String XERES_SERVER_ADDRESS = "XERES_SERVER_ADDRESS";

	/**
	 * The incoming port for peer connections.
	 */
	private static final String XERES_SERVER_PORT = "XERES_SERVER_PORT";

	/**
	 * If we are running in server mode only (that is, we're only accepting incoming connections).
	 * Ideal for a chat server.
	 */
	private static final String XERES_SERVER_ONLY = "XERES_SERVER_ONLY";

	private static final String ENVIRONMENT_VARIABLE_STRING = "Environment variable";

	private HostVariable()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Sets properties using env variables.
	 */
	public static void parse()
	{
		get(XERES_DATA_DIR).ifPresent(s -> setString(XERES_DATA_DIR, DATA_DIR, s));
		get(XERES_SERVER_ONLY).ifPresent(s -> setBoolean(XERES_SERVER_ONLY, SERVER_ONLY, s));
		get(XERES_CONTROL_PORT).ifPresent(s -> {
			setPort(XERES_CONTROL_PORT, CONTROL_PORT, s);
			setPort(XERES_CONTROL_PORT, UI_PORT, s);
		});
		get(XERES_SERVER_ADDRESS).ifPresent(s -> setString(XERES_SERVER_ADDRESS, SERVER_ADDRESS, s));
		get(XERES_SERVER_PORT).ifPresent(s -> setPort(XERES_SERVER_PORT, SERVER_PORT, s));
	}

	private static Optional<String> get(String key)
	{
		return Optional.ofNullable(System.getenv(key));
	}

	private static void setString(String name, Property property, String value)
	{
		try
		{
			StartupProperties.setString(property, value, StartupProperties.Origin.ENVIRONMENT_VARIABLE);
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException(ENVIRONMENT_VARIABLE_STRING + " " + name + " does not contain a value");
		}
	}

	private static void setBoolean(String name, Property property, String value)
	{
		try
		{
			StartupProperties.setBoolean(property, value, StartupProperties.Origin.ENVIRONMENT_VARIABLE);
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException(ENVIRONMENT_VARIABLE_STRING + " " + name + " does not contain a boolean value (" + value + ")");
		}
	}

	private static void setPort(String name, Property property, String value)
	{
		try
		{
			StartupProperties.setPort(property, value, StartupProperties.Origin.ENVIRONMENT_VARIABLE);
		}
		catch (IllegalArgumentException e)
		{
			throw new IllegalArgumentException(ENVIRONMENT_VARIABLE_STRING + " " + name + " does not contain a valid port bigger than 0 and smaller than 65536 (" + value + ")");
		}
	}
}
