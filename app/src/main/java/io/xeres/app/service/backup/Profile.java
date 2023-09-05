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

package io.xeres.app.service.backup;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(name = "localProfile")
class Profile
{
	private byte[] pgpPrivateKey;

	@SuppressWarnings("unused")
	public Profile()
	{
		// Default constructor
	}

	public Profile(byte[] pgpPrivateKey)
	{
		this.pgpPrivateKey = pgpPrivateKey;
	}

	@XmlAttribute
	public byte[] getPgpPrivateKey()
	{
		return pgpPrivateKey;
	}

	public void setPgpPrivateKey(byte[] pgpPrivateKey)
	{
		this.pgpPrivateKey = pgpPrivateKey;
	}
}
