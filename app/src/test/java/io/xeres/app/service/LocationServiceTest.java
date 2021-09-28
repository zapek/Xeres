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

package io.xeres.app.service;

import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.database.model.connection.ConnectionFakes;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.app.database.repository.LocationRepository;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.common.id.LocationId;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class LocationServiceTest
{
	@Mock
	private PrefsService prefsService;

	@Mock
	private ProfileService profileService;

	@Mock
	private LocationRepository locationRepository;

	@InjectMocks
	private LocationService locationService;

	private static PGPSecretKey pgpSecretKey;
	private static KeyPair keyPair;
	private static Profile ownProfile;

	@BeforeAll
	static void setup() throws PGPException, IOException
	{
		Security.addProvider(new BouncyCastleProvider());

		pgpSecretKey = PGP.generateSecretKey("test", "", 512);
		keyPair = RSA.generateKeys(512);
		ownProfile = Profile.createProfile("test", pgpSecretKey.getKeyID(), pgpSecretKey.getPublicKey().getFingerprint(), pgpSecretKey.getPublicKey().getEncoded());
	}

	@Test
	void LocationService_GenerateLocationKeys_OK()
	{
		when(prefsService.getLocationPrivateKeyData()).thenReturn(null);

		locationService.generateLocationKeys();

		verify(prefsService).getLocationPrivateKeyData();
		verify(prefsService).saveLocationKeys(any(KeyPair.class));
	}

	@Test
	void LocationService_GenerateLocationKeys_LocationAlreadyExists_OK()
	{
		when(prefsService.getLocationPrivateKeyData()).thenReturn(new byte[]{1});

		verify(prefsService, times(0)).saveLocationKeys(any(KeyPair.class));
	}

	@Test
	void LocationService_GenerateLocationCertificate_OK() throws NoSuchAlgorithmException, CertificateException, InvalidKeySpecException, IOException
	{
		when(prefsService.hasOwnLocation()).thenReturn(false);
		when(prefsService.isOwnProfilePresent()).thenReturn(true);
		when(prefsService.getSecretProfileKey()).thenReturn(pgpSecretKey.getEncoded());
		when(prefsService.getLocationPublicKeyData()).thenReturn(keyPair.getPublic().getEncoded());
		when(profileService.getOwnProfile()).thenReturn(ownProfile);

		locationService.generateLocationCertificate();

		verify(prefsService).hasOwnLocation();
		verify(prefsService).isOwnProfilePresent();
		verify(prefsService).saveLocationCertificate(any(byte[].class));
	}

	@Test
	void LocationService_GenerateLocationCertificate_LocationAlreadyExists_OK() throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, IOException
	{
		when(prefsService.hasOwnLocation()).thenReturn(true);

		locationService.generateLocationCertificate();

		verify(prefsService, times(0)).saveLocationCertificate(any(byte[].class));
	}

	@Test
	void LocationService_GenerateLocationCertificate_MissingProfile_Fail()
	{
		when(prefsService.hasOwnLocation()).thenReturn(false);
		when(prefsService.isOwnProfilePresent()).thenReturn(false);

		assertThatThrownBy(() -> locationService.generateLocationCertificate())
				.isInstanceOf(CertificateException.class)
				.hasMessageContaining("without a profile");

		verify(prefsService).hasOwnLocation();
		verify(prefsService).isOwnProfilePresent();
	}

	@Test
	void LocationService_CreateLocation_OK() throws CertificateException, IOException
	{
		when(prefsService.isOwnProfilePresent()).thenReturn(true);
		when(profileService.getOwnProfile()).thenReturn(ownProfile);
		when(prefsService.getLocationId()).thenReturn(new LocationId());
		when(prefsService.getSecretProfileKey()).thenReturn(pgpSecretKey.getEncoded());
		when(prefsService.getLocationPublicKeyData()).thenReturn(keyPair.getPublic().getEncoded());
		when(profileService.getOwnProfile()).thenReturn(ownProfile);

		locationService.createOwnLocation("test");

		verify(prefsService, times(2)).isOwnProfilePresent();
		verify(profileService, times(2)).getOwnProfile();
		verify(prefsService).getLocationId();
		verify(locationRepository).save(any(Location.class));
		// There's no way to reliably wait for the publisher's event since it's asynchronous
	}

	@Test
	void LocationService_GetConnectionsToConnectTo_OK()
	{
		Location location1 = LocationFakes.createLocation("test1", ownProfile);
		location1.addConnection(ConnectionFakes.createConnection());
		Location location2 = LocationFakes.createLocation("test2", ownProfile);
		location2.addConnection(ConnectionFakes.createConnection());
		location2.addConnection(ConnectionFakes.createConnection(PeerAddress.Type.IPV4, "1.2.3.4:1234"));

		List<Location> locations = List.of(location1, location2);
		Slice<Location> slice = new SliceImpl<>(locations);
		when(locationRepository.findAllByConnectedFalse(any(Pageable.class))).thenReturn(slice);

		List<Connection> connections = locationService.getConnectionsToConnectTo();

		assertEquals(2, connections.size());
	}

	@Test
	void LocationService_SetConnected_OK()
	{
		Location location = LocationFakes.createLocation("foo", ProfileFakes.createProfile("foo", 1));

		locationService.setConnected(location, new InetSocketAddress("127.0.0.1", 666));

		assertTrue(location.isConnected());
		verify(locationRepository).save(any(Location.class));
	}

	@Test
	void LocationService_SetDisconnected_OK()
	{
		long LOCATION_ID = 1;
		Location location = LocationFakes.createLocation("foo", ProfileFakes.createProfile("foo", 1));
		location.setConnected(true);

		when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(location));

		locationService.setDisconnected(location);

		assertFalse(location.isConnected());
	}
}
