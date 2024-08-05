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

package io.xeres.app.service;

import io.xeres.app.crypto.pgp.PGP;
import io.xeres.app.crypto.rsa.RSA;
import io.xeres.app.crypto.rsid.RSSerialVersion;
import io.xeres.app.crypto.x509.X509;
import io.xeres.app.database.model.connection.Connection;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.database.repository.LocationRepository;
import io.xeres.app.net.protocol.PeerAddress;
import io.xeres.app.net.util.NetworkMode;
import io.xeres.common.id.LocationId;
import io.xeres.common.protocol.NetMode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.*;

import static io.xeres.app.net.util.NetworkMode.hasDht;
import static io.xeres.app.net.util.NetworkMode.isDiscoverable;
import static io.xeres.app.service.ResourceCreationState.*;
import static io.xeres.common.dto.location.LocationConstants.OWN_LOCATION_ID;
import static java.util.function.Predicate.not;

@Service
public class LocationService
{
	private static final Logger log = LoggerFactory.getLogger(LocationService.class);

	private static final int KEY_SIZE = 3072;

	private final SettingsService settingsService;
	private final ProfileService profileService;
	private final LocationRepository locationRepository;

	private Slice<Location> locations;
	private int pageIndex;
	private int connectionIndex = -1;

	public LocationService(SettingsService settingsService, ProfileService profileService, LocationRepository locationRepository)
	{
		this.settingsService = settingsService;
		this.profileService = profileService;
		this.locationRepository = locationRepository;
	}

	KeyPair generateLocationKeys()
	{
		if (settingsService.getLocationPrivateKeyData() != null)
		{
			return null;
		}

		log.info("Generating keys, algorithm: RSA, bits: {} ...", KEY_SIZE);

		var keyPair = RSA.generateKeys(KEY_SIZE);

		log.info("Successfully generated key pair");

		return keyPair;
	}

	byte[] generateLocationCertificate(byte[] locationPublicKeyData) throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, IOException
	{
		log.info("Generating certificate...");

		var x509Certificate = X509.generateCertificate(
				PGP.getPGPSecretKey(settingsService.getSecretProfileKey()),
				RSA.getPublicKey(locationPublicKeyData),
				"CN=" + Long.toHexString(profileService.getOwnProfile().getPgpIdentifier()).toUpperCase(Locale.ROOT), // older RS use a random string I think, like 12:34:55:44:4e:44:99:23
				"CN=-",
				new Date(0),
				new Date(0),
				RSSerialVersion.V07_0001.serialNumber()
		);

		log.info("Successfully generated certificate");

		return x509Certificate.getEncoded();
	}

