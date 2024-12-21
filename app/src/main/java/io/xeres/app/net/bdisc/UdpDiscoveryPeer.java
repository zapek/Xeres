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

package io.xeres.app.net.bdisc;

import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.id.ProfileFingerprint;

import java.time.Instant;

public class UdpDiscoveryPeer
{
	public enum Status
	{
		PRESENT,
		LEAVING // Not implemented. I don't see the point
	}

	private Status status;
	private int appId;
	private int peerId;
	private long packetIndex;
	private String ipAddress;

	private ProfileFingerprint fingerprint;
	private LocationIdentifier locationIdentifier;
	private int localPort;
	private String profileName;

	private Instant lastSeen;

	public Status getStatus()
	{
		return status;
	}

	public void setStatus(Status status)
	{
		this.status = status;
	}

	public int getAppId()
	{
		return appId;
	}

	public void setAppId(int appId)
	{
		this.appId = appId;
	}

	public int getPeerId()
	{
		return peerId;
	}

	public void setPeerId(int peerId)
	{
		this.peerId = peerId;
	}

	public long getPacketIndex()
	{
		return packetIndex;
	}

	public void setPacketIndex(long packetIndex)
	{
		this.packetIndex = packetIndex;
	}

	public String getIpAddress()
	{
		return ipAddress;
	}

	public void setIpAddress(String ipAddress)
	{
		this.ipAddress = ipAddress;
	}

	public ProfileFingerprint getFingerprint()
	{
		return fingerprint;
	}

	public void setFingerprint(ProfileFingerprint fingerprint)
	{
		this.fingerprint = fingerprint;
	}

	public LocationIdentifier getLocationIdentifier()
	{
		return locationIdentifier;
	}

	public void setLocationIdentifier(LocationIdentifier locationIdentifier)
	{
		this.locationIdentifier = locationIdentifier;
	}

	public int getLocalPort()
	{
		return localPort;
	}

	public void setLocalPort(int localPort)
	{
		this.localPort = localPort;
	}

	public String getProfileName()
	{
		return profileName;
	}

	public void setProfileName(String profileName)
	{
		this.profileName = profileName;
	}

	public Instant getLastSeen()
	{
		return lastSeen;
	}

	public void setLastSeen(Instant lastSeen)
	{
		this.lastSeen = lastSeen;
	}

	@Override
	public String toString()
	{
		return "UdpDiscoveryPeer{" +
				"status=" + status +
				", AppId=" + appId +
				", peerId=" + peerId +
				", packetIndex=" + packetIndex +
				", fingerprint=" + fingerprint +
				", locationIdentifier=" + locationIdentifier +
				", ipAddress='" + ipAddress + '\'' +
				", localPort=" + localPort +
				", profileName='" + profileName + '\'' +
				'}';
	}
}
