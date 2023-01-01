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

package io.xeres.common.protocol;

import java.util.Locale;

/**
 * The NetMode<br>
 * Note: this is stored in the database in ordinal. Do not modify the order.
 */
public enum NetMode
{
	UNKNOWN, // Unknown netmode
	UDP, // firewalled | UDP mode
	UPNP, // automatic (UPNP) | Ext (UPNP)
	EXT, // manually forwarded port | External port
	HIDDEN, // hidden mode | Hidden
	UNREACHABLE; // UDP mode (unreachable)

	@Override
	public String toString()
	{
		return super.toString().toLowerCase(Locale.ROOT);
	}
}
