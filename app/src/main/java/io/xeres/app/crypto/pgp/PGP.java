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

import io.xeres.app.crypto.rsa.RSA;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.jcajce.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.util.Date;
import java.util.List;

import static org.bouncycastle.bcpg.HashAlgorithmTags.SHA1;
import static org.bouncycastle.bcpg.HashAlgorithmTags.SHA512;
import static org.bouncycastle.bcpg.PublicKeyAlgorithmTags.DSA;
import static org.bouncycastle.bcpg.PublicKeyAlgorithmTags.RSA_SIGN;
import static org.bouncycastle.openpgp.PGPEncryptedData.CAST5;
import static org.bouncycastle.openpgp.PGPPublicKey.RSA_GENERAL;
import static org.bouncycastle.openpgp.PGPSignature.BINARY_DOCUMENT;
import static org.bouncycastle.openpgp.PGPSignature.DEFAULT_CERTIFICATION;
import static org.bouncycastle.openpgp.PGPUtil.SHA256;

/**
 * Utility class containing all PGP related methods.
 */
public final class PGP
{
	private PGP()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public enum Armor
	{
		NONE,
		BASE64
	}

	/**
	 * Gets the PGP public key as an armored (ASCII) key.
	 *
	 * @param pgpPublicKey the public key
	 * @param out the output stream
	 * @throws IOException if three's an I/O error
	 */
	public static void getPublicKeyArmored(PGPPublicKey pgpPublicKey, OutputStream out) throws IOException
	{
		getPublicKeyArmored(pgpPublicKey.getEncoded(true), out);
	}

	/**
	 * Gets the PGP public key as an armored (ASCII) key.
	 *
	 * @param data the public key as a byte array
	 * @param out the output stream
	 * @throws IOException if there's an I/O error
	 */
	public static void getPublicKeyArmored(byte[] data, OutputStream out) throws IOException
	{
		var aOut = new ArmoredOutputStream(out);

		var pgpObjectFactory = new PGPObjectFactory(data, new BcKeyFingerprintCalculator());

		var object = pgpObjectFactory.nextObject();

		if (object instanceof PGPPublicKeyRing pgpPublicKeyRing)
		{
			for (var publicKey : pgpPublicKeyRing)
			{
				publicKey.encode(aOut);
				aOut.close();
			}
		}
		else
		{
			throw new IllegalArgumentException("Wrong encoded key structure: " + object.getClass().getCanonicalName());
		}
	}

	/**
	 * Gets the PGP secret key. While a secret key needs a password to be converted to a private
	 * key, this implementation uses an empty password.
	 *
	 * @param data a byte array containing the raw PGP key
	 * @return the {@link PGPSecretKey}
	 * @throws IllegalArgumentException if the key is wrong
	 */
	public static PGPSecretKey getPGPSecretKey(byte[] data)
	{
		var pgpObjectFactory = new PGPObjectFactory(data, new BcKeyFingerprintCalculator());

		try
		{
			var object = pgpObjectFactory.nextObject();

			if (object instanceof PGPSecretKeyRing pgpSecretKeyRing)
			{
				if (!pgpSecretKeyRing.iterator().hasNext())
				{
					throw new IllegalArgumentException("PGPSecretKeyRing is empty");
				}
				return pgpSecretKeyRing.iterator().next();
			}
			else
			{
				throw new IllegalArgumentException("PGPSecretKeyRing expected, got: " + object.getClass().getCanonicalName() + " instead");
			}
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException("PGPSecretKeyRing is corrupted", e);
		}
	}

	/**
	 * Gets the PGP public key.
	 *
	 * @param data a byte array containing the raw PGP key
	 * @return the {@link PGPPublicKey}
	 * @throws InvalidKeyException if the key is wrong
	 */
	public static PGPPublicKey getPGPPublicKey(byte[] data) throws InvalidKeyException
	{
		var pgpObjectFactory = new PGPObjectFactory(data, new BcKeyFingerprintCalculator());

		try
		{
			var object = pgpObjectFactory.nextObject();

			if (object instanceof PGPPublicKeyRing pgpPublicKeyRing)
			{
				if (!pgpPublicKeyRing.iterator().hasNext())
				{
					throw new InvalidKeyException("PGPPublicKeyRing is empty");
				}
				return pgpPublicKeyRing.iterator().next();
			}
			else
			{
				throw new InvalidKeyException("PGPPublicKeyRing expected, got: " + object.getClass().getCanonicalName() + " instead");
			}
		}
		catch (IOException e)
		{
			throw new InvalidKeyException("PGPPublicKeyRing is corrupted", e);
		}
	}

