/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service.chat;

import io.xeres.app.xrs.RsDeprecated;

public enum ChatFlags
{
	/**
	 * Set for all direct, distant and chat room messages.
	 */
	PRIVATE,

	/**
	 * Set when requesting an avatar. The message must be empty
	 * and set to private too. Xeres uses it to get the remote icon
	 * when opening a private/distant chat window.
	 */
	REQUEST_AVATAR,

	/**
	 * No longer used.
	 */
	@RsDeprecated
	CONTAINS_AVATAR,

	/**
	 * Set if we changed our avatar (not used by Xeres).
	 */
	AVATAR_AVAILABLE,

	/**
	 * Used to send status strings in a ChatStatusItem (currently not used by Xeres).
	 */
	CUSTOM_STATE,

	/**
	 * Set for broadcast messages.
	 */
	PUBLIC,

	/**
	 * Used to request a custom string in a ChatStatusItem (currently not used by Xeres).
	 */
	REQUEST_CUSTOM_STATE,

	/**
	 * Used to tell we have or changed a status string in a ChatStatusItem
	 * (currently not used by Xeres).
	 */
	CUSTOM_STATE_AVAILABLE,

	/**
	 * Used to tell that this is a large message that is split and needs
	 * to be reassembled.
	 */
	PARTIAL_MESSAGE,

	/**
	 * Always set for ChatRoomMessageItem.
	 */
	LOBBY,

	/**
	 * No longer used. Uses Gxs Tunnels instead.
	 */
	@RsDeprecated
	CLOSING_DISTANT_CONNECTION,

	/**
	 * No longer used. Uses turtle instead.
	 */
	@RsDeprecated
	ACK_DISTANT_CONNECTION,

	/**
	 * No longer used.
	 */
	@RsDeprecated
	KEEP_ALIVE,

	/**
	 * Set for distant chats to refuse a connection. Currently not used by Xeres.
	 */
	CONNECTION_REFUSED
}
