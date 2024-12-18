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

package io.xeres.ui.model.settings;

import io.micrometer.common.util.StringUtils;

public class Settings implements Cloneable
{
	private String torSocksHost;
	private int torSocksPort;

	private String i2pSocksHost;
	private int i2pSocksPort;

	private boolean upnpEnabled;

	private boolean broadcastDiscoveryEnabled;

	private boolean dhtEnabled;

	private boolean autoStartEnabled;

	private String incomingDirectory;

	private String remotePassword;

	private boolean remoteEnabled;

	private boolean isRemoteUpnpEnabled;

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

	public void setUpnpEnabled(boolean upnpEnabled)
	{
		this.upnpEnabled = upnpEnabled;
	}

	public boolean isBroadcastDiscoveryEnabled()
	{
		return broadcastDiscoveryEnabled;
	}

	public void setBroadcastDiscoveryEnabled(boolean broadcastDiscoveryEnabled)
	{
		this.broadcastDiscoveryEnabled = broadcastDiscoveryEnabled;
	}

	public boolean isDhtEnabled()
	{
		return dhtEnabled;
	}

	public void setDhtEnabled(boolean dhtEnabled)
	{
		this.dhtEnabled = dhtEnabled;
	}

	public boolean isAutoStartEnabled()
	{
		return autoStartEnabled;
	}

	public void setAutoStartEnabled(boolean autoStartEnabled)
	{
		this.autoStartEnabled = autoStartEnabled;
	}

	public boolean hasIncomingDirectory()
	{
		return StringUtils.isNotEmpty(incomingDirectory);
	}

	public String getIncomingDirectory()
	{
		return incomingDirectory;
	}

	public void setIncomingDirectory(String incomingDirectory)
	{
		this.incomingDirectory = incomingDirectory;
	}

	public String getRemotePassword()
	{
		return remotePassword;
	}

	public void setRemotePassword(String remotePassword)
	{
		this.remotePassword = remotePassword;
	}

	public boolean isRemoteEnabled()
	{
		return remoteEnabled;
	}

	public void setRemoteEnabled(boolean enabled)
	{
		remoteEnabled = enabled;
	}

	public boolean isRemoteUpnpEnabled()
	{
		return isRemoteUpnpEnabled;
	}

	public void setRemoteUpnpEnabled(boolean remoteUpnpEnabled)
	{
		isRemoteUpnpEnabled = remoteUpnpEnabled;
	}

	@Override
	public Settings clone()
	{
		try
		{
			return (Settings) super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new AssertionError();
		}
	}
}