	/**
	 * Generates a PGP secret key.
	 * <p>
	 * The key is a PGP <b>V4</b> format, <b>RSA</b> key with a <b>default certification</b>,
	 * <b>SHA-1</b> integrity checksum and encrypted with <b>CAST5</b>. The packet sizes are encoded using the original format.
	 * <p>
	 * This is the most compatible PGP key, yet considered secure as of 2020. Do not attempt to change it.
	 *
	 * @param id     the id of the key
	 * @param suffix the suffix appended to the id
	 * @param size   the size of the key
	 * @return the {@link PGPSecretKey}
	 * @throws PGPException if somehow the PGP key generation failed (for example, wrong key size)
	 */
	public static PGPSecretKey generateSecretKey(String id, String suffix, int size) throws PGPException
	{
		var keyPair = RSA.generateKeys(size);

		var sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(SHA1);

		PGPKeyPair pgpKeyPair = new JcaPGPKeyPair(RSA_GENERAL, keyPair, new Date());

		return new PGPSecretKey(DEFAULT_CERTIFICATION, pgpKeyPair, suffix != null ? (id + " " + suffix) : id, sha1Calc, null, null,
				new JcaPGPContentSignerBuilder(pgpKeyPair.getPublicKey().getAlgorithm(), SHA256),
				new JcePBESecretKeyEncryptorBuilder(CAST5, sha1Calc)
						.setProvider("BC").build("".toCharArray()));
	}

	/**
	 * Signs a message as a <b>binary document</b> using <b>SHA-256</b>.
	 *
	 * @param pgpSecretKey the secret key to sign the message with
	 * @param in           the message
	 * @param out          the resulting PGP signature
	 * @param armor        optional ASCII armoring (base 64 encoding)
	 * @throws PGPException if there's a PGP error
	 * @throws IOException  if there's an I/O error
	 */
	public static void sign(PGPSecretKey pgpSecretKey, InputStream in, OutputStream out, Armor armor) throws PGPException, IOException
	{
		if (armor == Armor.BASE64)
		{
			out = new ArmoredOutputStream(out);
		}

		var pgpPrivateKey = pgpSecretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build("".toCharArray()));

		var signatureGenerator = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(pgpSecretKey.getPublicKey().getAlgorithm(), SHA256));

		signatureGenerator.init(BINARY_DOCUMENT, pgpPrivateKey);

		var bOut = new BCPGOutputStream(out);

		signatureGenerator.update(in.readAllBytes());
		in.close();

		signatureGenerator.generate().encode(bOut);

		if (armor == Armor.BASE64)
		{
			out.close();
		}
	}

	/**
	 * Verifies a PGP signature.
	 * <p>
	 * Note that only a handful of algorithms are supported.
	 *
	 * @param pgpPublicKey the public key corresponding to the private key used to generate the signature
	 * @param signature    the signature
	 * @param in           the message
	 * @throws SignatureException if the message verification failed
	 * @throws IOException        if there's an I/O error
	 * @throws PGPException       if there's a PGP error
	 */
	public static void verify(PGPPublicKey pgpPublicKey, byte[] signature, InputStream in) throws IOException, SignatureException, PGPException
	{
		var pgpSignature = getSignature(signature);

		pgpSignature.init(new BcPGPContentVerifierBuilderProvider(), pgpPublicKey);
		pgpSignature.update(in.readAllBytes());
		in.close();
		if (!pgpSignature.verify())
		{
			throw new SignatureException("Wrong signature");
		}
	}

	public static long getIssuer(byte[] signature)
	{
		try
		{
			var pgpSignature = getSignature(signature);
			return pgpSignature.getKeyID();
		}
		catch (SignatureException | IOException e)
		{
			return 0L;
		}
	}

	private static PGPSignature getSignature(byte[] signature) throws SignatureException, IOException
	{
		var pgpObjectFactory = new PGPObjectFactory(signature, new BcKeyFingerprintCalculator());

		var object = pgpObjectFactory.nextObject();
		if (!(object instanceof PGPSignatureList pgpSignatures))
		{
			throw new SignatureException("Signature doesn't contain a PGP signature list");
		}
		if (pgpSignatures.isEmpty())
		{
			throw new SignatureException("Signature list empty");
		}

		var pgpSignature = pgpSignatures.get(0);

		if (pgpSignature.getSignatureType() != BINARY_DOCUMENT)
		{
			throw new SignatureException("Signature is not of BINARY_DOCUMENT (" + pgpSignature.getSignatureType() + ")");
		}

		if (pgpSignature.getVersion() != 4)
		{
			throw new SignatureException("Signature is not PGP version 4 (" + pgpSignature.getVersion() + ")");
		}

		if (!List.of(RSA_GENERAL, RSA_SIGN, DSA).contains(pgpSignature.getKeyAlgorithm()))
		{
			throw new SignatureException("Signature key algorithm is not of RSA or DSA (" + pgpSignature.getSignatureType() + ")");
		}

		if (!List.of(SHA1, SHA256, SHA512).contains(pgpSignature.getHashAlgorithm()))
		{
			throw new SignatureException("Signature hash algorithm is not of SHA family (" + pgpSignature.getHashAlgorithm() + ")");
		}
		return pgpSignature;
	}

	/**
	 * Gets the PGP identifier, which is the last long of the PGP fingerprint
	 *
	 * @return the PGP identifier
	 */
	public static long getPGPIdentifierFromFingerprint(byte[] fingerprint)
	{
		var buf = ByteBuffer.allocate(Long.BYTES);
		buf.put(fingerprint, 12, 8);
		buf.flip();
		return buf.getLong();
	}
}
