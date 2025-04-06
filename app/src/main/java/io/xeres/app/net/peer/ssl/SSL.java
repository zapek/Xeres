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
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.net.peer.ConnectionType;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.ProfileService;
import io.xeres.common.id.LocationIdentifier;
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
import java.util.Locale;
import java.util.regex.Pattern;

import static io.xeres.app.net.peer.ConnectionType.TCP_INCOMING;

public final class SSL
{
	private static final Logger log = LoggerFactory.getLogger(SSL.class);

	private static final Pattern ISSUER_MATCHER = Pattern.compile("^CN=(\\p{XDigit}{16})$");

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
				.sslProvider(SslProvider.JDK)
				.protocols("TLSv1.3")
				.clientAuth(ClientAuth.REQUIRE)
				.trustManager(InsecureTrustManagerFactory.INSTANCE)
				.build();
	}

	/**
	 * Checks if a certificate is valid. Either it matches a location that we already have or it's the location of a profile that
	 * we have accepted. In the later case, the new location is also created with a null name that will be updated later
	 * using discovery.
	 *
	 * @param profileService  the profile service
	 * @param locationService the location service
	 * @param chain           the certificate chain
	 * @return the location
	 * @throws CertificateException if the location is not allowed
	 */
	public static Location checkPeerCertificate(ProfileService profileService, LocationService locationService, Certificate[] chain) throws CertificateException
	{
		var isNewLocation = false;

		if (chain == null || chain.length == 0)
		{
			throw new CertificateException("Empty certificate");
		}

		var x509Certificate = X509.getCertificate(chain[0].getEncoded());

		var locationIdentifier = X509.getLocationIdentifier(x509Certificate);
		log.debug("SSL ID: {}", locationIdentifier);

		var location = locationService.findLocationByLocationIdentifier(locationIdentifier).orElse(null);
		if (location == null)
		{
			location = createLocationIfAcceptedProfile(locationIdentifier, x509Certificate, profileService);
			if (location == null)
			{
				throw new CertificateException("Unknown location (SSL ID: " + locationIdentifier + ")");
			}
			isNewLocation = true;
		}
		log.debug("Found location: {} {}", location.getSafeName(), location.isConnected() ? ", is already connected" : "");
		if (location.isConnected())
		{
			throw new CertificateException("Already connected");
		}

		if (location.getProfile().isComplete())
		{
			try
			{
				verify(PGP.getPGPPublicKey(location.getProfile().getPgpPublicKeyData()), x509Certificate);
				if (isNewLocation)
				{
					profileService.createOrUpdateProfile(location.getProfile());
				}
			}
			catch (InvalidKeyException e)
			{
				throw new CertificateException(e.getMessage(), e);
			}
		}
		return location;
	}

	private static Location createLocationIfAcceptedProfile(LocationIdentifier locationIdentifier, X509Certificate x509Certificate, ProfileService profileService)
	{
		var issuer = x509Certificate.getIssuerX500Principal().getName();
		var matcher = ISSUER_MATCHER.matcher(issuer);

		if (matcher.matches())
		{
			var pgpIdentifier = Long.parseUnsignedLong(matcher.group(1).toLowerCase(Locale.ROOT), 16);

			var profile = profileService.findProfileByPgpIdentifier(pgpIdentifier)
					.filter(Profile::isComplete)
					.filter(Profile::isAccepted)
					.orElse(null);

			if (profile != null)
			{
				return Location.createLocation(null, profile, locationIdentifier);
			}
			log.debug("No profile found for location: {}", locationIdentifier);
		}
		else
		{
			log.debug("Couldn't match PGP key from certificate issuer: {}", issuer);
		}
		return null;
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
