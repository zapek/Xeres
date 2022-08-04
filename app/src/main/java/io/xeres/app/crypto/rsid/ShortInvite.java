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
import io.xeres.common.id.ProfileFingerprint;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateParsingException;
import java.util.*;

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

	private ProfileFingerprint pgpFingerprint;
	private PeerAddress hiddenLocator;
	private PeerAddress ext4Locator;
	private PeerAddress loc4Locator;
	private PeerAddress hostnameLocator;
	private final Set<PeerAddress> locators = new HashSet<>();

	ShortInvite()
	{
	}

	@SuppressWarnings("DuplicatedCode")
	@Override
	void parseInternal(String data) throws CertificateParsingException
	{
		try
		{
			var shortInviteBytes = Base64.getDecoder().decode(cleanupInput(data.getBytes()));
			var checksum = RSIdCrc.calculate24bitsCrc(shortInviteBytes, shortInviteBytes.length - 5); // ignore the checksum PTAG which is 5 bytes in total and at the end
			var in = new ByteArrayInputStream(shortInviteBytes);
			Boolean checksumPassed = null;

			while (in.available() > 0)
			{
				var ptag = in.read();
				var size = getPacketSize(in);
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
					case PGP_FINGERPRINT -> setPgpFingerprint(buf);
					case NAME -> setName(buf);
					case SSL_ID -> setLocationId(new LocationId(buf));
					case DNS_LOCATOR -> setDnsName(buf);
					case HIDDEN_LOCATOR -> setHiddenNodeAddress(buf);
					case EXT4_LOCATOR -> setExt4Locator(buf);
					case LOC4_LOCATOR -> setLoc4Locator(buf);
					case LOCATOR -> addLocator(new String(buf));
					case CHECKSUM -> {
						if (buf.length != 3)
						{
							throw new IllegalArgumentException("Checksum corrupted");
						}
						checksumPassed = checksum == (Byte.toUnsignedInt(buf[2]) << 16 | Byte.toUnsignedInt(buf[1]) << 8 | Byte.toUnsignedInt(buf[0])); // little endian
					}
					default -> log.warn("Unhandled tag {}, ignoring.", ptag);
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
		if (getPgpFingerprint() == null)
		{
			throw new IllegalArgumentException("Missing PGP fingerprint");
		}
	}

	void setExt4Locator(byte[] data)
	{
		ext4Locator = PeerAddress.fromByteArray(swapBytes(data));
	}

	void setExt4Locator(String ipAndPort)
	{
		ext4Locator = PeerAddress.fromIpAndPort(ipAndPort);
	}

	void setLoc4Locator(byte[] data)
	{
		loc4Locator = PeerAddress.fromByteArray(swapBytes(data));
	}

	void setLoc4Locator(String ipAndPort)
	{
		loc4Locator = PeerAddress.fromIpAndPort(ipAndPort);
	}

	@Override
	public Optional<PeerAddress> getInternalIp()
	{
		if (loc4Locator != null && loc4Locator.isValid())
		{
			return Optional.of(loc4Locator);
		}
		return Optional.empty();
	}

	@Override
	public Optional<PeerAddress> getExternalIp()
	{
		if (ext4Locator != null && ext4Locator.isValid())
		{
			return Optional.of(ext4Locator);
		}
		return Optional.empty();
	}

	void setPgpFingerprint(byte[] pgpFingerprint)
	{
		this.pgpFingerprint = new ProfileFingerprint(pgpFingerprint);
	}

	@Override
	public ProfileFingerprint getPgpFingerprint()
	{
		return pgpFingerprint;
	}

	@Override
	public Optional<PGPPublicKey> getPgpPublicKey()
	{
		return Optional.empty();
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

	void setLocationId(LocationId locationId)
	{
		this.locationId = locationId;
	}

	@Override
	public Optional<PeerAddress> getDnsName()
	{
		return Optional.ofNullable(hostnameLocator);
	}

	void setDnsName(String dnsName)
	{
		hostnameLocator = PeerAddress.fromHostnameAndPort(dnsName);
	}

	private void setDnsName(byte[] portAndDns)
	{
		if (portAndDns == null || portAndDns.length <= 3 || portAndDns.length > 255)
		{
			throw new IllegalArgumentException("DNS name format is wrong");
		}

		var port = Byte.toUnsignedInt(portAndDns[0]) << 8 | Byte.toUnsignedInt(portAndDns[1]);
		var hostname = new String(Arrays.copyOfRange(portAndDns, 2, portAndDns.length), StandardCharsets.US_ASCII);
		hostnameLocator = PeerAddress.fromHostname(hostname, port);
	}

	@Override
	public Optional<PeerAddress> getHiddenNodeAddress()
	{
		if (hiddenLocator != null && hiddenLocator.isValid())
		{
			return Optional.of(hiddenLocator);
		}
		return Optional.empty();
	}

	private void setHiddenNodeAddress(String hiddenNodeAddress)
	{
		this.hiddenLocator = PeerAddress.fromHidden(hiddenNodeAddress);
	}

	private void setHiddenNodeAddress(byte[] hiddenNodeAddress)
	{
		if (hiddenNodeAddress != null && hiddenNodeAddress.length >= 11 && hiddenNodeAddress.length <= 255)
		{
			var port = Byte.toUnsignedInt(hiddenNodeAddress[4]) << 8 | Byte.toUnsignedInt(hiddenNodeAddress[5]);
			setHiddenNodeAddress(new String(Arrays.copyOfRange(hiddenNodeAddress, 6, hiddenNodeAddress.length), StandardCharsets.US_ASCII) + ":" + port);
		}
		else
		{
			this.hiddenLocator = PeerAddress.fromInvalid();
		}
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

		addPacket(SSL_ID, getLocationId().getBytes(), out);
		addPacket(NAME, getName().getBytes(), out);
		addPacket(PGP_FINGERPRINT, getPgpFingerprint().getBytes(), out);
		if (getHiddenNodeAddress().isPresent())
		{
			addPacket(HIDDEN_LOCATOR, getHiddenNodeAddress().get().getAddressAsBytes().orElseThrow(), out);
		}
		else
		{
			getDnsName().ifPresent(peerAddress -> addPacket(DNS_LOCATOR, swapDnsBytes(peerAddress.getAddressAsBytes().orElseThrow()), out));
			getExternalIp().ifPresent(peerAddress -> addPacket(EXT4_LOCATOR, swapBytes(peerAddress.getAddressAsBytes().orElseThrow()), out));
			getInternalIp().ifPresent(peerAddress -> addPacket(LOC4_LOCATOR, swapBytes(peerAddress.getAddressAsBytes().orElseThrow()), out));
			// Use one locator. Ideally, the first one should be the most recent address
			getLocators().stream()
					.findFirst()
					.ifPresent(peerAddress -> addPacket(LOCATOR, peerAddress.getUrl().getBytes(StandardCharsets.US_ASCII), out));
		}
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
