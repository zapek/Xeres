/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.common;

import io.xeres.common.id.GxsId;

import java.util.HashMap;
import java.util.Map;

import static io.xeres.app.xrs.common.SecurityKey.Flags.TYPE_FULL;
import static io.xeres.app.xrs.common.SecurityKey.Flags.TYPE_PUBLIC_ONLY;

public class SecurityKeySet
{
	private final String groupId = ""; // XXX: seems unused, confirm
	private final Map<GxsId, SecurityKey> privateKeys = new HashMap<>();
	private final Map<GxsId, SecurityKey> publicKeys = new HashMap<>();

	public SecurityKeySet()
	{
		// Needed
	}

	public String getGroupId()
	{
		return groupId;
	}

	public void put(SecurityKey securityKey)
	{
		if (securityKey.getFlags().contains(TYPE_PUBLIC_ONLY))
		{
			publicKeys.put(securityKey.getGxsId(), securityKey);
		}
		else if (securityKey.getFlags().contains(TYPE_FULL))
		{
			privateKeys.put(securityKey.getGxsId(), securityKey);
		}
	}

	public Map<GxsId, SecurityKey> getPrivateKeys()
	{
		return privateKeys;
	}

	public Map<GxsId, SecurityKey> getPublicKeys()
	{
		return publicKeys;
	}
}
