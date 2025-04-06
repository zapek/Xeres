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

import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.crypto.rsid.RSSerialVersion;
import io.xeres.app.crypto.x509.X509;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.ProfileService;
import io.xeres.common.id.Id;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.testutils.TestUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.Optional;

import static io.xeres.app.net.peer.ConnectionType.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class SSLTest
{
	private static PGPSecretKey pgpKey;
	private static KeyPair rsaKey;
	private static Profile profile;
	private static X509Certificate certificate;

	@Mock
	private ProfileService profileService;

	@Mock
	private LocationService locationService;

	@BeforeAll
	static void setup() throws PGPException, IOException, CertificateException
	{
		Security.addProvider(new BouncyCastleProvider());

		pgpKey = PGP.generateSecretKey("foo", "", 512);
		rsaKey = RSA.generateKeys(512);
		profile = ProfileFakes.createProfile("foo", pgpKey.getKeyID(), pgpKey.getPublicKey().getFingerprint(), pgpKey.getPublicKey().getEncoded());
		profile.setAccepted(true);

		certificate = X509.generateCertificate(pgpKey, rsaKey.getPublic(), "CN=" + Id.toString(profile.getPgpIdentifier()), "CN=-", new Date(0), new Date(0), RSSerialVersion.V07_0001.serialNumber());
	}

	@Test
	void Instance_ThrowsException() throws NoSuchMethodException
	{
		TestUtils.assertUtilityClass(SSL.class);
	}

	@Test
	void CreateClientContext_Success() throws InvalidKeySpecException, NoSuchAlgorithmException, SSLException
	{
		var sslContext = SSL.createSslContext(rsaKey.getPrivate().getEncoded(), certificate, TCP_OUTGOING);

		assertNotNull(sslContext);
		assertTrue(sslContext.isClient());
	}

	@Test
	void CreateServerContext_Success() throws InvalidKeySpecException, NoSuchAlgorithmException, SSLException
	{
		var sslContext = SSL.createSslContext(rsaKey.getPrivate().getEncoded(), certificate, TCP_INCOMING);

		assertNotNull(sslContext);
		assertTrue(sslContext.isServer());
	}

	@Test
	void CreateServerContext_Tor_Success() throws InvalidKeySpecException, NoSuchAlgorithmException, SSLException
	{
		var sslContext = SSL.createSslContext(rsaKey.getPrivate().getEncoded(), certificate, TOR_OUTGOING);

		assertNotNull(sslContext);
		assertTrue(sslContext.isClient());
	}

	@Test
	void CreateServerContext_I2P_Success() throws InvalidKeySpecException, NoSuchAlgorithmException, SSLException
	{
		var sslContext = SSL.createSslContext(rsaKey.getPrivate().getEncoded(), certificate, I2P_OUTGOING);

		assertNotNull(sslContext);
		assertTrue(sslContext.isClient());
	}

	@Test
	void CheckPeerCertificate_Success() throws CertificateException
	{
		var location = LocationFakes.createLocation("bar", profile);

		when(locationService.findLocationByLocationIdentifier(any(LocationIdentifier.class))).thenReturn(Optional.of(location));

		var result = SSL.checkPeerCertificate(profileService, locationService, new X509Certificate[]{certificate});

		assertEquals(result, location);
		verify(locationService).findLocationByLocationIdentifier(any(LocationIdentifier.class));
	}

	@Test
	void CheckPeerCertificate_EmptyCertificate_Failure()
	{
		assertThatThrownBy(() -> SSL.checkPeerCertificate(profileService, locationService, new X509Certificate[]{}))
				.isInstanceOf(CertificateException.class)
				.hasMessage("Empty certificate");

		verify(locationService, never()).findLocationByLocationIdentifier(any(LocationIdentifier.class));
	}

	@Test
	void CheckPeerCertificate_AlreadyConnected_Failure()
	{
		var location = LocationFakes.createLocation("bar", profile);
		location.setConnected(true);

		when(locationService.findLocationByLocationIdentifier(any(LocationIdentifier.class))).thenReturn(Optional.of(location));

		assertThatThrownBy(() -> SSL.checkPeerCertificate(profileService, locationService, new X509Certificate[]{certificate}))
				.isInstanceOf(CertificateException.class)
				.hasMessage("Already connected");

		verify(locationService).findLocationByLocationIdentifier(any(LocationIdentifier.class));
	}

	@Test
	void CheckPeerCertificate_WrongCertificate_Failure() throws CertificateException, IOException, PGPException
	{
		var wrongPgpKey = PGP.generateSecretKey("notFoo", "", 512);
		var wrongCertificate = X509.generateCertificate(wrongPgpKey, rsaKey.getPublic(), "CN=me", "CN=foobar", new Date(0), new Date(0), RSSerialVersion.V07_0001.serialNumber());
		var location = LocationFakes.createLocation("bar", profile);

		when(locationService.findLocationByLocationIdentifier(any(LocationIdentifier.class))).thenReturn(Optional.of(location));

		assertThatThrownBy(() -> SSL.checkPeerCertificate(profileService, locationService, new X509Certificate[]{wrongCertificate}))
				.isInstanceOf(CertificateException.class)
				.hasMessageContaining("Wrong signature");

		verify(locationService).findLocationByLocationIdentifier(any(LocationIdentifier.class));
	}

	@Test
	void CheckPeerCertificate_NoLocationButProfile_Success() throws CertificateException
	{
		when(locationService.findLocationByLocationIdentifier(any(LocationIdentifier.class))).thenReturn(Optional.empty());
		when(profileService.findProfileByPgpIdentifier(profile.getPgpIdentifier())).thenReturn(Optional.of(profile));

		var newLocation = SSL.checkPeerCertificate(profileService, locationService, new X509Certificate[]{certificate});

		assertNotNull(newLocation);
		assertNull(newLocation.getName());
		assertEquals("[Unknown]", newLocation.getSafeName());
		assertEquals(newLocation.getProfile(), profile);
	}
}
