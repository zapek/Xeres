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

import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.crypto.rsid.RSSerialVersion;
import io.xeres.testutils.TestUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class X509Test
{
	private static final int KEY_SIZE = 512;
	private static PGPSecretKey pgpSecretKey;
	private static KeyPair keyPair;

	@BeforeAll
	static void setup() throws PGPException
	{
		Security.addProvider(new BouncyCastleProvider());

		pgpSecretKey = PGP.generateSecretKey("test", null, KEY_SIZE);
		keyPair = RSA.generateKeys(KEY_SIZE);
	}

	@Test
	void X509_NoInstance_OK() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(X509.class);
	}

	/**
	 * Generates an X509 certificate.
	 */
	@Test
	void X509_GenerateCertificate_OK() throws PGPException, IOException, CertificateException, SignatureException
	{
		generateCertificate(RSSerialVersion.V07_0001.serialNumber());
	}

	@Test
	void X509_GenerateCertificate_OldRS_0_6_5_OK() throws PGPException, IOException, CertificateException, SignatureException
	{
		generateCertificate(RSSerialVersion.V06_0001.serialNumber());
	}

	@Test
	void X509_GenerateCertificate_OldestRS_OK() throws PGPException, IOException, CertificateException, SignatureException
	{
		generateCertificate(new BigInteger("123456", 16));
	}

	private void generateCertificate(BigInteger serialNumber) throws IOException, CertificateException, PGPException, SignatureException
	{
		var issuer = "CN=1234";
		var subject = "CN=-";
		var from = new Date(0);
		var to = new Date(0);

		var cert = X509.generateCertificate(pgpSecretKey, keyPair.getPublic(), issuer, subject, from, to, serialNumber);
		assertNotNull(cert);
		assertEquals(issuer, cert.getIssuerX500Principal().getName());
		assertEquals(subject, cert.getSubjectX500Principal().getName());
		assertEquals(serialNumber, cert.getSerialNumber());
		assertEquals(from, cert.getNotBefore());
		assertEquals(to, cert.getNotAfter());
		PGP.verify(pgpSecretKey.getPublicKey(), cert.getSignature(), new ByteArrayInputStream(cert.getTBSCertificate()));
	}
}
