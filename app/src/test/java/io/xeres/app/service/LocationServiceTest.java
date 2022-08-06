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
import io.xeres.app.database.model.connection.ConnectionFakes;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.app.database.repository.LocationRepository;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
		var now = Instant.now();

		// First location with 1 connection
		var location1 = LocationFakes.createLocation("test1", ownProfile);
		location1.addConnection(ConnectionFakes.createConnection());

		// Second location with 3 connections
		var location2 = LocationFakes.createLocation("test2", ownProfile);
		var oldConnection = ConnectionFakes.createConnection();
		var recentConnection = ConnectionFakes.createConnection();
		var nullConnection = ConnectionFakes.createConnection();
		oldConnection.setLastConnected(now.minus(Duration.ofDays(1)));
		location2.addConnection(oldConnection);
		recentConnection.setLastConnected(now);
		location2.addConnection(recentConnection);
		location2.addConnection(nullConnection);

		var locations = List.of(location1, location2);
		Slice<Location> slice = new SliceImpl<>(locations);
		when(locationRepository.findAllByConnectedFalse(any(Pageable.class))).thenReturn(slice);

		// First run
		var connections = locationService.getConnectionsToConnectTo(10);
		assertEquals(2, connections.size());
		assertEquals(location1.getConnections().get(0), connections.get(0));
		assertEquals(recentConnection, connections.get(1));

		// Second run
		connections = locationService.getConnectionsToConnectTo(10);
		assertEquals(2, connections.size());
		assertEquals(location1.getConnections().get(0), connections.get(0));
		assertEquals(oldConnection, connections.get(1));

		// Third run
		connections = locationService.getConnectionsToConnectTo(10);
		assertEquals(2, connections.size());
		assertEquals(location1.getConnections().get(0), connections.get(0));
		assertEquals(nullConnection, connections.get(1));
	}

	@Test
	void LocationService_SetConnected_OK()
	{
		var location = LocationFakes.createLocation("foo", ProfileFakes.createProfile("foo", 1));

		locationService.setConnected(location, new InetSocketAddress("127.0.0.1", 666));

		assertTrue(location.isConnected());
		verify(locationRepository).save(location);
	}

	@Test
	void LocationService_SetDisconnected_OK()
	{
		var location = LocationFakes.createLocation("foo", ProfileFakes.createProfile("foo", 1));
		location.setConnected(true);

		locationService.setDisconnected(location);

		assertFalse(location.isConnected());
	}
}
