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

package io.xeres.app.crypto.x509;

import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.app.crypto.hash.sha256.Sha256MessageDigest;
import io.xeres.app.crypto.pgp.PGPSigner;
import io.xeres.app.crypto.rsid.RSSerialVersion;
import io.xeres.common.id.LocationIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.openpgp.PGPSecretKey;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Optional;

/**
 * Implements all X509 certificate functions. Used to create an SSL certificate for the location.
 */
public final class X509
{
	private static final String CERTIFICATE_TYPE = "X.509";

	private X509()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	/**
	 * Generates a certificate.
	 *
	 * @param pgpSecretKey a PGP secret key
	 * @param rsaPublicKey an RSA public key
	 * @param issuer       the issuer
	 * @param subject      the subject
	 * @param dateOfIssue  the date of certificate validity
	 * @param dateOfExpiry the date of certificate expiration
	 * @param serial       the serial number
	 * @return a {@link X509Certificate}
	 * @throws IOException          if there's an I/O error
	 * @throws CertificateException if there's a certificate error
	 */
	public static X509Certificate generateCertificate(PGPSecretKey pgpSecretKey, PublicKey rsaPublicKey, String issuer, String subject, Date dateOfIssue, Date dateOfExpiry, BigInteger serial) throws IOException, CertificateException
	{
		var certificateBuilder = new X509v1CertificateBuilder(
				new X500Name(issuer),
				serial,
				dateOfIssue,
				dateOfExpiry,
				new X500Name(subject),
				SubjectPublicKeyInfo.getInstance(rsaPublicKey.getEncoded())
		);

		var pgpSigner = new PGPSigner(pgpSecretKey);
		var certificateBytes = certificateBuilder.build(pgpSigner).getEncoded();

		return (X509Certificate) CertificateFactory.getInstance(CERTIFICATE_TYPE).generateCertificate(new ByteArrayInputStream(certificateBytes));
	}

	/**
	 * Gets the certificate from its encoded form.
	 *
	 * @param data a byte array with the encoded certificate
	 * @return a X509 certificate
	 * @throws CertificateException if there's a parse error
	 */
	public static X509Certificate getCertificate(byte[] data) throws CertificateException
	{
		return (X509Certificate) CertificateFactory.getInstance(CERTIFICATE_TYPE).generateCertificate(new ByteArrayInputStream(data));
	}

	/**
	 * Gets the SSL ID of the certificate.
	 *
	 * @param certificate the X509 certificate
	 * @return the ID that can be used as SSL ID
	 */
	public static LocationIdentifier getLocationIdentifier(X509Certificate certificate) throws CertificateException
	{
		var serialNumber = Optional.ofNullable(certificate.getSerialNumber()).orElseThrow(() -> new CertificateException("Missing serial number"));

		var out = new byte[LocationIdentifier.LENGTH];

		// There are several certificate versions
		if (serialNumber.equals(RSSerialVersion.V07_0001.serialNumber()))
		{
			// RS 0.6.6. ID is SHA-256 of signature (16 first bytes)
			var md = new Sha256MessageDigest();
			md.update(certificate.getSignature());
			System.arraycopy(md.getBytes(), 0, out, 0, out.length);
		}
		else if (serialNumber.equals(RSSerialVersion.V06_0001.serialNumber()))
		{
			// RS 0.6.5 after November 2017, ID is SHA-1 of signature (16 first bytes)
			var md = new Sha1MessageDigest();
			md.update(certificate.getSignature());
			System.arraycopy(md.getBytes(), 0, out, 0, out.length);
		}
		else
		{
			// The serial number here is either "60000" or a totally random string.
			// RS < November 2017. ID is the last 16 bytes of the signature.
			System.arraycopy(certificate.getSignature(), certificate.getSignature().length - out.length, out, 0, out.length);
		}
		return new LocationIdentifier(out);
	}
}
