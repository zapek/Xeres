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

import io.xeres.common.id.LocationIdentifier;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlType(name = "localLocation")
class Location
{
	private LocationIdentifier locationIdentifier;
	private byte[] privateKey;
	private byte[] publicKey;
	private byte[] x509Certificate;
	private int localPort;

	@SuppressWarnings("unused")
	public Location()
	{
		// Default constructor
	}

	public Location(LocationIdentifier locationIdentifier, byte[] privateKey, byte[] publicKey, byte[] x509Certificate, int localPort)
	{
		this.locationIdentifier = locationIdentifier;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		this.x509Certificate = x509Certificate;
		this.localPort = localPort;
	}

	@XmlAttribute(name = "locationId")
	@XmlJavaTypeAdapter(LocationIdentifierXmlAdapter.class)
	public LocationIdentifier getLocationIdentifier()
	{
		return locationIdentifier;
	}

	public void setLocationIdentifier(LocationIdentifier locationIdentifier)
	{
		this.locationIdentifier = locationIdentifier;
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

	@XmlAttribute
	public byte[] getX509Certificate()
	{
		return x509Certificate;
	}

	public void setX509Certificate(byte[] x509Certificate)
	{
		this.x509Certificate = x509Certificate;
	}

	@XmlAttribute
	public int getLocalPort()
	{
		return localPort;
	}

	public void setLocalPort(int localPort)
	{
		this.localPort = localPort;
	}
}
