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

package io.xeres.app.crypto.rsid.certificate;

import io.xeres.app.crypto.pgp.PGP;
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
import java.security.InvalidKeyException;
import java.security.cert.CertificateParsingException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public final class RSCertificate extends RSId
{
	private static final Logger log = LoggerFactory.getLogger(RSCertificate.class);

	public static final int VERSION_06 = 6;

	private PGPPublicKey pgpPublicKey;

	private String name;
	private LocationId locationIdentifier;

	// Note that dnsName is not supported because it doesn't have a port (XXX: it could be supported by using the one from externalIp though)
	private PeerAddress hiddenNodeAddress;
	private PeerAddress internalIp;
	private PeerAddress externalIp;
	private Set<String> locators = new HashSet<>();

	public RSCertificate()
	{
	}

	public static RSCertificate parseRSCertificate(String data) throws CertificateParsingException
	{
		try
		{
			var cert = new RSCertificate();
			byte[] certBytes = Base64.getDecoder().decode(cleanupInput(data.getBytes()));
			int checksum = RSIdCrc.calculate24bitsCrc(certBytes, certBytes.length - 5); // ignore the checksum PTAG which is 5 bytes in total and at the end
			var in = new ByteArrayInputStream(certBytes);
			var version = 0;
			Boolean checksumPassed = null;

			while (in.available() > 0)
			{
				int ptag = in.read();
				int size = getPacketSize(in);
				if (size == 0)
				{
					continue; // seen in the wild, just skip them
				}
				var buf = new byte[size];
				if (in.readNBytes(buf, 0, size) != size)
				{
					throw new IllegalArgumentException("Packet " + ptag + " is shorter than its advertised size");
				}

				switch (ptag)
				{
					case RSCertificateTags.VERSION:
						version = buf[0];
						break;

					case RSCertificateTags.PGP:
						cert.setPgpPublicKey(buf);
						break;

					case RSCertificateTags.NAME:
						cert.setLocationName(buf);
						break;

					case RSCertificateTags.SSLID:
						cert.setLocationId(new LocationId(buf));
						break;

					case RSCertificateTags.DNS:
						cert.setDnsName(buf);
						break;

					case RSCertificateTags.HIDDEN_NODE:
						cert.setHiddenNodeAddress(buf);
						break;

					case RSCertificateTags.INTERNAL_IP_AND_PORT:
						cert.setInternalIp(buf);
						break;

					case RSCertificateTags.EXTERNAL_IP_AND_PORT:
						cert.setExternalIp(buf);
						break;

					case RSCertificateTags.CHECKSUM:
						if (buf.length != 3)
						{
							throw new IllegalArgumentException("Checksum corrupted");
						}
						checksumPassed = checksum == (Byte.toUnsignedInt(buf[2]) << 16 | Byte.toUnsignedInt(buf[1]) << 8 | Byte.toUnsignedInt(buf[0])); // little endian
						break;

					case RSCertificateTags.EXTRA_LOCATOR:
						// XXX: insert the URLs (I probably need a RsUrl object...
						break;

					default:
						RSCertificate.log.warn("Unhandled tag {}, ignoring.", ptag);
						break;
				}
			}

			if (version == 0)
			{
				throw new IllegalArgumentException("Missing certificate version");
			}
			else if (version != RSCertificate.VERSION_06)
			{
				throw new IllegalArgumentException("Wrong certificate version: " + version);
			}

			if (checksumPassed == null)
			{
				throw new IllegalArgumentException("Missing checksum packet");
			}
			else if (Boolean.FALSE.equals(checksumPassed))
			{
				throw new IllegalArgumentException("Wrong checksum");
			}
			return cert;
		}
		catch (IllegalArgumentException | IOException e)
		{
			throw new CertificateParsingException("Parse error: " + e.getMessage(), e);
		}
	}

	public void setInternalIp(byte[] data)
	{
		internalIp = PeerAddress.fromByteArray(data);
	}

	public void setInternalIp(String ipAndPort)
	{
		internalIp = PeerAddress.fromIpAndPort(ipAndPort);
	}

	public void setExternalIp(byte[] data)
	{
		externalIp = PeerAddress.fromByteArray(data);
	}

	public void setExternalIp(String ipAndPort)
	{
		externalIp = PeerAddress.fromIpAndPort(ipAndPort);
	}

	@Override
	public boolean hasInternalIp()
	{
		return internalIp != null && internalIp.isValid();
	}

	@Override
	public PeerAddress getInternalIp()
	{
		return internalIp;
	}

	@Override
	public boolean hasExternalIp()
	{
		return externalIp != null && externalIp.isValid();
	}

	@Override
	public PeerAddress getExternalIp()
	{
		return externalIp;
	}

	@Override
	public boolean hasPgpPublicKey()
	{
		return pgpPublicKey != null;
	}

	@Override
	public PGPPublicKey getPgpPublicKey()
	{
		return pgpPublicKey;
	}

	public void setPgpPublicKey(byte[] data) throws CertificateParsingException
	{
		try
		{
			this.pgpPublicKey = PGP.getPGPPublicKey(data);
		}
		catch (InvalidKeyException e)
		{
			throw new CertificateParsingException("Error in public PGP key: " + e.getMessage(), e);
		}
	}

	@Override
	public byte[] getPgpFingerprint()
	{
		return pgpPublicKey.getFingerprint();
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

	public void setName(String name)
	{
		this.name = name;
	}

	public void setLocationName(byte[] name)
	{
		// XXX: we need a maximum location name. Find out what is RS' limit
		this.name = new String(name, StandardCharsets.UTF_8);
	}

	@Override
	public boolean hasLocationInfo()
	{
		return locationIdentifier != null;
	}

	@Override
	public LocationId getLocationId()
	{
		return locationIdentifier;
	}

	public void setLocationId(LocationId locationId)
	{
		this.locationIdentifier = locationId;
	}

	@Override
	public boolean hasDnsName()
	{
		return false;
	}

	@Override
	public PeerAddress getDnsName()
	{
		return null;
	}

	public void setDnsName(String dnsName)
	{
		// do nothing, we don't support those anymore
	}

	public void setDnsName(byte[] dnsName)
	{
		// ditto
	}

	@Override
	public boolean isHiddenNode()
	{
		return hiddenNodeAddress != null;
	}

	@Override
	public PeerAddress getHiddenNodeAddress()
	{
		return hiddenNodeAddress;
	}

	public void setHiddenNodeAddress(String hiddenNodeAddress)
	{
		this.hiddenNodeAddress = PeerAddress.fromHidden(hiddenNodeAddress);
	}

	public void setHiddenNodeAddress(byte[] hiddenNodeAddress)
	{
		if (hiddenNodeAddress != null && hiddenNodeAddress.length >= 5 && hiddenNodeAddress.length <= 255)
		{
			setHiddenNodeAddress(new String(hiddenNodeAddress, StandardCharsets.US_ASCII));
		}
		else
		{
			this.hiddenNodeAddress = PeerAddress.fromInvalid();
		}
	}

	@Override
	public boolean hasLocators()
	{
		return locators != null && !locators.isEmpty();
	}

	@Override
	public Set<String> getLocators()
	{
		return locators;
	}

	public void setLocators(Set<String> locators)
	{
		// XXX: make sure none of them is null
		this.locators = locators;
	}
}
