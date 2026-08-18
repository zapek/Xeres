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

package io.xeres.common.message.chat;

import java.time.Duration;

public final class ChatConstants
{
	public static final Duration TYPING_NOTIFICATION_DELAY = Duration.ofSeconds(5);

	/// The maximum size of a message in total for private and distant messages.
	///
	/// Retroshare itself has no limit to them (which is dumb). We don't accept messages
	/// bigger than that, though.
	public static final int MESSAGE_TOTAL_SIZE_MAX = 400_000;

	/// When a message is bigger than [MESSAGE_TOTAL_SIZE_MAX], it is split
	/// using that value. Retroshare itself splits to 15000 bytes (which is dumb, too),
	/// instead we split to slightly smaller than the buffer allocated by its PQI Streamer (262_143 bytes),
	/// which is the maximum packet size that it can send or receive.
	public static final int MESSAGE_SPLIT_SLICE_SIZE_MAX = 260_000;

	/// Hard limit for chat rooms only. No messages can be bigger than that
	/// and partial messages aren't supported.
	public static final int CHAT_ROOM_MESSAGE_MAXIMUM_SIZE = 31_000;

	private ChatConstants()
	{
		throw new UnsupportedOperationException("Utility class");
	}
}
