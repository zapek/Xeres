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

package io.xeres.app.xrs.service.mail;

import org.junit.jupiter.api.Test;

import static io.xeres.app.xrs.service.mail.MailFlags.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MailFlagsTest
{
	@Test
	void Enum_Order_Fixed()
	{
		assertEquals(0x1, toIntFlag(OUTGOING.ordinal()));
		assertEquals(0x2, toIntFlag(PENDING.ordinal()));
		assertEquals(0x4, toIntFlag(DRAFT.ordinal()));
		assertEquals(0x10, toIntFlag(NEW.ordinal()));
		assertEquals(0x20, toIntFlag(TRASH.ordinal()));
		assertEquals(0x40, toIntFlag(UNREAD_BY_USER.ordinal()));
		assertEquals(0x80, toIntFlag(REPLIED.ordinal()));
		assertEquals(0x100, toIntFlag(FORWARDED.ordinal()));
		assertEquals(0x200, toIntFlag(STAR.ordinal()));
		assertEquals(0x400, toIntFlag(PARTIAL.ordinal()));
		assertEquals(0x800, toIntFlag(USER_REQUEST.ordinal()));
		assertEquals(0x1000, toIntFlag(FRIEND_RECOMMENDATION.ordinal()));
		assertEquals(0x2000, toIntFlag(RETURN_RECEIPT.ordinal()));
		assertEquals(0x4000, toIntFlag(ENCRYPTED.ordinal()));
		assertEquals(0x8000, toIntFlag(DISTANT.ordinal()));
		assertEquals(0x10000, toIntFlag(SIGNATURE_CHECKS.ordinal()));
		assertEquals(0x20000, toIntFlag(SIGNED.ordinal()));
		assertEquals(0x40000, toIntFlag(LOAD_EMBEDDED_IMAGES.ordinal()));
		assertEquals(0x80000, toIntFlag(DECRYPTED.ordinal()));
		assertEquals(0x100000, toIntFlag(ROUTED.ordinal()));
		assertEquals(0x200000, toIntFlag(PUBLISH_KEY.ordinal()));
		assertEquals(0x400000, toIntFlag(SPAM.ordinal()));
	}

	private static int toIntFlag(int value)
	{
		return 1 << value;
	}
}