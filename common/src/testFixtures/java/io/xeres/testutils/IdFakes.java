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

package io.xeres.testutils;

import io.xeres.common.id.GxsId;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.id.MessageId;

public final class IdFakes
{
	private IdFakes()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static GxsId createGxsId()
	{
		return new GxsId(RandomUtils.nextBytes(GxsId.LENGTH));
	}

	public static GxsId createGxsId(byte[] gxsId)
	{
		return new GxsId(gxsId);
	}

	public static MessageId createMessageId()
	{
		return new MessageId(RandomUtils.nextBytes(MessageId.LENGTH));
	}

	public static LocationIdentifier createLocationIdentifier()
	{
		return new LocationIdentifier(RandomUtils.nextBytes(LocationIdentifier.LENGTH));
	}

	public static long createLong()
	{
		return RandomUtils.nextLong(1, Long.MAX_VALUE);
	}

	public static int createInt()
	{
		return RandomUtils.nextInt(1, Integer.MAX_VALUE);
	}
}
