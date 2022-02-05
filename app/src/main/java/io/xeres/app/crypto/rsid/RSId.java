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
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.ProfileFingerprint;
import io.xeres.common.rsid.Type;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openpgp.PGPPublicKey;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.CertificateParsingException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.xeres.common.rsid.Type.*;

/**
 * This abstract class represents an RS ID, which is a string that allows to exchange a profile identity
 * with another user.
 */
public abstract class RSId
{
	private static final Map<Class<? extends RSId>, Type> engines = Map.of(
			ShortInvite.class, SHORT_INVITE,
			RSCertificate.class, CERTIFICATE);

	/**
	 * Parses an ID.
	 *
	 * @param data the ID encoded in a string
	 * @param type restrict the type to parse or use ANY
	 * @return an RSId
	 */
	public static Optional<RSId> parse(String data, Type type)
	{
		if (StringUtils.isBlank(data))
		{
			return Optional.empty();
		}

		for (var entry : engines.entrySet())
		{
			Class<? extends RSId> engineClass = entry.getKey();
			Type engineType = entry.getValue();

			if (type != ANY && type != engineType)
			{
				continue;
			}

			try
			{
				RSId rsId = engineClass.getDeclaredConstructor().newInstance();
				rsId.parseInternal(data);
				rsId.checkRequiredFields();
				return Optional.of(rsId);
			}
			catch (NoSuchMethodException e)
			{
				throw new IllegalArgumentException(engineClass.getSimpleName() + " requires an empty constructor");
			}
			catch (InvocationTargetException | InstantiationException | IllegalAccessException e)
			{
				throw new RuntimeException(e);
			}
			catch (CertificateParsingException ignored)
			{
				// Parsing failed, try the next one
			}
		}
		return Optional.empty();
	}

	abstract void parseInternal(String data) throws CertificateParsingException;

	abstract void checkRequiredFields();

	/**
	 * Gets the internal IP (IP used on the LAN).
	 *
	 * @return the internal IP (for example 192.168.1.10)
	 */
	public abstract Optional<PeerAddress> getInternalIp();

	/**
	 * Gets the external IP (IP used on the Internet).
	 *
	 * @return the external IP (for example 85.12.43.18)
	 */
	public abstract Optional<PeerAddress> getExternalIp();

	/**
	 * Gets the PGP fingerprint. Should always be available.
	 *
	 * @return the PGP fingerprint
	 */
	public abstract ProfileFingerprint getPgpFingerprint();

	/**
	 * Gets the PGP public key (optional).
	 *
	 * @return the PGP public key
	 */
	public abstract Optional<PGPPublicKey> getPgpPublicKey();

	/**
	 * Gets the profile name (usually the name or nickname of the user).
	 *
	 * @return the profile name
	 */
	public abstract String getName();

	/**
	 * Gets the location ID (node identifier).
	 *
	 * @return the location ID
	 */
	public abstract LocationId getLocationId();

	/**
	 * Gets the DNS name.
	 *
	 * @return the DNS name
	 */
	public abstract Optional<PeerAddress> getDnsName();

	/**
	 * Gets the hidden node address, if this is a hidden node.
	 *
	 * @return the hidden node address
	 */
	public abstract Optional<PeerAddress> getHiddenNodeAddress();

	/**
	 * Gets a set of addresses where the node is available.
	 *
	 * @return a set of addresses
	 */
	public abstract Set<PeerAddress> getLocators();

	/**
	 * Gets an armored version of the certificate or short invite. It's encoded using base64 and can be
	 * used in emails, forums, etc...
	 *
	 * @return an ASCII armored version of it
	 */
	public abstract String getArmored();

	/**
	 * Gets the PGP identifier, which is the last long of the PGP fingerprint
	 *
	 * @return the PGP identifier
	 */
	public Long getPgpIdentifier()
	{
		if (getPgpFingerprint() == null)
		{
			return null;
		}
		return PGP.getPGPIdentifierFromFingerprint(getPgpFingerprint().getBytes());
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
