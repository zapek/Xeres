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
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.common.id.LocationId;
import io.xeres.common.id.ProfileFingerprint;
import io.xeres.common.rsid.Type;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.CertificateParsingException;
import java.util.LinkedHashMap;
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
	private static final Logger log = LoggerFactory.getLogger(RSId.class);

	private static final Map<Class<? extends RSId>, Type> engines = LinkedHashMap.newLinkedHashMap(2);

	static
	{
		engines.put(ShortInvite.class, SHORT_INVITE);
		engines.put(RSCertificate.class, CERTIFICATE);
	}

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

		String error = null;

		for (var entry : engines.entrySet())
		{
			var engineClass = entry.getKey();
			var engineType = entry.getValue();

			if (type != ANY && type != engineType)
			{
				continue;
			}

			try
			{
				var rsId = engineClass.getDeclaredConstructor().newInstance();
				rsId.parseInternal(data);
				rsId.checkRequiredFieldsAndThrow();
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
			catch (CertificateParsingException e)
			{
				// Parsing failed, try the next one
				error = e.getMessage();
			}
		}
		log.debug("RSId parsing error: {}", error);
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
			for (var b : data)
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
		var octet1 = in.read();

		if (octet1 < 192) // size is coded in one byte
		{
			return octet1;
		}
		else if (octet1 < 224) // size is coded in 2 bytes
		{
			var octet2 = in.read();

			return ((octet1 - 192) << 8) + octet2 + 192;
		}
		else
		{
			throw new IllegalArgumentException("Unsupported packet data size");
		}
	}

	private void checkRequiredFieldsAndThrow() throws CertificateParsingException
	{
		try
		{
			checkRequiredFields();
		}
		catch (IllegalArgumentException e)
		{
			throw new CertificateParsingException("Required field error: " + e.getMessage(), e);
		}
	}
}
