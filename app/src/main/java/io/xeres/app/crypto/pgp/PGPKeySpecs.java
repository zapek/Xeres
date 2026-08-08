/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

import io.xeres.app.crypto.ec.Ed25519;
import io.xeres.app.crypto.rsa.RSA;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyPacket;
import org.bouncycastle.bcpg.SignaturePacket;

import java.security.KeyPair;

import static io.xeres.common.Features.EXPERIMENTAL_EC;

public final class PGPKeySpecs
{
	private PGPKeySpecs()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static KeyPair generateKeys(int size)
	{
		return EXPERIMENTAL_EC ? Ed25519.generateKeys(size) : RSA.generateKeys(size);
	}

	public static int getKeySize()
	{
		return EXPERIMENTAL_EC ? 255 : 3072;
	}

	public static String getAlgorithmName()
	{
		return EXPERIMENTAL_EC ? "EdDSA" : "RSA";
	}

	public static int getKeyPacketVersion()
	{
		return EXPERIMENTAL_EC ? PublicKeyPacket.VERSION_6 : PublicKeyPacket.VERSION_4;
	}

	public static int getKeyAlgorithm()
	{
		return EXPERIMENTAL_EC ? PublicKeyAlgorithmTags.Ed25519 : PublicKeyAlgorithmTags.RSA_GENERAL;
	}

	public static int getSignatureVersion()
	{
		return EXPERIMENTAL_EC ? SignaturePacket.VERSION_6 : SignaturePacket.VERSION_4;
	}
}
