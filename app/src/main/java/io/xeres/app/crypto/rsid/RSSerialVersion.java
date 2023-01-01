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

package io.xeres.app.crypto.rsid;

import java.math.BigInteger;

public enum RSSerialVersion
{
	V06_0000("60000", "Retroshare 0.6.4 or earlier"), // RS 0.6.4 and earlier, before November 2017 (note that the version which is in the cert's serial number can be random)
	V06_0001("60001", "Retroshare 0.6.5"), // RS 0.6.5 after November 2017
	V07_0001("70001", "Retroshare 0.6.6"); // RS 0.6.6

	private final String versionString;
	private final String description;

	RSSerialVersion(String versionString, String description)
	{
		this.versionString = versionString;
		this.description = description;
	}

	public BigInteger serialNumber()
	{
		return new BigInteger(versionString, 16);
	}

	public String versionString()
	{
		return versionString;
	}

	@Override
	public String toString()
	{
		return description + " (" + versionString + ")";
	}

	public static RSSerialVersion getFromSerialNumber(BigInteger serialNumber)
	{
		for (var value : values())
		{
			if (value.serialNumber().equals(serialNumber))
			{
				return value;
			}
		}
		return V06_0000; // old versions used a random serial number
	}
}
