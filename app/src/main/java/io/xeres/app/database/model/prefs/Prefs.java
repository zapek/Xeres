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

package io.xeres.app.database.model.prefs;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "prefs")
@Entity
public class Prefs
{
	@SuppressWarnings("unused")
	@Id
	private final byte lock = 1;

	private byte[] pgpPrivateKeyData;

	private byte[] locationPrivateKeyData;
	private byte[] locationPublicKeyData;
	private byte[] locationCertificate;

	protected Prefs()
	{
	}

	public byte[] getPgpPrivateKeyData()
	{
		return pgpPrivateKeyData;
	}

	public void setPgpPrivateKeyData(byte[] keyData)
	{
		this.pgpPrivateKeyData = keyData;
	}

	public byte[] getLocationPrivateKeyData()
	{
		return locationPrivateKeyData;
	}

	public void setLocationPrivateKeyData(byte[] keyData)
	{
		this.locationPrivateKeyData = keyData;
	}

	public byte[] getLocationPublicKeyData()
	{
		return locationPublicKeyData;
	}

	public void setLocationPublicKeyData(byte[] keyData)
	{
		this.locationPublicKeyData = keyData;
	}

	public byte[] getLocationCertificate()
	{
		return locationCertificate;
	}

	public void setLocationCertificate(byte[] certificate)
	{
		this.locationCertificate = certificate;
	}

	public boolean hasLocationCertificate()
	{
		return locationCertificate != null;
	}
}
