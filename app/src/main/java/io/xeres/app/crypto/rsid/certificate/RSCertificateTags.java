/*
 * Copyright (c) 2019-2020 by David Gerber - https://zapek.com
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

package io.xeres.app.crypto.rsid.certificate;

public final class RSCertificateTags
{
	public static final int PGP = 1;
	public static final int EXTERNAL_IP_AND_PORT = 2;
	public static final int INTERNAL_IP_AND_PORT = 3;
	public static final int DNS = 4;
	public static final int SSLID = 5;
	public static final int NAME = 6;
	public static final int CHECKSUM = 7;
	public static final int HIDDEN_NODE = 8;
	public static final int VERSION = 9;
	public static final int EXTRA_LOCATOR = 10;

	private RSCertificateTags()
	{
		throw new UnsupportedOperationException("Utility class");
	}
}
