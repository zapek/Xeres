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

import io.xeres.common.id.GxsId;
import io.xeres.common.id.Sha1Sum;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

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
	 *
	 * @param data       the data to sign
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
	 *
	 * @param publicKey the RSA public key
	 * @param signature the signature
	 * @param data      the data to verify
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
	 * Converts a PKCS #1 byte array to an RSA private key
	 *
	 * @param data the DER encoded PKCS #1 byte array
	 * @return an RSA private key
	 * @throws IOException              wrong key format
	 * @throws NoSuchAlgorithmException wrong key format
	 * @throws InvalidKeySpecException  wrong encoding
	 */
	public static PrivateKey getPrivateKeyFromPkcs1(byte[] data) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
	{
		//noinspection resource
		var asn1InputStream = new ASN1InputStream(data);
		var asn1Primitive = asn1InputStream.readObject();
		var algorithmIdentifier = new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE);
		var privateKeyInfo = new PrivateKeyInfo(algorithmIdentifier, asn1Primitive);
		return getPrivateKey(privateKeyInfo.getEncoded());
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

	/**
	 * Converts a PKCS #1 byte array to an RSA public key.
	 *
	 * @param data the DER encoded PKCS #1 byte array
	 * @return an RSA public key
	 * @throws IOException              wrong key format
	 * @throws NoSuchAlgorithmException wrong key format
	 * @throws InvalidKeySpecException  wrong encoding
	 */
	public static PublicKey getPublicKeyFromPkcs1(byte[] data) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
	{
		var algorithmIdentifier = new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE);
		var subjectPublicKeyInfo = new SubjectPublicKeyInfo(algorithmIdentifier, data);
		return getPublicKey(subjectPublicKeyInfo.getEncoded());
	}

	public static GxsId getGxsId(Key key)
	{
		if (key instanceof PrivateKey privateKey)
		{
			var rsaPrivateKey = (RSAPrivateKey) privateKey;
			return makeGxsId(
					getAsOneComplement(rsaPrivateKey.getModulus()),
					getAsOneComplement(rsaPrivateKey.getPrivateExponent())
			);
		}
		else if (key instanceof PublicKey publicKey)
		{
			var rsaPublicKey = (RSAPublicKey) publicKey;
			return makeGxsId(
					getAsOneComplement(rsaPublicKey.getModulus()),
					getAsOneComplement(rsaPublicKey.getPublicExponent())
			);
		}
		throw new IllegalArgumentException("Cannot extract GxsId from key " + key);
	}

	private static byte[] getAsOneComplement(BigInteger number)
	{
		var array = number.toByteArray();
		if (array[0] == 0)
		{
			array = Arrays.copyOfRange(array, 1, array.length);
		}
		return array;
	}

	private static GxsId makeGxsId(byte[] modulus, byte[] exponent)
	{
		var sha1sum = new byte[Sha1Sum.LENGTH];

		Digest digest = new SHA1Digest();
		digest.update(modulus, 0, modulus.length);
		digest.update(exponent, 0, exponent.length);
		digest.doFinal(sha1sum, 0);

		// Copy the first 16 bytes of the sha1 sum to get the GxsId
		return new GxsId(Arrays.copyOfRange(sha1sum, 0, GxsId.LENGTH));
	}
}
