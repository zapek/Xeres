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

class Identity
{
	private String name;
	private byte[] privateKey;
	private byte[] publicKey;

	@SuppressWarnings("unused")
	public Identity()
	{
		// Default constructor
	}

	public Identity(String name, byte[] privateKey, byte[] publicKey)
	{
		this.name = name;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}

	@XmlAttribute
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@XmlAttribute
	public byte[] getPrivateKey()
	{
		return privateKey;
	}

	public void setPrivateKey(byte[] privateKey)
	{
		this.privateKey = privateKey;
	}

	@XmlAttribute
	public byte[] getPublicKey()
	{
		return publicKey;
	}

	public void setPublicKey(byte[] publicKey)
	{
		this.publicKey = publicKey;
	}
}
