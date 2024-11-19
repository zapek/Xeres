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

package io.xeres.common.dto.identity;

public final class IdentityConstants
{
	// Those must be like the profile, because the name is derived from it
	public static final int NAME_LENGTH_MIN = 2;
	public static final int NAME_LENGTH_MAX = 30;

	public static final long NO_IDENTITY_ID = 0L;
	public static final long OWN_IDENTITY_ID = 1L;

	private IdentityConstants()
	{
		throw new UnsupportedOperationException("Utility class");
	}
}
