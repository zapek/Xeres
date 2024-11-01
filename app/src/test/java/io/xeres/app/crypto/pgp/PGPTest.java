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

package io.xeres.app.crypto.pgp;

import io.xeres.testutils.TestUtils;
import org.bouncycastle.bcpg.ArmoredInputStream;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Security;
import java.security.SignatureException;

import static io.xeres.app.crypto.pgp.PGP.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class PGPTest
{
	private static final int KEY_SIZE = 512;
	private static PGPSecretKey pgpSecretKey;

	@BeforeAll
	static void setup() throws PGPException
	{
		Security.addProvider(new BouncyCastleProvider());

		pgpSecretKey = generateSecretKey("test", null, KEY_SIZE);
	}

	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(PGP.class);
	}

	/**
	 * Generates a PGP secret key.
	 */
	@Test
	void GenerateSecretKey_Success() throws PGPException
	{
		assertNotNull(pgpSecretKey);
		assertTrue(pgpSecretKey.isMasterKey());
		assertTrue(pgpSecretKey.isSigningKey());
		assertFalse(pgpSecretKey.isPrivateKeyEmpty());
		assertEquals(SymmetricKeyAlgorithmTags.AES_128, pgpSecretKey.getKeyEncryptionAlgorithm());
		assertNotNull(pgpSecretKey.getPublicKey());
		assertNotNull(pgpSecretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build("".toCharArray())));
	}

	/**
	 * Signs using a PGP secret key then verifies.
	 */
	@Test
	void Sign_Success() throws PGPException, IOException, SignatureException
	{
		var in = "The lazy dog jumps over the drunk fox".getBytes();

		var out = new ByteArrayOutputStream();

		sign(pgpSecretKey, new ByteArrayInputStream(in), out, Armor.NONE);

		verify(pgpSecretKey.getPublicKey(), out.toByteArray(), new ByteArrayInputStream(in));
	}

	@Test
	void Sign_Armored_Success() throws PGPException, IOException, SignatureException
	{
		var in = "The lazy dog jumps over the drunk fox".getBytes();

		var out = new ByteArrayOutputStream();

		sign(pgpSecretKey, new ByteArrayInputStream(in), out, Armor.BASE64);

		verify(pgpSecretKey.getPublicKey(), new ArmoredInputStream(new ByteArrayInputStream(out.toByteArray())).readAllBytes(), new ByteArrayInputStream(in));
	}

	/**
	 * Signs using a PGP secret key then verifies with another.
	 */
	@Test
	void Sign_WrongKey_Failure() throws PGPException, IOException
	{
		var in = "The lazy dog jumps over the drunk fox".getBytes();

		var pgpSecretKey2 = generateSecretKey("test2", null, KEY_SIZE);

		var out = new ByteArrayOutputStream();

		sign(pgpSecretKey, new ByteArrayInputStream(in), out, Armor.NONE);

		assertThatThrownBy(() -> verify(pgpSecretKey2.getPublicKey(), out.toByteArray(), new ByteArrayInputStream(in)))
				.isInstanceOf(SignatureException.class);
	}

	@Test
	void GetSecretKey_Success() throws IOException
	{
		assertEquals(pgpSecretKey.getKeyID(), getPGPSecretKey(pgpSecretKey.getEncoded()).getKeyID());
	}

	@Test
	void GetSecretKey_Corrupted_Failure()
	{
		assertThatThrownBy(() -> getPGPSecretKey(new byte[]{1, 2, 3}))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("corrupted");
	}

	@Test
	void GetPublicKey_Success() throws IOException, InvalidKeyException
	{
		assertEquals(pgpSecretKey.getPublicKey().getKeyID(), getPGPPublicKey(pgpSecretKey.getPublicKey().getEncoded()).getKeyID());
	}

	@Test
	void GetPublicKey_Corrupted_Failure()
	{
		assertThatThrownBy(() -> getPGPPublicKey(new byte[]{1, 2, 3}))
				.isInstanceOf(InvalidKeyException.class)
				.hasMessageContaining("corrupted");
	}

	@Test
	void GetPublicKeyArmored_Success() throws IOException
	{
		var out = new ByteArrayOutputStream();
		getPublicKeyArmored(pgpSecretKey.getPublicKey(), out);

		var output = out.toString();

		assertTrue(output.contains("BEGIN PGP"));
		assertTrue(output.contains("END PGP"));
	}
}
