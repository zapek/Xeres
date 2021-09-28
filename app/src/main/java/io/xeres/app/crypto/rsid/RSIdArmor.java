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

import io.xeres.app.crypto.rsid.certificate.RSCertificate;
import io.xeres.app.crypto.rsid.certificate.RSCertificateTags;
import io.xeres.app.crypto.rsid.shortinvite.ShortInvite;
import io.xeres.app.crypto.rsid.shortinvite.ShortInviteQuirks;
import io.xeres.app.crypto.rsid.shortinvite.ShortInviteTags;
import io.xeres.common.id.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class RSIdArmor
{
	private static final Logger log = LoggerFactory.getLogger(RSIdArmor.class);

	private enum WrapMode
	{
		CONTINUOUS,
		SLICED
	}

	private RSIdArmor()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Gets an armored version of the certificate or short invite. It's encoded using base64 and can be
	 * used in emails, forums, etc...
	 *
	 * @param rsId the RSId
	 * @return an ascii armored version of it
	 * @throws IOException I/O error
	 */
	public static String getArmored(RSId rsId) throws IOException
	{
		try (var out = new ByteArrayOutputStream())
		{
			if (rsId instanceof RSCertificate)
			{
				return getArmoredCertificate(rsId, out);
			}
			else if (rsId instanceof ShortInvite)
			{
				return getArmoredShortInvite(rsId, out);
			}
			else
			{
				throw new UnsupportedOperationException("Armor mode not implemented");
			}
		}
	}

	private static String getArmoredShortInvite(RSId rsId, ByteArrayOutputStream out) throws IOException
	{
		addPacket(ShortInviteTags.SSLID, rsId.getLocationId(), out);
		addPacket(ShortInviteTags.NAME, rsId.getName().getBytes(), out);
		addPacket(ShortInviteTags.PGP_FINGERPRINT, rsId.getPgpFingerprint(), out);
		if (rsId.isHiddenNode())
		{
			addPacket(ShortInviteTags.HIDDEN_LOCATOR, rsId.getHiddenNodeAddress().getAddressAsBytes().orElseThrow(), out);
		}
		else if (rsId.hasDnsName())
		{
			addPacket(ShortInviteTags.DNS_LOCATOR, rsId.getDnsName().getAddressAsBytes().orElseThrow(), out);
		}
		else if (rsId.hasExternalIp())
		{
			addPacket(ShortInviteTags.EXT4_LOCATOR, ShortInviteQuirks.swapBytes(rsId.getExternalIp().getAddressAsBytes().orElseThrow()), out);
		}
		else if (rsId.hasLocators())
		{
			// XXX: use ONE most recently known locator. I still think the url scheme is a waste
		}
		addCrcPacket(ShortInviteTags.CHECKSUM, out);

		return wrapWithBase64(out.toByteArray(), WrapMode.CONTINUOUS);
	}

	private static String getArmoredCertificate(RSId rsId, ByteArrayOutputStream out) throws IOException
	{
		addPacket(RSCertificateTags.VERSION, new byte[]{RSCertificate.VERSION_06}, out);
		if (rsId.hasPgpPublicKey())
		{
			addPacket(RSCertificateTags.PGP, rsId.getPgpPublicKey().getEncoded(), out);
		}

		if (rsId.hasLocationInfo())
		{
			if (rsId.isHiddenNode())
			{
				addPacket(RSCertificateTags.HIDDEN_NODE, rsId.getHiddenNodeAddress().getAddressAsBytes().orElseThrow(), out);
			}
			else
			{
				if (rsId.hasExternalIp())
				{
					addPacket(RSCertificateTags.EXTERNAL_IP_AND_PORT, rsId.getExternalIp().getAddressAsBytes().orElseThrow(), out);
				}
				if (rsId.hasInternalIp())
				{
					addPacket(RSCertificateTags.INTERNAL_IP_AND_PORT, rsId.getInternalIp().getAddressAsBytes().orElseThrow(), out);
				}
				//if (rsId.hasDnsName())
				//{
				//addPacket(DNS, rsId.getDnsName().getBytes(), out);
				//}
			}

			if (rsId.hasName())
			{
				addPacket(RSCertificateTags.NAME, rsId.getName().getBytes(), out);
			}
			addPacket(RSCertificateTags.SSLID, rsId.getLocationId(), out);

			if (rsId.hasLocators())
			{
				for (String locator : rsId.getLocators())
				{
					addPacket(RSCertificateTags.EXTRA_LOCATOR, locator.getBytes(), out);
				}
			}
		}
		addCrcPacket(RSCertificateTags.CHECKSUM, out);

		return wrapWithBase64(out.toByteArray(), WrapMode.SLICED);
	}

	private static void addPacket(int pTag, Identifier identifier, OutputStream out) throws IOException
	{
		addPacket(pTag, identifier.getBytes(), out);
	}

	private static void addPacket(int pTag, byte[] data, OutputStream out) throws IOException
	{
		if (data != null)
		{
			// This is like PGP packets, see https://tools.ietf.org/html/rfc4880
			out.write(pTag);
			if (data.length < 192) // size is coded in one byte
			{
				// one byte
				out.write(data.length);
			}
			else if (data.length < 8384) // size is coded in 2 bytes
			{
				int octet2 = (data.length - 192) & 0xff;
				out.write(((data.length - 192 - octet2) >> 8) + 192);
				out.write(octet2);
			}
			else
			{
				// We don't support more as it makes little sense to have an oversized certificate
				throw new IllegalArgumentException("Packet data size too big: " + data.length);
			}
			out.write(data);
		}
		else
		{
			log.warn("Trying to write certificate tag {} with empty data. Skipping...", pTag);
		}
	}

	private static void addCrcPacket(int pTag, ByteArrayOutputStream out) throws IOException
	{
		byte[] data = out.toByteArray();

		int crc = RSIdCrc.calculate24bitsCrc(data, data.length);

		// Perform byte swapping
		var le = new byte[3];
		le[0] = (byte) (crc & 0xff);
		le[1] = (byte) ((crc >> 8) & 0xff);
		le[2] = (byte) ((crc >> 16) & 0xff);

		addPacket(pTag, le, out);
	}

	private static String wrapWithBase64(byte[] data, WrapMode wrapMode)
	{
		byte[] base64 = Base64.getEncoder().encode(data);

		try (var out = new ByteArrayOutputStream())
		{
			for (var i = 0; i < base64.length; i++)
			{
				out.write(base64[i]);

				if (wrapMode == WrapMode.SLICED && i % 64 == 64 - 1)
				{
					out.write('\n');
				}
			}
			return out.toString(StandardCharsets.US_ASCII);
		}
		catch (IOException e)
		{
			throw new IllegalStateException(e);
		}
	}
}
