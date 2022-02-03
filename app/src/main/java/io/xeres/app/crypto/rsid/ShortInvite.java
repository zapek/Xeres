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

package io.xeres.app.crypto.rsid;

import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.common.dto.profile.ProfileConstants;
import io.xeres.common.id.LocationId;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateParsingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static io.xeres.app.crypto.rsid.RSIdArmor.*;

class ShortInvite extends RSId
{
	private static final Logger log = LoggerFactory.getLogger(ShortInvite.class);

	static final int SSL_ID = 0x0;
	static final int NAME = 0x1;
	static final int LOCATOR = 0x2;
	static final int PGP_FINGERPRINT = 0x3;
	static final int CHECKSUM = 0x4;
	static final int HIDDEN_LOCATOR = 0x90;
	static final int DNS_LOCATOR = 0x91;
	static final int EXT4_LOCATOR = 0x92;
	static final int LOC4_LOCATOR = 0x93;

	private String name;
	private LocationId locationId;

	private byte[] pgpFingerprint;
	private PeerAddress hiddenLocator;
	private PeerAddress ext4Locator;
	private PeerAddress loc4Locator;
	private PeerAddress hostnameLocator;
	private final Set<PeerAddress> locators = new HashSet<>();

	public ShortInvite()
	{
	}

	public static ShortInvite parseShortInvite(String data) throws CertificateParsingException
	{
		try
		{
			var shortInvite = new ShortInvite();
			byte[] shortInviteBytes = Base64.getDecoder().decode(cleanupInput(data.getBytes()));
			int checksum = RSIdCrc.calculate24bitsCrc(shortInviteBytes, shortInviteBytes.length - 5); // ignore the checksum PTAG which is 5 bytes in total and at the end
			var in = new ByteArrayInputStream(shortInviteBytes);
			Boolean checksumPassed = null;

			while (in.available() > 0)
			{
				int ptag = in.read();
				int size = getPacketSize(in);
				if (size == 0)
				{
					continue; // not seen in the wild yet but just skip them in any case
				}
				var buf = new byte[size];
				if (in.readNBytes(buf, 0, size) != size)
				{
					throw new IllegalArgumentException("Packet " + ptag + " is shorter than its advertised size");
				}

				switch (ptag)
				{
					case PGP_FINGERPRINT -> shortInvite.setPgpFingerprint(buf);
					case NAME -> shortInvite.setName(buf);
					case SSL_ID -> shortInvite.setLocationId(new LocationId(buf));
					case DNS_LOCATOR -> shortInvite.setDnsName(buf);
					case HIDDEN_LOCATOR -> shortInvite.setHiddenNodeAddress(buf);
					case EXT4_LOCATOR -> shortInvite.setExt4Locator(buf);
					case LOC4_LOCATOR -> shortInvite.setLoc4Locator(buf);
					case LOCATOR -> shortInvite.addLocator(new String(buf));
					case CHECKSUM -> {
						if (buf.length != 3)
						{
							throw new IllegalArgumentException("Checksum corrupted");
						}
						checksumPassed = checksum == (Byte.toUnsignedInt(buf[2]) << 16 | Byte.toUnsignedInt(buf[1]) << 8 | Byte.toUnsignedInt(buf[0])); // little endian
					}
					default -> ShortInvite.log.warn("Unhandled tag {}, ignoring.", ptag);
				}
			}

			if (checksumPassed == null)
			{
				throw new IllegalArgumentException("Missing checksum packet");
			}
			else if (Boolean.FALSE.equals(checksumPassed))
			{
				throw new IllegalArgumentException("Wrong checksum");
			}
			return shortInvite;
		}
		catch (IllegalArgumentException | IOException e)
		{
			throw new CertificateParsingException("Parse error: " + e.getMessage(), e);
		}
	}

	public void setExt4Locator(byte[] data)
	{
		ext4Locator = PeerAddress.fromByteArray(swapBytes(data));
	}

	public void setExt4Locator(String ipAndPort)
	{
		ext4Locator = PeerAddress.fromIpAndPort(ipAndPort);
	}

	private void setLoc4Locator(byte[] data)
	{
		loc4Locator = PeerAddress.fromByteArray(swapBytes(data));
	}

	public void setLoc4Locator(String ipAndPort)
	{
		loc4Locator = PeerAddress.fromIpAndPort(ipAndPort);
	}

	@Override
	public boolean hasInternalIp()
	{
		return loc4Locator != null && loc4Locator.isValid();
	}

	@Override
	public PeerAddress getInternalIp()
	{
		return loc4Locator;
	}

	@Override
	public boolean hasExternalIp()
	{
		return ext4Locator != null && ext4Locator.isValid();
	}

	@Override
	public PeerAddress getExternalIp()
	{
		return ext4Locator;
	}

	public void setPgpFingerprint(byte[] pgpFingerprint)
	{
		this.pgpFingerprint = pgpFingerprint;
	}

	@Override
	public byte[] getPgpFingerprint()
	{
		return pgpFingerprint;
	}

	@Override
	public boolean hasName()
	{
		return name != null;
	}

	@Override
	public String getName()
	{
		return name;
	}

	public void setName(byte[] name)
	{
		this.name = StringUtils.substring(new String(name, StandardCharsets.UTF_8), 0, ProfileConstants.NAME_LENGTH_MAX);
	}

