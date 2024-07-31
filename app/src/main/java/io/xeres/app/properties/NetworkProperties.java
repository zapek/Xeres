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

package io.xeres.app.properties;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
	 * Sets the encrypted tunnel format.
	 * <ul>
	 *     <li>ChaCha20 with HMAC SHA-256 {@code "chacha20-sha256"}: the default of Retroshare</li>
	 *     <li>ChaCha20 with Poly1305 authenticator {@code "chacha20-poly1305"}: should be accepted by Retroshare, but untested</li>
	 * </ul>
	 */
	private String tunnelEncryption = TUNNEL_ENCRYPTION_CHACHA20_SHA256;
	public static final String TUNNEL_ENCRYPTION_CHACHA20_SHA256 = "chacha20-sha256";
	public static final String TUNNEL_ENCRYPTION_CHACHA20_POLY1305 = "chacha20-poly1305";

	private String fileTransferStrategy = FILE_TRANSFER_STRATEGY_LINEAR;
	public static final String FILE_TRANSFER_STRATEGY_LINEAR = "linear";
	public static final String FILE_TRANSFER_STRATEGY_RANDOM = "random";

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

	public String getTunnelEncryption()
	{
		return tunnelEncryption;
	}

	public void setTunnelEncryption(String tunnelEncryption)
	{
		this.tunnelEncryption = tunnelEncryption;
	}

	public String getFileTransferStrategy()
	{
		return fileTransferStrategy;
	}

	public void setFileTransferStrategy(String fileTransferStrategy)
	{
		this.fileTransferStrategy = fileTransferStrategy;
	}
}