	@Transactional
	public ResourceCreationState generateOwnLocation(String name)
	{
		if (!settingsService.isOwnProfilePresent())
		{
			log.error("Cannot create a location without a profile; Create a profile first");
			return FAILED;
		}
		var ownProfile = profileService.getOwnProfile();

		if (!ownProfile.getLocations().isEmpty())
		{
			return ALREADY_EXISTS;
		}

		var keyPair = generateLocationKeys();
		byte[] x509Certificate;

		try
		{
			x509Certificate = generateLocationCertificate(keyPair.getPublic().getEncoded());
			createOwnLocation(name, keyPair, x509Certificate);
		}
		catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException | CertificateException e)
		{
			log.error("Failed to generate certificate: {}", e.getMessage());
			return FAILED;
		}
		return CREATED;
	}

	@Transactional
	public void createOwnLocation(String name, KeyPair keyPair, byte[] x509Certificate) throws CertificateException
	{
		settingsService.saveLocationKeys(keyPair);
		settingsService.saveLocationCertificate(x509Certificate);

		var location = Location.createLocation(name);
		location.setLocationId(X509.getLocationId(X509.getCertificate(settingsService.getLocationCertificate())));
		profileService.getOwnProfile().addLocation(location);
		locationRepository.save(location);
	}

	/**
	 * Find the location.
	 *
	 * @param locationId the SSL identifier
	 * @return the location
	 */
	public Optional<Location> findLocationByLocationId(LocationId locationId)
	{
		return locationRepository.findByLocationId(locationId);
	}

	public Optional<Location> findOwnLocation()
	{
		return locationRepository.findById(OWN_LOCATION_ID);
	}

	public Optional<Location> findLocationById(long id)
	{
		return locationRepository.findById(id);
	}

	public boolean hasOwnLocation()
	{
		return findOwnLocation().isPresent();
	}

	public void markAllConnectionsAsDisconnected()
	{
		locationRepository.putAllConnectedToFalse();
	}

	@Transactional
	public void setConnected(Location location, SocketAddress socketAddress)
	{
		updateConnection(location, socketAddress); // XXX: is this the right place? maybe it should be done in discovery service

		location.setConnected(true);
	}

	private static void updateConnection(Location location, SocketAddress socketAddress)
	{
		var inetSocketAddress = (InetSocketAddress) socketAddress;

		location.getConnections().stream()
				.filter(conn -> conn.getAddress().split(":")[0].equals(inetSocketAddress.getHostString()))
				.findFirst()
				.ifPresent(connection -> connection.setLastConnected(Instant.now()));
	}

	@Transactional
	public void setDisconnected(Location location)
	{
		location.setConnected(false);
		locationRepository.save(location); // This is needed because PeerHandler calls it from a non managed context
	}

	@Transactional
	public Location update(Location location, String locationName, NetMode netMode, String version, NetworkMode networkMode, List<PeerAddress> peerAddresses)
	{
		location.setName(locationName);
		location.setNetMode(netMode);
		location.setVersion(version);
		location.setDiscoverable(isDiscoverable(networkMode));
		location.setDht(hasDht(networkMode));
		peerAddresses.forEach(peerAddress -> updateConnection(location, peerAddress));
		return locationRepository.save(location);
	}

	@Transactional
	public List<Connection> getConnectionsToConnectTo(int simultaneousLocations)
	{
		var ownConnection = findOwnLocation().orElseThrow()
				.getConnections()
				.stream()
				.filter(Connection::isExternal)
				.findFirst().orElse(null);

		var ownIp = ownConnection != null ? ownConnection.getIp() : null;

		locations = locationRepository.findAllByConnectedFalse(PageRequest.of(getPageIndex(), simultaneousLocations, Sort.by("lastConnected").descending()));

		return locations.stream()
				.filter(not(Location::isOwn))
				.flatMap(location -> location.getBestConnection(getConnectionIndex(), ownIp))
				.limit(simultaneousLocations)
				.toList();
	}

	public Slice<Location> getUnconnectedLocationsWithDht(Pageable pageable)
	{
		return locationRepository.findAllByConnectedFalseAndDhtTrue(pageable);
	}

	public List<Location> getConnectedLocations()
	{
		return locationRepository.findAllByConnectedTrue();
	}

	public List<Location> getAllLocations()
	{
		return locationRepository.findAll();
	}

	@Transactional
	public void updateConnection(Location location, PeerAddress peerAddress)
	{
		if (peerAddress.isInvalid())
		{
			return;
		}

		if (location.isOwn())
		{
			updateOwnConnection(location, peerAddress);
		}
		else
		{
			updateOtherConnection(location, peerAddress);
		}
	}

	private static void updateOwnConnection(Location location, PeerAddress peerAddress)
	{
		var updated = false;

		for (var connection : location.getConnections())
		{
			updated = updateAddressIfSameType(peerAddress, connection);
			if (updated)
			{
				break;
			}
		}

		if (!updated)
		{
			location.addConnection(Connection.from(peerAddress));
		}
	}

	private static void updateOtherConnection(Location location, PeerAddress peerAddress)
	{
		location.addConnection(Connection.from(peerAddress));
	}

	public String getHostname() throws UnknownHostException
	{
		return InetAddress.getLocalHost().getHostName();
	}

	public String getUsername()
	{
		var username = System.getProperty("user.name");
		if (StringUtils.isEmpty(username))
		{
			throw new NoSuchElementException("No logged in username");
		}
		return username;
	}

	private static boolean updateAddressIfSameType(PeerAddress from, Connection to)
	{
		if ((from.isExternal() && to.isExternal())
				|| (!from.isExternal() && !to.isExternal()))
		{
			to.setAddress(from.getAddress().orElseThrow());
			return true;
		}
		return false;
	}

	private int getPageIndex() // XXX: that stuff should be moved out
	{
		if (locations == null || locations.isLast())
		{
			pageIndex = 0;
			connectionIndex++;
		}
		else
		{
			pageIndex++;
		}
		return pageIndex;
	}

	private int getConnectionIndex()
	{
		return connectionIndex;
	}
}
