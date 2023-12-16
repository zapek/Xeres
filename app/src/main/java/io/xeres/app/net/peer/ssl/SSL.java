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

package io.xeres.app.net.peer.ssl;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.xeres.app.crypto.hash.sha1.Sha1MessageDigest;
import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.crypto.rsid.RSSerialVersion;
import io.xeres.app.crypto.x509.X509;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.peer.ConnectionType;
import io.xeres.app.service.LocationService;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

import static io.xeres.app.net.peer.ConnectionType.TCP_INCOMING;

public final class SSL
{
	private static final Logger log = LoggerFactory.getLogger(SSL.class);

	private SSL()
	{
		throw new UnsupportedOperationException("Utility class");
	}

	public static SslContext createSslContext(byte[] privateKeyData, X509Certificate certificate, ConnectionType connectionType) throws InvalidKeySpecException, NoSuchAlgorithmException, SSLException
	{
		SslContextBuilder builder;
		if (connectionType == TCP_INCOMING)
		{
			builder = SslContextBuilder.forServer(RSA.getPrivateKey(privateKeyData), certificate);
		}
		else
		{
			builder = SslContextBuilder.forClient()
					.keyManager(RSA.getPrivateKey(privateKeyData), certificate);
		}
		return builder
				.sslProvider(SslProvider.OPENSSL_REFCNT)
				.protocols("TLSv1.3")
				.clientAuth(ClientAuth.REQUIRE)
				.trustManager(InsecureTrustManagerFactory.INSTANCE)
				.build();
	}

	public static Location checkPeerCertificate(LocationService locationService, Certificate[] chain) throws CertificateException
	{
		if (chain == null || chain.length == 0)
		{
			throw new CertificateException("Empty certificate");
		}

		var x509Certificate = X509.getCertificate(chain[0].getEncoded());

		var locationId = X509.getLocationId(x509Certificate);
		log.debug("SSL ID: {}", locationId);

		var location = locationService.findLocationByLocationId(locationId).orElseThrow(() -> new CertificateException("Unknown location (SSL ID: " + locationId + ")")); // XXX: don't throw but handle location not found, see below
		// XXX: if the location is not found, we can check if we have a profile (accepted=true) that would verify it (verify() method below), then it would be a new location. The pgp identifier is in the issuer field (CN=pgp_id). create the location above instead of the orElseThrow() add orElseGet()
		// XXX: what we don't know is the location name so use something like [Unknown] (we need to allow null location names!) and get it with discovery
		log.debug("Found location: {} {}", location.getName(), location.isConnected() ? ", is already connected" : "");
		if (location.isConnected())
		{
			throw new CertificateException("Already connected");
		}

		// XXX: make sure everything is allright and there's no way to fool the system with shortInvites
		if (location.getProfile().isComplete())
		{
			try
			{
				verify(PGP.getPGPPublicKey(location.getProfile().getPgpPublicKeyData()), x509Certificate);
			}
			catch (InvalidKeyException e)
			{
				throw new CertificateException(e.getMessage(), e);
			}
		}
		return location;
	}

	private static void verify(PGPPublicKey pgpPublicKey, X509Certificate cert) throws CertificateException
	{
		var version = RSSerialVersion.getFromSerialNumber(cert.getSerialNumber());
		log.debug("Certificate version: {}", version);

		try
		{
			var in = cert.getTBSCertificate();

			if (version.ordinal() < RSSerialVersion.V07_0001.ordinal())
			{
				// If this is a 0.6 certificate, the signature verification is performed
				// on the hash of the certificate
				var md = new Sha1MessageDigest();
				md.update(in);
				in = md.getBytes();
			}
			PGP.verify(pgpPublicKey, cert.getSignature(), new ByteArrayInputStream(in));
		}
		catch (CertificateEncodingException | IOException | SignatureException | PGPException e)
		{
			throw new CertificateException(e);
		}
	}
}
