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

package io.xeres.app.service;

import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.database.model.connection.ConnectionFakes;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.model.location.LocationFakes;
import io.xeres.app.database.model.profile.Profile;
import io.xeres.app.database.model.profile.ProfileFakes;
import io.xeres.app.database.repository.LocationRepository;
import io.xeres.common.id.ProfileFingerprint;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

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
import java.util.Optional;

import static io.xeres.app.net.protocol.PeerAddress.Type.IPV4;
import static io.xeres.common.dto.location.LocationConstants.OWN_LOCATION_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest
{
	@Mock
	private SettingsService settingsService;

	@Mock
	private ProfileService profileService;

	@Mock
	private LocationRepository locationRepository;

	@Mock
	private ApplicationEventPublisher publisher;

	@InjectMocks
	private LocationService locationService;

	private static PGPSecretKey pgpSecretKey;
	private static KeyPair keyPair;
	private static Profile ownProfile;

	@BeforeAll
	static void setup() throws PGPException
	{
		Security.addProvider(new BouncyCastleProvider());

		pgpSecretKey = PGP.generateSecretKey("test", "", 512);
		keyPair = RSA.generateKeys(512);
		ownProfile = Profile.createProfile("test", pgpSecretKey.getKeyID(), pgpSecretKey.getPublicKey().getCreationTime().toInstant(), new ProfileFingerprint(pgpSecretKey.getPublicKey().getFingerprint()), pgpSecretKey.getPublicKey());
	}

	@Test
	void LocationService_GenerateLocationKeys_Success()
	{
		when(settingsService.getLocationPrivateKeyData()).thenReturn(null);

		assertNotNull(locationService.generateLocationKeys());

		verify(settingsService).getLocationPrivateKeyData();
	}

	@Test
	void GenerateLocationKeys_LocationAlreadyExists_Success()
	{
		when(settingsService.getLocationPrivateKeyData()).thenReturn(new byte[]{1});

		assertNull(locationService.generateLocationKeys());

		verify(settingsService, never()).saveLocationKeys(any(KeyPair.class));
	}

	@Test
	void GenerateLocationCertificate_Success() throws NoSuchAlgorithmException, CertificateException, InvalidKeySpecException, IOException
	{
		when(settingsService.getSecretProfileKey()).thenReturn(pgpSecretKey.getEncoded());
		when(profileService.getOwnProfile()).thenReturn(ownProfile);

		assertNotNull(locationService.generateLocationCertificate(keyPair.getPublic().getEncoded()));

	}

	@Test
	void CreateLocation_Success() throws IOException
	{
		when(settingsService.isOwnProfilePresent()).thenReturn(true);
		when(profileService.getOwnProfile()).thenReturn(ownProfile);
		when(settingsService.getSecretProfileKey()).thenReturn(pgpSecretKey.getEncoded());
		when(settingsService.getLocationCertificate()).thenReturn(keyPair.getPublic().getEncoded());
		when(profileService.getOwnProfile()).thenReturn(ownProfile);

		locationService.generateOwnLocation("test");

		verify(settingsService, times(1)).isOwnProfilePresent();
		verify(profileService, times(2)).getOwnProfile();
	}

	@Test
	void GetConnectionsToConnectTo_Success()
	{
		var now = Instant.now();

		// Own location
		var ownLocation = LocationFakes.createOwnLocation();
		ownLocation.addConnection(ConnectionFakes.createConnection(IPV4, "2.3.4.5:1234", true));

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

		when(locationRepository.findById(OWN_LOCATION_ID)).thenReturn(Optional.of(ownLocation));

		// First run
		var connections = locationService.getConnectionsToConnectTo(10);
		assertEquals(2, connections.size());
		assertEquals(location1.getConnections().getFirst(), connections.get(0));
		assertEquals(recentConnection, connections.get(1));

		// Second run
		connections = locationService.getConnectionsToConnectTo(10);
		assertEquals(2, connections.size());
		assertEquals(location1.getConnections().getFirst(), connections.get(0));
		assertEquals(oldConnection, connections.get(1));

		// Third run
		connections = locationService.getConnectionsToConnectTo(10);
		assertEquals(2, connections.size());
		assertEquals(location1.getConnections().getFirst(), connections.get(0));
		assertEquals(nullConnection, connections.get(1));
	}

	@Test
	void GetConnectionsToConnectTo_PreferLAN()
	{
		var now = Instant.now();

		// Own location
		var ownLocation = LocationFakes.createOwnLocation();
		ownLocation.addConnection(ConnectionFakes.createConnection(IPV4, "2.3.4.5:1234", true));

		// First location with 1 connection, same address
		var location1 = LocationFakes.createLocation("test1", ownProfile);
		location1.addConnection(ConnectionFakes.createConnection(IPV4, "2.3.4.5:1234", true));

		// Second location with 2 connections, one same, one LAN
		var location2 = LocationFakes.createLocation("test2", ownProfile);
		var wanConnection = ConnectionFakes.createConnection(IPV4, "2.3.4.5:1234", true);
		var lanConnection = ConnectionFakes.createConnection(IPV4, "192.168.1.25:1234", false);
		wanConnection.setLastConnected(now);
		location2.addConnection(wanConnection);
		lanConnection.setLastConnected(now);
		location2.addConnection(lanConnection);

		var locations = List.of(location1, location2);
		Slice<Location> slice = new SliceImpl<>(locations);
		when(locationRepository.findAllByConnectedFalse(any(Pageable.class))).thenReturn(slice);

		when(locationRepository.findById(OWN_LOCATION_ID)).thenReturn(Optional.of(ownLocation));

		// First run
		var connections = locationService.getConnectionsToConnectTo(10);
		assertEquals(2, connections.size());
		assertEquals(location1.getConnections().getFirst(), connections.get(0));
		assertEquals(lanConnection, connections.get(1));

		// Second run
		connections = locationService.getConnectionsToConnectTo(10);
		assertEquals(2, connections.size());
		assertEquals(location1.getConnections().getFirst(), connections.get(0));
		assertEquals(wanConnection, connections.get(1));
	}

	@Test
	void SetConnected_Success()
	{
		var location = LocationFakes.createLocation("foo", ProfileFakes.createProfile("foo", 1));

		locationService.setConnected(location, new InetSocketAddress("127.0.0.1", 666));

		assertTrue(location.isConnected());
	}

	@Test
	void SetDisconnected_Success()
	{
		var location = LocationFakes.createLocation("foo", ProfileFakes.createProfile("foo", 1));
		location.setConnected(true);

		locationService.setDisconnected(location);

		assertFalse(location.isConnected());
	}
}
