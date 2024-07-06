/*
 * Copyright (c) 2023 by David Gerber - https://zapek.com
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

import static io.xeres.common.properties.StartupProperties.Property.*;

public final class LocalPortFinder
{
	private static final Logger log = LoggerFactory.getLogger(LocalPortFinder.class);

	private static final int DEFAULT_PORT = 1066;
	private static final int MAX_INSTANCES = 1024;

	private LocalPortFinder()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static void ensureFreePort()
	{
		var uiAddress = StartupProperties.getBoolean(UI_ADDRESS);
		if (uiAddress != null)
		{
			return; // Don't bother with free port selection if we only want to connect to a remote client
		}
		var port = StartupProperties.getInteger(CONTROL_PORT);
		if (port == null)
		{
			port = DEFAULT_PORT;
		}
		var portMax = Math.min(65536, port + MAX_INSTANCES);
		var portFound = -1;

		for (int i = port; i < portMax; i++)
		{
			try (var ignored = new ServerSocket(i))
			{
				portFound = i;
				break;
			}
			catch (IOException ignored)
			{
				// Port already in use
			}
		}

		if (portFound == -1)
		{
			throw new IllegalStateException("No local port available, tried range: " + port + "-" + portMax);
		}
		else
		{
			if (port != portFound)
			{
				log.info("Local port {} already used, using {} instead", port, portFound);
			}
			// Make sure the properties are always set
			StartupProperties.setPort(CONTROL_PORT, String.valueOf(portFound));
			StartupProperties.setPort(UI_PORT, String.valueOf(portFound));
		}
	}
}
