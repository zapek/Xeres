/*
 * Copyright (c) 2019-2020 by David Gerber - https://zapek.com
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

package io.xeres.app.crypto.pgp;

import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.operator.ContentSigner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static io.xeres.app.crypto.pgp.PGP.Armor;
import static io.xeres.app.crypto.pgp.PGP.sign;

public class PGPSigner implements ContentSigner
{
	private final ByteArrayOutputStream outputStream;
	private final PGPSecretKey pgpSecretKey;

	public PGPSigner(PGPSecretKey pgpSecretKey)
	{
		this.pgpSecretKey = pgpSecretKey;
		outputStream = new ByteArrayOutputStream();
	}

	@Override
	public AlgorithmIdentifier getAlgorithmIdentifier()
	{
		return new AlgorithmIdentifier(PKCSObjectIdentifiers.sha256WithRSAEncryption);
	}

	@Override
	public OutputStream getOutputStream()
	{
		return outputStream;
	}

	@Override
	public byte[] getSignature()
	{
		try (var out = new ByteArrayOutputStream())
		{
			sign(pgpSecretKey, new ByteArrayInputStream(outputStream.toByteArray()), out, Armor.NONE);
			outputStream.close();

			return out.toByteArray();
		}
		catch (PGPException | IOException e)
		{
			throw new IllegalStateException("Failed to sign certificate: " + e.getMessage(), e.getCause());
		}
	}
}
