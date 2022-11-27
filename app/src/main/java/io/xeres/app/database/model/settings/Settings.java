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

package io.xeres.app.database.model.settings;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Table(name = "settings")
@Entity
public class Settings
{
	@SuppressWarnings("unused")
	@Id
	private final byte lock = 1;

	// The following 4 should not be exposed by JSON. The mapper must ignore them.
	private byte[] pgpPrivateKeyData;

	private byte[] locationPrivateKeyData;
	private byte[] locationPublicKeyData;
	private byte[] locationCertificate;

	// The following should not be stored in any database
	@Transient
	private String localIpAddress;
	@Transient
	private int localPort;

	private String torSocksHost;
	private int torSocksPort;

	private String i2pSocksHost;
	private int i2pSocksPort;

	private boolean upnpEnabled;

	private boolean broadcastDiscoveryEnabled;

	private boolean dhtEnabled;

	private boolean autoStartEnabled;

	protected Settings()
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

	public String getTorSocksHost()
	{
		return torSocksHost;
	}

	public void setTorSocksHost(String torSocksHost)
	{
		this.torSocksHost = torSocksHost;
	}

	public int getTorSocksPort()
	{
		return torSocksPort;
	}

	public void setTorSocksPort(int torSocksPort)
	{
		this.torSocksPort = torSocksPort;
	}

	public String getI2pSocksHost()
	{
		return i2pSocksHost;
	}

	public void setI2pSocksHost(String i2pSocksHost)
	{
		this.i2pSocksHost = i2pSocksHost;
	}

	public int getI2pSocksPort()
	{
		return i2pSocksPort;
	}

	public void setI2pSocksPort(int i2pSocksPort)
	{
		this.i2pSocksPort = i2pSocksPort;
	}

	public boolean isUpnpEnabled()
	{
		return upnpEnabled;
	}

	public void setUpnpEnabled(boolean enabled)
	{
		upnpEnabled = enabled;
	}

	public boolean isBroadcastDiscoveryEnabled()
	{
		return broadcastDiscoveryEnabled;
	}

	public void setBroadcastDiscoveryEnabled(boolean enabled)
	{
		broadcastDiscoveryEnabled = enabled;
	}

	public boolean isDhtEnabled()
	{
		return dhtEnabled;
	}

	public void setDhtEnabled(boolean dhtEnabled)
	{
		this.dhtEnabled = dhtEnabled;
	}

	public String getLocalIpAddress()
	{
		return localIpAddress;
	}

	public void setLocalIpAddress(String localIpAddress)
	{
		this.localIpAddress = localIpAddress;
	}

	public int getLocalPort()
	{
		return localPort;
	}

	public void setLocalPort(int localPort)
	{
		this.localPort = localPort;
	}

	public boolean isAutoStartEnabled()
	{
		return autoStartEnabled;
	}

	public void setAutoStartEnabled(boolean autoStartEnabled)
	{
		this.autoStartEnabled = autoStartEnabled;
	}
}
