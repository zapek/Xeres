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

package io.xeres.app.crypto.rsa;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Implements all RSA related functions. Used for creating the private and public SSL keys
 * which identify one location, also known as a machine or node.
 */
public final class RSA
{
	private static final String KEY_ALGORITHM = "RSA";
	private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

	private RSA()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Generates an RSA private/public key pair.
	 *
	 * @param size the key size (512, 1024, 2048, 3072, 4096, etc...)
	 * @return the key pair
	 */
	public static KeyPair generateKeys(int size)
	{
		try
		{
			var keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM);

			keyPairGenerator.initialize(size);

			return keyPairGenerator.generateKeyPair();
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalArgumentException("Algorithm not supported");
		}
	}

	/**
	 * Gets the RSA public key from the encoded form.
	 *
	 * @param data the public key in encoded bytes
	 * @return the public key
	 * @throws NoSuchAlgorithmException RSA algorithm unavailable
	 * @throws InvalidKeySpecException  Not an RSA key
	 */
	public static PublicKey getPublicKey(byte[] data) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(data));
	}

	/**
	 * Gets the RSA private key from the encoded form.
	 *
	 * @param data the private key in encoded bytes
	 * @return the private key
	 * @throws NoSuchAlgorithmException RSA algorithm unavailable
	 * @throws InvalidKeySpecException  Not an RSA key
	 */
	public static PrivateKey getPrivateKey(byte[] data) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		return KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(new PKCS8EncodedKeySpec(data));
	}

	/**
	 * Signs some data.
	 * @param data the data to sign
	 * @param privateKey the RSA private key
	 * @return the signature
	 */
	public static byte[] sign(byte[] data, PrivateKey privateKey)
	{
		try
		{
			var signer = Signature.getInstance(SIGNATURE_ALGORITHM);
			signer.initSign(privateKey);
			signer.update(data);
			return signer.sign();
		}
		catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Verifies signed data.
	 * @param publicKey the RSA public key
	 * @param signature the signature
	 * @param data the data to verify
	 * @return true if verification is successful
	 */
	public static boolean verify(PublicKey publicKey, byte[] signature, byte[] data)
	{
		try
		{
			var signer = Signature.getInstance(SIGNATURE_ALGORITHM);
			signer.initVerify(publicKey);
			signer.update(data);
			return signer.verify(signature);
		}
		catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Converts an RSA private key from PKCS #8 to PKCS #1
	 *
	 * @param privateKey the RSA private key
	 * @return the RSA private key in PKCS #8 format
	 * @throws IOException wrong key format
	 */
	public static byte[] getPrivateKeyAsPkcs1(PrivateKey privateKey) throws IOException
	{
		var privateKeyInfo = PrivateKeyInfo.getInstance(privateKey.getEncoded());
		var encodable = privateKeyInfo.parsePrivateKey();
		var primitive = encodable.toASN1Primitive();
		return primitive.getEncoded();
	}

	/**
	 * Converts an RSA public key from X.509 to PKCS #1
	 *
	 * @param publicKey the RSA public key
	 * @return the RSA public key in PKCS #1 format
	 * @throws IOException wrong key format
	 */
	public static byte[] getPublicKeyAsPkcs1(PublicKey publicKey) throws IOException
	{
		var subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
		var primitive = subjectPublicKeyInfo.parsePublicKey();
		return primitive.getEncoded();
	}
}