	@Override
	public boolean hasLocationInfo()
	{
		return locationId != null;
	}

	@Override
	public LocationId getLocationId()
	{
		return locationId;
	}

	public void setLocationId(LocationId locationId)
	{
		this.locationId = locationId;
	}

	@Override
	public boolean hasDnsName()
	{
		return hostnameLocator != null;
	}

	@Override
	public PeerAddress getDnsName()
	{
		return hostnameLocator;
	}

	@Override
	public byte[] getDnsNameAsBytes()
	{
		return hostnameLocator.getAddressAsBytes().orElseThrow();
	}

	public void setDnsName(String dnsName)
	{
		hostnameLocator = PeerAddress.fromHostnameAndPort(dnsName);
	}

	private void setDnsName(byte[] portAndDns)
	{
		if (portAndDns == null || portAndDns.length <= 3 || portAndDns.length > 255)
		{
			throw new IllegalArgumentException("DNS name format is wrong");
		}

		int port = Byte.toUnsignedInt(portAndDns[0]) << 8 | Byte.toUnsignedInt(portAndDns[1]);
		var hostname = new String(Arrays.copyOfRange(portAndDns, 2, portAndDns.length), StandardCharsets.US_ASCII);
		hostnameLocator = PeerAddress.fromHostname(hostname, port);
	}

	@Override
	public boolean isHiddenNode()
	{
		return hiddenLocator != null;
	}

	@Override
	public PeerAddress getHiddenNodeAddress()
	{
		return hiddenLocator;
	}

	private void setHiddenNodeAddress(String hiddenNodeAddress)
	{
		this.hiddenLocator = PeerAddress.fromHidden(hiddenNodeAddress);
	}

	private void setHiddenNodeAddress(byte[] hiddenNodeAddress)
	{
		if (hiddenNodeAddress != null && hiddenNodeAddress.length >= 5 && hiddenNodeAddress.length <= 255)
		{
			setHiddenNodeAddress(new String(hiddenNodeAddress, StandardCharsets.US_ASCII));
		}
		else
		{
			this.hiddenLocator = PeerAddress.fromInvalid();
		}
	}

	public void addLocator(String locator)
	{
		var peerAddress = PeerAddress.fromUrl(locator);

		if (peerAddress.isValid())
		{
			locators.add(peerAddress);
		}
	}

	@Override
	public boolean hasLocators()
	{
		return !locators.isEmpty();
	}

	@Override
	public Set<PeerAddress> getLocators()
	{
		return locators;
	}

	@Override
	public String getArmored()
	{
		var out = new ByteArrayOutputStream();

		addPacket(SSL_ID, getLocationId().getBytes(), out);
		addPacket(NAME, getName().getBytes(), out);
		addPacket(PGP_FINGERPRINT, getPgpFingerprint(), out);
		if (isHiddenNode())
		{
			addPacket(HIDDEN_LOCATOR, getHiddenNodeAddress().getAddressAsBytes().orElseThrow(), out);
		}
		else
		{
			if (hasDnsName())
			{
				addPacket(DNS_LOCATOR, swapDnsBytes(getDnsNameAsBytes()), out);
			}
			if (hasExternalIp())
			{
				addPacket(EXT4_LOCATOR, swapBytes(getExternalIp().getAddressAsBytes().orElseThrow()), out);
			}
			if (hasInternalIp())
			{
				addPacket(LOC4_LOCATOR, swapBytes(getInternalIp().getAddressAsBytes().orElseThrow()), out);
			}
			if (hasLocators())
			{
				// Use one locator. Ideally, the first one should be the most recent address
				getLocators().stream()
						.findFirst()
						.ifPresent(peerAddress -> addPacket(LOCATOR, peerAddress.getUrl().getBytes(StandardCharsets.US_ASCII), out));
			}
		}
		// Note that we don't use LOC4_LOCATOR as we expect the broadcast discovery to work
		addCrcPacket(CHECKSUM, out);

		return wrapWithBase64(out.toByteArray(), RSIdArmor.WrapMode.CONTINUOUS);
	}

	/**
	 * Retroshare puts IP addresses in big-endian in certificates, but when it comes
	 * to short invites, a mistake was made and, while the port is in big-endian, the
	 * IP address is not. Since the mistake is done on output and input, it works fine
	 * within Retroshare so a workaround has to be implemented here.
	 *
	 * @param data the IP address + port
	 * @return the IP address in swapped endian + port left alone
	 */
	static byte[] swapBytes(byte[] data)
	{
		if (data == null || data.length != 6)
		{
			return data; // don't touch anything, input is bad
		}
		var bytes = new byte[6];
		bytes[0] = data[3];
		bytes[1] = data[2];
		bytes[2] = data[1];
		bytes[3] = data[0];
		bytes[4] = data[4];
		bytes[5] = data[5];

		return bytes;
	}

	private static byte[] swapDnsBytes(byte[] data)
	{
		if (data == null || data.length < 4)
		{
			return data; // don't touch anything, input is bad
		}
		var bytes = new byte[data.length];
		System.arraycopy(data, 0, bytes, 2, data.length - 2);
		bytes[0] = data[data.length - 2];
		bytes[1] = data[data.length - 1];

		return bytes;
	}
}
