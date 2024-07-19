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

package io.xeres.common.util;

import java.text.DecimalFormat;

/**
 * In the beginning God created the computer. And the computer was without form, and void;
 * and darkness was upon the face of the silicon. And the Spirit of God moved upon the face of
 * the wafers. And God said, let there be bytes: and there were bytes. And God saw the bytes,
 * that they were good: and God divided the bytes by 1024.
 */
public final class ByteUnitUtils
{
	private static final DecimalFormat df = new DecimalFormat("#.##");

	private ByteUnitUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Returns the number of bytes in their proper unit, with up to 2 decimals, except for KBs.
	 *
	 * @param bytes the number of bytes
	 * @return the bytes in their proper unit
	 */
	public static String fromBytes(long bytes)
	{
		if (bytes < 0)
		{
			return "invalid";
		}
		if (bytes < 1024)
		{
			return bytes + " bytes";
		}
		else if (bytes < 1024 * 1024)
		{
			return df.format(bytes / 1024) + " KB";
		}
		else if (bytes < 1024 * 1024 * 1024)
		{
			return df.format(bytes / 1024.0 / 1024.0) + " MB";
		}
		else if (bytes < 1024L * 1024 * 1024 * 1024)
		{
			return df.format(bytes / 1024.0 / 1024.0 / 1024.0) + " GB";
		}
		else if (bytes < 1024L * 1024 * 1024 * 1024 * 1024)
		{
			return df.format(bytes / 1024.0 / 1024.0 / 1024.0 / 1024.0) + " TB";
		}
		else if (bytes < 1024L * 1024 * 1024 * 1024 * 1024 * 1024)
		{
			return df.format(bytes / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0) + " PB";
		}
		return "???";
	}
}
