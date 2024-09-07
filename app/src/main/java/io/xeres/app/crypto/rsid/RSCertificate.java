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

package io.xeres.app.crypto.rsid;

import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.net.protocol.DomainNameSocketAddress;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.common.dto.profile.ProfileConstants;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.ProfileFingerprint;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.cert.CertificateParsingException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static io.xeres.app.crypto.rsid.RSIdArmor.*;

class RSCertificate extends RSId
{
	private static final Logger log = LoggerFactory.getLogger(RSCertificate.class);

	public static final int VERSION_06 = 6;

	static final int PGP_KEY = 1;
	static final int EXTERNAL_IP_AND_PORT = 2;
	static final int INTERNAL_IP_AND_PORT = 3;
	static final int DNS = 4;
	static final int SSL_ID = 5;
	static final int NAME = 6;
	static final int CHECKSUM = 7;
	static final int HIDDEN_NODE = 8;
	static final int VERSION = 9;
	static final int EXTRA_LOCATOR = 10;

	private PGPPublicKey pgpPublicKey;
	private ProfileFingerprint pgpFingerprint;

	private String name;
	private LocationId locationId;

	private PeerAddress hiddenNodeAddress;
	private PeerAddress internalIp;
	private PeerAddress externalIp;
	private PeerAddress dnsName;
	private final Set<PeerAddress> locators = new HashSet<>();

	RSCertificate()
	{
	}

