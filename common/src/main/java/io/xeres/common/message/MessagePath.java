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

package io.xeres.common.message;

public final class MessagePath
{
	public static final String BROKER_PREFIX = "/topic";
	public static final String DIRECT_PREFIX = "/queue";
	public static final String APP_PREFIX = "/app";

	public static final String CHAT_ROOT = "/chat";
	public static final String CHAT_PRIVATE_DESTINATION = "/private";
	public static final String CHAT_ROOM_DESTINATION = "/room";
	public static final String CHAT_BROADCAST_DESTINATION = "/broadcast";
	public static final String CHAT_DISTANT_DESTINATION = "/distant";

	public static final String VOIP_ROOT = "/voip";
	public static final String VOIP_PRIVATE_DESTINATION = "/private";

	private MessagePath()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static String chatPrivateDestination()
	{
		return BROKER_PREFIX + CHAT_ROOT + CHAT_PRIVATE_DESTINATION;
	}

	public static String chatRoomDestination()
	{
		return BROKER_PREFIX + CHAT_ROOT + CHAT_ROOM_DESTINATION;
	}

	public static String chatBroadcastDestination()
	{
		return BROKER_PREFIX + CHAT_ROOT + CHAT_BROADCAST_DESTINATION;
	}

	public static String chatDistantDestination()
	{
		return BROKER_PREFIX + CHAT_ROOT + CHAT_DISTANT_DESTINATION;
	}

	public static String voipPrivateDestination()
	{
		return BROKER_PREFIX + VOIP_ROOT + VOIP_PRIVATE_DESTINATION;
	}
}
