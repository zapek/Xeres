/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

import io.xeres.common.protocol.ActivationMode;
import org.apache.commons.lang3.StringUtils;

public class Settings implements Cloneable
{
	private String torSocksHost;
	private int torSocksPort;

	private String i2pSocksHost;
	private int i2pSocksPort;

	private ActivationMode upnpActivationMode;

	private ActivationMode broadcastDiscoveryActivationMode;

	private boolean dhtEnabled;

	private boolean dnsLookupEnabled;

	private boolean autoStartEnabled;

	private String incomingDirectory;

	private String remotePassword;

	private boolean remoteEnabled;

	private boolean isUpnpRemoteEnabled;

	private int remotePort;

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

	public ActivationMode getUpnpActivationMode()
	{
		return upnpActivationMode;
	}

	public void setUpnpActivationMode(ActivationMode upnpActivationMode)
	{
		this.upnpActivationMode = upnpActivationMode;
	}

	public ActivationMode getBroadcastDiscoveryActivationMode()
	{
		return broadcastDiscoveryActivationMode;
	}

	public void setBroadcastDiscoveryActivationMode(ActivationMode broadcastDiscoveryActivationMode)
	{
		this.broadcastDiscoveryActivationMode = broadcastDiscoveryActivationMode;
	}

	public boolean isDhtEnabled()
	{
		return dhtEnabled;
	}

	public void setDhtEnabled(boolean dhtEnabled)
	{
		this.dhtEnabled = dhtEnabled;
	}

	public boolean isDnsLookupEnabled()
	{
		return dnsLookupEnabled;
	}

	public void setDnsLookupEnabled(boolean dnsLookupEnabled)
	{
		this.dnsLookupEnabled = dnsLookupEnabled;
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

	public boolean isUpnpRemoteEnabled()
	{
		return isUpnpRemoteEnabled;
	}

	public void setUpnpRemoteEnabled(boolean upnpRemoteEnabled)
	{
		isUpnpRemoteEnabled = upnpRemoteEnabled;
	}

	public int getRemotePort()
	{
		return remotePort;
	}

	public void setRemotePort(int remotePort)
	{
		this.remotePort = remotePort;
	}

	@Override
	public Settings clone()
	{
		try
		{
			return (Settings) super.clone();
		}
		catch (CloneNotSupportedException _)
		{
			throw new AssertionError();
		}
	}
}
