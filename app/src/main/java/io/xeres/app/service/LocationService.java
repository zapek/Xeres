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

import io.xeres.app.application.events.LocationReadyEvent;
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
import io.xeres.common.properties.StartupProperties;
import io.xeres.common.protocol.NetMode;
import io.xeres.common.protocol.ip.IP;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.*;

import static io.xeres.app.net.util.NetworkMode.hasDht;
import static io.xeres.app.net.util.NetworkMode.isDiscoverable;
import static io.xeres.common.dto.location.LocationConstants.OWN_LOCATION_ID;
import static java.util.function.Predicate.not;

@Service
@Transactional(readOnly = true)
public class LocationService
{
	private static final Logger log = LoggerFactory.getLogger(LocationService.class);

	private static final int KEY_SIZE = 3072;

	public enum UpdateConnectionStatus
	{
		UPDATED,
		ADDED,
		FAILED
	}

	private final SettingsService settingsService;
	private final ProfileService profileService;
	private final LocationRepository locationRepository;
	private final ApplicationEventPublisher publisher;

	private Slice<Location> locations;
	private int pageIndex;
	private int connectionIndex = -1;

	public LocationService(SettingsService settingsService, ProfileService profileService, LocationRepository locationRepository, ApplicationEventPublisher publisher)
	{
		this.settingsService = settingsService;
		this.profileService = profileService;
		this.locationRepository = locationRepository;
		this.publisher = publisher;
	}

	void generateLocationKeys()
	{
		if (settingsService.getLocationPrivateKeyData() != null)
		{
			return;
		}

		log.info("Generating keys, algorithm: RSA, bits: {} ...", KEY_SIZE);

		var keyPair = RSA.generateKeys(KEY_SIZE);

		log.info("Successfully generated key pair");

		settingsService.saveLocationKeys(keyPair);
	}

	void generateLocationCertificate() throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, IOException
	{
		if (settingsService.hasOwnLocation())
		{
			return;
		}
		if (!settingsService.isOwnProfilePresent())
		{
			throw new CertificateException("Cannot generate certificate without a profile; Create a profile first");
		}

		log.info("Generating certificate...");

		var x509Certificate = X509.generateCertificate(
				PGP.getPGPSecretKey(settingsService.getSecretProfileKey()),
				RSA.getPublicKey(settingsService.getLocationPublicKeyData()),
				"CN=" + Long.toHexString(profileService.getOwnProfile().getPgpIdentifier()).toUpperCase(Locale.ROOT), // older RS use a random string I think, like 12:34:55:44:4e:44:99:23
				"CN=-",
				new Date(0),
				new Date(0),
				RSSerialVersion.V07_0001.serialNumber()
		);

		log.info("Successfully generated certificate");

		settingsService.saveLocationCertificate(x509Certificate.getEncoded());
	}

	@Transactional
	public void createOwnLocation(String name) throws CertificateException
	{
		if (!settingsService.isOwnProfilePresent())
		{
			throw new CertificateException("Cannot create a location without a profile; Create a profile first");
		}
		var ownProfile = profileService.getOwnProfile();

		if (!ownProfile.getLocations().isEmpty())
		{
			throw new CertificateException("Location already exists");
		}

		var localIpAddress = Optional.ofNullable(IP.getLocalIpAddress()).orElseThrow(() -> new CertificateException("Current host has no IP address. Please configure your network"));

		// Create an IPv4 location
		int localPort = Optional.ofNullable(StartupProperties.getInteger(StartupProperties.Property.SERVER_PORT)).orElseGet(IP::getFreeLocalPort);
		log.info("Using local ip address {} and port {}", localIpAddress, localPort);

		generateLocationKeys();

		try
		{
			generateLocationCertificate();
		}
		catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e)
		{
			throw new CertificateException("Failed to generate certificate: " + e.getMessage());
		}

		var location = Location.createLocation(name);
		location.setLocationId(settingsService.getLocationId());
		ownProfile.addLocation(location);
		locationRepository.save(location);

		// Send the event asynchronously so that our transaction can complete first
		publisher.publishEvent(new LocationReadyEvent(localIpAddress, localPort));
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

	@Transactional
	public void markAllConnectionsAsDisconnected()
	{
		locationRepository.putAllConnectedToFalse();
	}

	@Transactional
	public void setConnected(Location location, SocketAddress socketAddress)
	{
		updateConnection(location, socketAddress); // XXX: is this the right place? maybe it should be done in discovery service

		location.setConnected(true);
		locationRepository.save(location);
	}

	private void updateConnection(Location location, SocketAddress socketAddress)
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
		locationRepository.save(location);
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

	public List<Connection> getConnectionsToConnectTo(int simultaneousLocations)
	{
		var ownConnection = findOwnLocation().orElseThrow()
				.getConnections()
				.stream()
				.filter(Connection::isExternal)
				.findFirst().orElseThrow();

		locations = locationRepository.findAllByConnectedFalse(PageRequest.of(getPageIndex(), simultaneousLocations, Sort.by("lastConnected").descending()));

		return locations.stream()
				.filter(not(Location::isOwn))
				.flatMap(location -> location.getBestConnection(getConnectionIndex(), ownConnection.getIp()))
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
	public UpdateConnectionStatus updateConnection(Location location, PeerAddress peerAddress)
	{
		if (peerAddress.isInvalid())
		{
			return UpdateConnectionStatus.FAILED;
		}

		if (location.isOwn())
		{
			return updateOwnConnection(location, peerAddress);
		}
		else
		{
			return updateOtherConnection(location, peerAddress);
		}
	}

	private UpdateConnectionStatus updateOwnConnection(Location location, PeerAddress peerAddress)
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
			updated = location.addConnection(Connection.from(peerAddress));
		}
		locationRepository.save(location);
		return updated ? UpdateConnectionStatus.UPDATED : UpdateConnectionStatus.ADDED;
	}

	private UpdateConnectionStatus updateOtherConnection(Location location, PeerAddress peerAddress)
	{
		var updated = location.addConnection(Connection.from(peerAddress));
		locationRepository.save(location);
		return updated ? UpdateConnectionStatus.UPDATED : UpdateConnectionStatus.FAILED;
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

	private boolean updateAddressIfSameType(PeerAddress from, Connection to)
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
