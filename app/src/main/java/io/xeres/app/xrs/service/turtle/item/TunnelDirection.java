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

package io.xeres.app.xrs.service.turtle.item;

/**
 * The direction of the tunnel. Either {@link #CLIENT} or {@link #SERVER}.
 * If for example a packet has "client" set, then it means whoever sent it is a client.
 */
public enum TunnelDirection
{
	/**
	 * A client, For example when downloading a file from a remote node or when we started a distant chat.
	 */
	CLIENT,

	/**
	 * A server, for example when serving a file to a remote node or receiving a distant chat.
	 */
	SERVER
}
