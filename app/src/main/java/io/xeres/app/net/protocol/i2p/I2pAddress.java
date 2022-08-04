/*
 * Copyright (c) 2019-2022 by David Gerber - https://zapek.com
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

package io.xeres.app.net.protocol.i2p;

import java.util.regex.Pattern;

public final class I2pAddress
{
	private static final Pattern I2P_B32_PATTERN = Pattern.compile("[a-z2-7]{52}\\.b32.i2p:[0-9]{1,5}");

	private I2pAddress()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static boolean isValidAddress(String address)
	{
		return I2P_B32_PATTERN.matcher(address).matches();
	}
}
