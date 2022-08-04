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

package io.xeres.app.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Configuration
@ConfigurationProperties(prefix = "xrs.network")
public class NetworkProperties
{
	/**
	 * Enables the slicing of packets. This is only available on new Retroshare packets and only if both ends
	 * of the connection agree to use them. Note that Xeres always accepts sliced packets.
	 */
	private boolean packetSlicing = false;

	/**
	 * Enables the grouping of packets. Only works if packet slicing is enabled.
	 */
	private boolean packetGrouping = false;

	/**
	 * Enables the DHT (Mainline DHT).
	 */
	private boolean dht = false;

	/**
	 * Enables Tor by setting the SOCKS 5 server address
	 */
	private String torSocksAddress;

	/**
	 * Sets the Tor SOCKS 5 port (default: 9050)
	 */
	private int torSocksPort = 9050;

	/**
	 * Enables I2P by setting the SOCKS 5 server address
	 */
	private String i2pSocksAddress;

	/**
	 * Sets the I2P SOCKS 5 port (default: 4447)
	 */
	private int i2pSocksPort = 4447;

	@PostConstruct
	private void checkConsistency()
	{
		if (packetGrouping && !packetSlicing)
		{
			throw new IllegalStateException("'network.packet-grouping' property cannot be enabled without 'network.packet-slicing'");
		}
	}

	public String getFeatures()
	{
		return "packet slicing: " + packetSlicing + ", " +
				"packet grouping: " + packetGrouping;
	}

	public boolean isPacketSlicing()
	{
		return packetSlicing;
	}

	public void setPacketSlicing(boolean packetSlicing)
	{
		this.packetSlicing = packetSlicing;
	}

	public boolean isPacketGrouping()
	{
		return packetGrouping;
	}

	public void setPacketGrouping(boolean packetGrouping)
	{
		this.packetGrouping = packetGrouping;
	}

	public boolean isDht()
	{
		return dht;
	}

	public void setDht(boolean dht)
	{
		this.dht = dht;
	}

	public boolean hasTorSocksConfigured()
	{
		return isNotBlank(torSocksAddress) && torSocksPort != 0;
	}

	public String getTorSocksAddress()
	{
		return torSocksAddress;
	}

	public void setTorSocksAddress(String torSocksAddress)
	{
		this.torSocksAddress = torSocksAddress;
	}

	public int getTorSocksPort()
	{
		return torSocksPort;
	}

	public void setTorSocksPort(int torSocksPort)
	{
		this.torSocksPort = torSocksPort;
	}

	public boolean hasI2pSocksConfigured()
	{
		return isNotBlank(i2pSocksAddress) && i2pSocksPort != 0;
	}

	public String getI2pSocksAddress()
	{
		return i2pSocksAddress;
	}

	public void setI2pSocksAddress(String i2pSocksAddress)
	{
		this.i2pSocksAddress = i2pSocksAddress;
	}

	public int getI2pSocksPort()
	{
		return i2pSocksPort;
	}

	public void setI2pSocksPort(int i2pSocksPort)
	{
		this.i2pSocksPort = i2pSocksPort;
	}
}
