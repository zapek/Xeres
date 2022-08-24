/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.app.job;

import io.xeres.app.XeresApplication;
import io.xeres.app.service.PeerService;
import io.xeres.common.properties.StartupProperties;

import static io.xeres.common.properties.StartupProperties.Property.SERVER_ONLY;

public final class JobUtils
{
	private JobUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	@SuppressWarnings("RedundantIfStatement")
	static boolean canRun(PeerService peerService)
	{
		// Do not execute if we're in server mode (i.e. only accepting connections)
		if (StartupProperties.getBoolean(SERVER_ONLY, false))
		{
			return false;
		}

		// Do not execute if we're only a remote UI client
		if (XeresApplication.isRemoteUiClient())
		{
			return false;
		}

		// Do not execute if there's no network or if we're shutting down
		if (!peerService.isRunning())
		{
			return false;
		}

		return true;
	}
}