	@SuppressWarnings("DuplicatedCode")
	@Override
	void parseInternal(String data) throws CertificateParsingException
	{
		try
		{
			var certBytes = Base64.getDecoder().decode(cleanupInput(data.getBytes()));
			var checksum = RSIdCrc.calculate24bitsCrc(certBytes, certBytes.length - 5); // ignore the checksum PTAG which is 5 bytes in total and at the end
			var in = new ByteArrayInputStream(certBytes);
			var version = 0;
			Boolean checksumPassed = null;

			while (in.available() > 0)
			{
				var pTag = in.read();
				var size = getPacketSize(in);
				if (size == 0)
				{
					continue; // seen in the wild, just skip them
				}
				var buf = new byte[size];
				if (in.readNBytes(buf, 0, size) != size)
				{
					throw new IllegalArgumentException("Packet " + pTag + " is shorter than its advertised size");
				}

				switch (pTag)
				{
					case VERSION -> version = buf[0];

					case PGP_KEY -> setPgpPublicKey(buf);

					case NAME -> setLocationName(buf);

					case SSL_ID -> setLocationId(new LocationId(buf));

					case DNS -> setDnsName(buf);

					case HIDDEN_NODE -> setHiddenNodeAddress(buf);

					case INTERNAL_IP_AND_PORT -> setInternalIp(buf);

					case EXTERNAL_IP_AND_PORT -> setExternalIp(buf);

					case CHECKSUM ->
					{
						if (buf.length != 3)
						{
							throw new IllegalArgumentException("Checksum corrupted");
						}
						checksumPassed = checksum == (Byte.toUnsignedInt(buf[2]) << 16 | Byte.toUnsignedInt(buf[1]) << 8 | Byte.toUnsignedInt(buf[0])); // little endian
					}

					case EXTRA_LOCATOR ->
					{
						// XXX: insert the URLs (I probably need a RsUrl object...
					}

					default -> log.trace("Unhandled tag {}, ignoring.", pTag);
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
		}
		catch (IllegalArgumentException | IOException e)
		{
			throw new CertificateParsingException("Parse error: " + e.getMessage(), e);
		}
	}

	@Override
	void checkRequiredFields()
	{
		if (getLocationId() == null)
		{
			throw new IllegalArgumentException("Missing location id");
		}
		if (getName() == null)
		{
			throw new IllegalArgumentException("Missing name");
		}
		if (getPgpPublicKey().isEmpty())
		{
			throw new IllegalArgumentException("Missing PGP public key");
		}

		addPortToDnsName();
	}

	private void addPortToDnsName()
	{
		if (dnsName != null && dnsName.isValid() && dnsName.getSocketAddress() instanceof DomainNameSocketAddress)
		{
			// Find another address for a port, then add it
			if (externalIp != null && externalIp.isValid())
			{
				dnsName = PeerAddress.fromHostname(dnsName.getAddress().orElseThrow(), ((InetSocketAddress) externalIp.getSocketAddress()).getPort());
			}
			else
			{
				dnsName = PeerAddress.fromInvalid();
			}
		}
	}

	void setPgpPublicKey(byte[] data) throws CertificateParsingException
	{
		try
		{
			setPgpPublicKey(PGP.getPGPPublicKey(data));
		}
		catch (InvalidKeyException e)
		{
			throw new CertificateParsingException("Error in RSCertificate PGP public key: " + e.getMessage(), e);
		}
	}

	/**
	 * Same as setPgpPublicKey() but from a valid PGP key data.
	 * This is done to avoid catching the exception.
	 *
	 * @param data the data
	 */
	void setVerifiedPgpPublicKey(byte[] data)
	{
		try
		{
			setPgpPublicKey(PGP.getPGPPublicKey(data));
		}
		catch (InvalidKeyException e)
		{
			throw new RuntimeException(e);
		}
	}

	void setPgpPublicKey(PGPPublicKey pgpPublicKey)
	{
		this.pgpPublicKey = pgpPublicKey;
		pgpFingerprint = new ProfileFingerprint(pgpPublicKey.getFingerprint());
	}

	private void setInternalIp(byte[] data)
	{
		internalIp = PeerAddress.fromByteArray(data);
	}

	void setInternalIp(String ipAndPort)
	{
		internalIp = PeerAddress.fromIpAndPort(ipAndPort);
	}

	private void setExternalIp(byte[] data)
	{
		externalIp = PeerAddress.fromByteArray(data);
	}

	void setExternalIp(String ipAndPort)
	{
		externalIp = PeerAddress.fromIpAndPort(ipAndPort);
	}

	private void setLocationName(byte[] name) throws CertificateParsingException
	{
		if (name.length > 255) // RS has no limit but let's enforce a sensible value
		{
			throw new CertificateParsingException("Certificate name too long: " + name.length);
		}
		this.name = new String(name, StandardCharsets.UTF_8);
	}

	void setLocationId(LocationId locationId)
	{
		this.locationId = locationId;
	}

	private void setHiddenNodeAddress(byte[] hiddenNodeAddress)
	{
		if (hiddenNodeAddress != null && hiddenNodeAddress.length >= 11 && hiddenNodeAddress.length <= 255)
		{
			setHiddenNodeAddress(new String(hiddenNodeAddress, StandardCharsets.US_ASCII));
		}
		else
		{
			this.hiddenNodeAddress = PeerAddress.fromInvalid();
		}
	}

	private void setHiddenNodeAddress(String hiddenNodeAddress)
	{
		this.hiddenNodeAddress = PeerAddress.fromHidden(hiddenNodeAddress);
	}

	@Override
	public Optional<PeerAddress> getInternalIp()
	{
		if (internalIp != null && internalIp.isValid())
		{
			return Optional.of(internalIp);
		}
		return Optional.empty();
	}

	@Override
	public Optional<PeerAddress> getExternalIp()
	{
		if (externalIp != null && externalIp.isValid())
		{
			return Optional.of(externalIp);
		}
		return Optional.empty();
	}

	@Override
	public ProfileFingerprint getPgpFingerprint()
	{
		return pgpFingerprint;
	}

	@Override
	public Optional<PGPPublicKey> getPgpPublicKey()
	{
		return Optional.ofNullable(pgpPublicKey);
	}

	@Override
	public String getName()
	{
		return name;
	}

	void setName(byte[] name)
	{
		this.name = StringUtils.substring(new String(name, StandardCharsets.UTF_8), 0, ProfileConstants.NAME_LENGTH_MAX);
	}

	@Override
	public LocationId getLocationId()
	{
		return locationId;
	}

	@Override
	public Optional<PeerAddress> getDnsName()
	{
		if (dnsName != null && dnsName.isValid())
		{
			return Optional.of(dnsName);
		}
		return Optional.empty();
	}

	private void setDnsName(byte[] dnsName)
	{
		setDnsName(new String(dnsName, StandardCharsets.US_ASCII));
	}

	void setDnsName(String dnsName)
	{
		this.dnsName = PeerAddress.fromHostname(dnsName);
	}

	@Override
	public Optional<PeerAddress> getHiddenNodeAddress()
	{
		if (hiddenNodeAddress != null && hiddenNodeAddress.isValid())
		{
			return Optional.of(hiddenNodeAddress);
		}
		return Optional.empty();
	}

	void addLocator(String locator)
	{
		var peerAddress = PeerAddress.fromUrl(locator);

		if (peerAddress.isValid())
		{
			locators.add(peerAddress);
		}
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

		addPacket(VERSION, new byte[]{RSCertificate.VERSION_06}, out);
		addPacket(PGP_KEY, getPgpPublicKeyData(pgpPublicKey), out);

		if (getHiddenNodeAddress().isPresent())
		{
			addPacket(HIDDEN_NODE, getHiddenNodeAddress().get().getAddressAsBytes().orElseThrow(), out);
		}
		else
		{
			getExternalIp().ifPresent(peerAddress -> addPacket(EXTERNAL_IP_AND_PORT, peerAddress.getAddressAsBytes().orElseThrow(), out));
			getInternalIp().ifPresent(peerAddress -> addPacket(INTERNAL_IP_AND_PORT, peerAddress.getAddressAsBytes().orElseThrow(), out));
			getDnsName().ifPresent(peerAddress -> addPacket(DNS, peerAddress.getAddressAsBytes().orElseThrow(), out));
		}

		addPacket(NAME, getName().getBytes(), out);
		addPacket(SSL_ID, getLocationId().getBytes(), out);

		getLocators().forEach(peerAddress -> addPacket(EXTRA_LOCATOR, peerAddress.getAddressAsBytes().orElseThrow(), out));

		addCrcPacket(CHECKSUM, out);

		return wrapWithBase64(out.toByteArray(), RSIdArmor.WrapMode.SLICED);
	}

	private static byte[] getPgpPublicKeyData(PGPPublicKey pgpPublicKey)
	{
		try
		{
			return pgpPublicKey.getEncoded();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
