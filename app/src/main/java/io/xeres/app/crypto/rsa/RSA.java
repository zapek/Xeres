/*
 * Copyright (c) 2019-2025 by David Gerber - https://zapek.com
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

import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.common.id.GxsId;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.util.BigIntegers;

import java.io.IOException;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Objects;

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
	 * @throws NoSuchAlgorithmException if the RSA algorithm is unavailable
	 * @throws InvalidKeySpecException  if it's not an RSA key
	 */
	public static PublicKey getPublicKey(byte[] data) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		Objects.requireNonNull(data);
		return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(new X509EncodedKeySpec(data));
	}

	/**
	 * Gets the RSA private key from the encoded form.
	 *
	 * @param data the private key in encoded bytes
	 * @return the private key
	 * @throws NoSuchAlgorithmException if the RSA algorithm is unavailable
	 * @throws InvalidKeySpecException  if it's not an RSA key
	 */
	public static PrivateKey getPrivateKey(byte[] data) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		Objects.requireNonNull(data);
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
		Objects.requireNonNull(privateKey);
		Objects.requireNonNull(data);
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
		Objects.requireNonNull(publicKey);
		Objects.requireNonNull(signature);
		Objects.requireNonNull(data);
		try
		{
			var signer = Signature.getInstance(SIGNATURE_ALGORITHM);
			signer.initVerify(publicKey);
			signer.update(data);
			return signer.verify(signature);
		}
		catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e)
		{
			return false;
		}
	}

	/**
	 * Converts an RSA private key from PKCS #8 to PKCS #1
	 *
	 * @param privateKey the RSA private key
	 * @return the RSA private key in PKCS #8 format
	 * @throws IOException if the key format is wrong
	 */
	public static byte[] getPrivateKeyAsPkcs1(PrivateKey privateKey) throws IOException
	{
		Objects.requireNonNull(privateKey);
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
	 * @throws IOException              if the key format is wrong
	 * @throws NoSuchAlgorithmException if the key format is wrong
	 * @throws InvalidKeySpecException  if the encoding is wrong
	 */
	public static PrivateKey getPrivateKeyFromPkcs1(byte[] data) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
	{
		Objects.requireNonNull(data);
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
	 * @throws IOException if the key format is wrong
	 */
	public static byte[] getPublicKeyAsPkcs1(PublicKey publicKey) throws IOException
	{
		Objects.requireNonNull(publicKey);
		var subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
		var primitive = subjectPublicKeyInfo.parsePublicKey();
		return primitive.getEncoded();
	}

	/**
	 * Converts a PKCS #1 byte array to an RSA public key.
	 *
	 * @param data the DER encoded PKCS #1 byte array
	 * @return an RSA public key
	 * @throws IOException              if the key format is wrong
	 * @throws NoSuchAlgorithmException if the key format is wrong
	 * @throws InvalidKeySpecException  if the encoding is wrong
	 */
	public static PublicKey getPublicKeyFromPkcs1(byte[] data) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
	{
		Objects.requireNonNull(data);
		var algorithmIdentifier = new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE);
		var subjectPublicKeyInfo = new SubjectPublicKeyInfo(algorithmIdentifier, data);
		return getPublicKey(subjectPublicKeyInfo.getEncoded());
	}

	/**
	 * Computes the GxsId from the key. This is done by sha1 hashing the n and e numbers
	 * and getting the first 16 bytes from it.
	 *
	 * @param publicKey the RSA public key
	 * @return the GxsId
	 */
	public static GxsId getGxsId(PublicKey publicKey)
	{
		Objects.requireNonNull(publicKey);
		var rsaPublicKey = (RSAPublicKey) publicKey;
		return makeGxsId(
				BigIntegers.asUnsignedByteArray(rsaPublicKey.getModulus()),
				BigIntegers.asUnsignedByteArray(rsaPublicKey.getPublicExponent())
		);
	}

	private static GxsId makeGxsId(byte[] modulus, byte[] exponent)
	{
		var md = new Sha1MessageDigest();
		md.update(modulus);
		md.update(exponent);

		// Copy the first 16 bytes of the sha1 sum to get the GxsId
		return new GxsId(Arrays.copyOfRange(md.getBytes(), 0, GxsId.LENGTH));
	}

	/**
	 * Computes the GxsId from the key.
	 *
	 * @param publicKey the RSA public key
	 * @return the GxsId
	 * @deprecated For compatibility with entities generated by old Retroshare versions. Is less secure. Do not use for new code.
	 */
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	public static GxsId getGxsIdInsecure(PublicKey publicKey)
	{
		Objects.requireNonNull(publicKey);
		var rsaPublicKey = (RSAPublicKey) publicKey;
		return makeGxsIdInsecure(BigIntegers.asUnsignedByteArray(rsaPublicKey.getModulus()));
	}

	private static GxsId makeGxsIdInsecure(byte[] modulus)
	{
		return new GxsId(Arrays.copyOfRange(modulus, 0, GxsId.LENGTH));
	}
}
