/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

package io.xeres.app.crypto.rsid.shortinvite;

import io.xeres.app.crypto.rsid.RSId;
import io.xeres.app.crypto.rsid.RSIdCrc;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.common.id.LocationId;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateParsingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public class ShortInvite extends RSId
{
	private static final Logger log = LoggerFactory.getLogger(ShortInvite.class);

	private String name;
	private LocationId locationId;

	private byte[] pgpFingerprint;
	private PeerAddress hiddenLocator;
	private PeerAddress ext4Locator;
	private PeerAddress loc4Locator;
	private PeerAddress hostnameLocator;
	private final Set<String> locators = new HashSet<>();

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
					case ShortInviteTags.PGP_FINGERPRINT:
						shortInvite.setPgpFingerprint(buf);
						break;

					case ShortInviteTags.NAME:
						shortInvite.setName(buf);
						break;

					case ShortInviteTags.SSLID:
						shortInvite.setLocationId(new LocationId(buf));
						break;

					case ShortInviteTags.DNS_LOCATOR:
						shortInvite.setDnsName(buf);
						break;

					case ShortInviteTags.HIDDEN_LOCATOR:
						shortInvite.setHiddenNodeAddress(buf);
						break;

					case ShortInviteTags.EXT4_LOCATOR:
						shortInvite.setExt4Locator(buf);
						break;

					case ShortInviteTags.LOC4_LOCATOR:
						shortInvite.setLoc4Locator(buf);
						break;

					case ShortInviteTags.CHECKSUM:
						if (buf.length != 3)
						{
							throw new IllegalArgumentException("Checksum corrupted");
						}
						checksumPassed = checksum == (Byte.toUnsignedInt(buf[2]) << 16 | Byte.toUnsignedInt(buf[1]) << 8 | Byte.toUnsignedInt(buf[0])); // little endian
						break;

					case ShortInviteTags.LOCATOR:
						// XXX: handle the URLs...
						break;

					default:
						ShortInvite.log.warn("Unhandled tag {}, ignoring.", ptag);
						break;

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
		ext4Locator = PeerAddress.fromByteArray(ShortInviteQuirks.swapBytes(data));
	}

	public void setExt4Locator(String ipAndPort)
	{
		ext4Locator = PeerAddress.fromIpAndPort(ipAndPort);
	}

	private void setLoc4Locator(byte[] data)
	{
		loc4Locator = PeerAddress.fromByteArray(ShortInviteQuirks.swapBytes(data));
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

	@Override
	public boolean hasPgpPublicKey()
	{
		return false;
	}

	@Override
	public PGPPublicKey getPgpPublicKey()
	{
		return null;
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
		// XXX: need maximum name size
		this.name = new String(name, StandardCharsets.UTF_8);
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
		return false;
	}

	@Override
	public PeerAddress getDnsName()
	{
		return hostnameLocator;
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

	@Override
	public boolean hasLocators()
	{
		return !locators.isEmpty();
	}

	@Override
	public Set<String> getLocators()
	{
		return locators;
	}
}
