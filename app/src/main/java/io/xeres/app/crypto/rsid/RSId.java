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

package io.xeres.app.crypto.rsid;

import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.crypto.rsid.certificate.RSCertificate;
import io.xeres.app.crypto.rsid.shortinvite.ShortInvite;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.common.id.LocationId;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openpgp.PGPPublicKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateParsingException;
import java.util.Set;

public abstract class RSId
{
	public enum Type
	{
		SHORT_INVITE,
		BOTH
	}

	public static RSId parse(String data) throws CertificateParsingException
	{
		if (StringUtils.isBlank(data))
		{
			throw new CertificateParsingException("Empty input");
		}

		try
		{
			return ShortInvite.parseShortInvite(data);
		}
		catch (CertificateParsingException e)
		{
			// XXX: this is not very nice... how do I know which parsing failed?
			return RSCertificate.parseRSCertificate(data);
		}
	}

	// XXX: the names need to be adjusted...

	public abstract boolean hasInternalIp();

	public abstract PeerAddress getInternalIp();

	public abstract boolean hasExternalIp();

	public abstract PeerAddress getExternalIp();

	public abstract boolean hasPgpPublicKey();

	public abstract PGPPublicKey getPgpPublicKey();

	public abstract byte[] getPgpFingerprint();

	public abstract boolean hasName();

	public abstract String getName();

	public abstract boolean hasLocationInfo();

	public abstract LocationId getLocationId();

	public abstract boolean hasDnsName();

	public abstract PeerAddress getDnsName();

	public abstract boolean isHiddenNode();

	public abstract PeerAddress getHiddenNodeAddress();

	public abstract boolean hasLocators();

	public abstract Set<String> getLocators();

	/**
	 * Gets the PGP identifier, which is the last long of the PGP fingerprint
	 *
	 * @return the PGP identifier
	 */
	public Long getPgpIdentifier()
	{
		byte[] bytes = getPgpFingerprint();

		if (bytes == null)
		{
			return null;
		}
		return PGP.getPGPIdentifierFromFingerprint(bytes);
	}

	protected static byte[] cleanupInput(byte[] data)
	{
		try (var out = new ByteArrayOutputStream())
		{
			for (byte b : data)
			{
				if (b == ' ' || b == '\n' || b == '\t' || b == '\r')
				{
					continue;
				}
				out.write(b);
			}
			return out.toByteArray();
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}

	protected static int getPacketSize(InputStream in) throws IOException
	{
		int octet1 = in.read();

		if (octet1 < 192) // size is coded in one byte
		{
			return octet1;
		}
		else if (octet1 < 224) // size is coded in 2 bytes
		{
			int octet2 = in.read();

			return ((octet1 - 192) << 8) + octet2 + 192;
		}
		else
		{
			throw new IllegalArgumentException("Unsupported packet data size");
		}
	}
}
