/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

import io.xeres.common.util.ScrambledString;

public final class Headless
{
	private Headless()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static ScrambledString getHeadlessPassword()
	{
		var envPassword = System.getenv("XERES_PROFILE_PASSWORD");
		if (envPassword != null)
		{
			return new ScrambledString(envPassword);
		}

		var console = System.console();
		if (console != null)
		{
			var passphrase = console.readPassword("Password: ");
			if (passphrase != null)
			{
				try
				{
					return new ScrambledString(passphrase);
				}
				finally
				{
					ScrambledString.clear(passphrase);
				}
			}
		}
		else
		{
			CommandArgument.portableOutput("No console available to get the password");
		}
		return null;
	}
}
