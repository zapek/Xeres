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

package io.xeres.app.crypto.chatcipher;

import io.xeres.common.id.Identifier;

/**
 * Utility class to handle challenge codes, which allows peers to know if they
 * have a common private chat room without disclosing it first.
 */
public final class ChatChallenge
{
	private ChatChallenge()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static long code(Identifier identifier, long chatRoomId, long messageId)
	{
		long code = 0;

		var id = identifier.getBytes();

		for (var i = 0; i < identifier.getLength(); i++)
		{
			code += messageId;
			code ^= code >>> 35;
			code += code << 6;
			code ^= Byte.toUnsignedLong(id[i]) * chatRoomId;
			code += code << 26;
			code ^= code >>> 13;
		}
		return code;
	}
}
