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

package io.xeres.common.util;

import io.xeres.common.i18n.I18nUtils;

import java.text.DecimalFormat;
import java.util.ResourceBundle;

/**
 * In the beginning God created the computer. And the computer was without form, and void;
 * and darkness was upon the face of the silicon. And the Spirit of God moved upon the face of
 * the wafers. And God said, let there be bytes: and there were bytes. And God saw the bytes,
 * that they were good: and God divided the bytes by 1024.
 */
public final class ByteUnitUtils
{
	private static final DecimalFormat df = new DecimalFormat("#.##");

	private static final ResourceBundle bundle = I18nUtils.getBundle();

	private ByteUnitUtils()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Returns the number of bytes in their proper unit, from bytes to exabytes, with up to 2 decimals, except for KBs.
	 *
	 * @param bytes the number of bytes, must be a positive number
	 * @return the bytes in their proper unit or "invalid" if a negative number was given as input
	 */
	public static String fromBytes(long bytes)
	{
		if (bytes < 0)
		{
			return bundle.getString("byte-unit.invalid");
		}
		if (bytes < 1024 * 10)
		{
			return bytes + " " + bundle.getString("byte-unit.bytes");
		}
		else if (bytes < 1024 * 1024)
		{
			return df.format(bytes / 1024) + " " + bundle.getString("byte-unit.kb");
		}
		else if (bytes < 1024 * 1024 * 1024)
		{
			return df.format(bytes / 1024.0 / 1024.0) + " " + bundle.getString("byte-unit.mb");
		}
		else if (bytes < 1024L * 1024 * 1024 * 1024)
		{
			return df.format(bytes / 1024.0 / 1024.0 / 1024.0) + " " + bundle.getString("byte-unit.gb");
		}
		else if (bytes < 1024L * 1024 * 1024 * 1024 * 1024)
		{
			return df.format(bytes / 1024.0 / 1024.0 / 1024.0 / 1024.0) + " " + bundle.getString("byte-unit.tb");
		}
		else if (bytes < 1024L * 1024 * 1024 * 1024 * 1024 * 1024)
		{
			return df.format(bytes / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0) + " " + bundle.getString("byte-unit.pb");
		}
		return df.format(bytes / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0 / 1024.0) + " " + bundle.getString("byte-unit.eb");
	}
}
