/*
 * Copyright (c) 2024-2026 by David Gerber - https://zapek.com
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

package io.xeres.common;

public final class Features
{
	/**
	 * Enable experimental generation of Elliptic Curve keys.
	 */
	public static final boolean EXPERIMENTAL_EC = false;

	/**
	 * Use patch for the settings. Should always be enabled
	 * unless the patch support breaks. It currently relies on a Jackson
	 * module and a default JSON-P implementation.
	 */
	public static final boolean USE_PATCH_SETTINGS = true;

	private Features()
	{
		throw new UnsupportedOperationException("Utility class");
	}
}
