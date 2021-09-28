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

package io.xeres.app.crypto.rsid.shortinvite;

public final class ShortInviteTags
{
	public static final int SSLID = 0x0;
	public static final int NAME = 0x1;
	public static final int LOCATOR = 0x2;
	public static final int PGP_FINGERPRINT = 0x3;
	public static final int CHECKSUM = 0x4;
	public static final int HIDDEN_LOCATOR = 0x90;
	public static final int DNS_LOCATOR = 0x91;
	public static final int EXT4_LOCATOR = 0x92;
	public static final int LOC4_LOCATOR = 0x93;

	private ShortInviteTags()
	{
		throw new UnsupportedOperationException("Utility class");
	}
}
